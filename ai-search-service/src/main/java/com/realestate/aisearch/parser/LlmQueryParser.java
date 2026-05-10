package com.realestate.aisearch.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.realestate.aisearch.config.AnthropicProperties;
import com.realestate.aisearch.dto.Chip;
import com.realestate.aisearch.dto.ParsedFilters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anthropic Claude parser using tool-use for strict JSON output.
 * Falls back to throwing on transport / 5xx so the caller can switch to regex.
 */
@Slf4j
@Component("llmQueryParser")
public class LlmQueryParser implements QueryParser {

    private static final String TOOL_NAME = "extract_property_search";

    private static final Map<String, Object> TOOL_PROPERTIES = Map.ofEntries(
            Map.entry("propertyType", Map.of("type", "string",
                    "enum", List.of("HOUSE", "APARTMENT", "VILLA", "TOWNHOUSE", "UNKNOWN"))),
            Map.entry("status", Map.of("type", "string",
                    "enum", List.of("FOR_SALE", "FOR_RENT", "UNKNOWN"))),
            Map.entry("bedrooms", Map.of("type", "integer", "minimum", 0, "maximum", 20)),
            Map.entry("bathrooms", Map.of("type", "integer", "minimum", 0, "maximum", 20)),
            Map.entry("minPrice", Map.of("type", "number")),
            Map.entry("maxPrice", Map.of("type", "number")),
            Map.entry("minArea", Map.of("type", "number")),
            Map.entry("maxArea", Map.of("type", "number")),
            Map.entry("cities", Map.of("type", "array", "items", Map.of("type", "string"))),
            Map.entry("districts", Map.of("type", "array", "items", Map.of("type", "string"))),
            Map.entry("freeText", Map.of("type", "string")),
            Map.entry("warnings", Map.of("type", "array", "items", Map.of("type", "string")))
    );

    private static final Map<String, Object> TOOL_SCHEMA = Map.of(
            "name", TOOL_NAME,
            "description", "Extract structured property search filters from a Vietnamese or English real-estate query.",
            "input_schema", Map.of(
                    "type", "object",
                    "properties", TOOL_PROPERTIES,
                    "required", List.of("warnings")
            )
    );

    private final AnthropicProperties props;
    private final WebClient client;

    public LlmQueryParser(AnthropicProperties props, WebClient.Builder builder) {
        this.props = props;
        this.client = builder
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Override
    public ParserMode mode() {
        return ParserMode.LLM;
    }

    @Override
    public ParseResult parse(String query, String locale) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured; cannot use LLM parser");
        }
        long start = System.nanoTime();

        Map<String, Object> body = buildRequestBody(query, locale);
        JsonNode response;
        try {
            response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", props.getApiKey())
                    .header("anthropic-version", props.getVersion())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(msg -> new RuntimeException(
                                    "Anthropic API error " + resp.statusCode().value() + ": " + msg)))
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(props.getTimeoutSeconds()));
        } catch (RuntimeException e) {
            log.warn("Anthropic call failed", e);
            throw e;
        }

        long ms = (System.nanoTime() - start) / 1_000_000;
        log.debug("Anthropic round-trip {} ms", ms);

        return decode(response, locale);
    }

    private Map<String, Object> buildRequestBody(String query, String locale) {
        String localeHint = locale == null ? "vi-VN" : locale;
        String systemPrompt = """
                You convert real-estate search queries (Vietnamese or English) into structured filters.
                You MUST call the tool `extract_property_search` exactly once.

                Currency normalization:
                - "5 tỷ" / "5 billion VND" / "5,000,000,000 đ" / "5B" all = 5000000000.
                - "20 triệu" / "20 million" = 20000000.

                Rent vs sale heuristics:
                - "/tháng", "cho thuê", "rent", "monthly" → status=FOR_RENT.
                - "bán", "for sale", "to buy" → status=FOR_SALE.
                - If neither, default to FOR_SALE.

                Locations:
                - Coastal cities include: Đà Nẵng, Nha Trang, Phú Quốc, Vũng Tàu, Hạ Long, Quy Nhơn, Hải Phòng.
                - "near the beach" / "ven biển" / "view biển" → fill `cities` with the relevant subset
                  AND keep the user's phrase in `freeText`.
                - Use canonical Vietnamese city names with diacritics.

                If unsure about a value, leave the field unset and add a short string to `warnings`
                explaining what was ambiguous. NEVER make up values.

                User locale hint: %s
                """.formatted(localeHint);

        return Map.of(
                "model", props.getModel(),
                "max_tokens", props.getMaxTokens(),
                "system", systemPrompt,
                "tools", List.of(TOOL_SCHEMA),
                "tool_choice", Map.of("type", "tool", "name", TOOL_NAME),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", query
                ))
        );
    }

    private ParseResult decode(JsonNode response, String locale) {
        JsonNode content = response.path("content");
        JsonNode toolUse = null;
        for (JsonNode block : content) {
            if ("tool_use".equals(block.path("type").asText())) {
                toolUse = block;
                break;
            }
        }
        if (toolUse == null) {
            throw new IllegalStateException("Anthropic response missing tool_use block");
        }
        JsonNode input = toolUse.path("input");

        // Validate against curated city list — drop unknown cities into warnings.
        Set<String> validCities = new HashSet<>(Locations.CITIES);
        Set<String> validDistricts = new HashSet<>(Locations.DISTRICTS);

        List<String> warnings = readStrings(input.path("warnings"));
        List<String> cities = new ArrayList<>();
        for (String c : readStrings(input.path("cities"))) {
            if (validCities.contains(c)) {
                cities.add(c);
            } else {
                warnings.add((isVi(locale) ? "Bỏ qua thành phố lạ: " : "Unknown city ignored: ") + c);
            }
        }
        List<String> districts = new ArrayList<>();
        for (String d : readStrings(input.path("districts"))) {
            if (validDistricts.contains(d)) {
                districts.add(d);
            } else {
                warnings.add((isVi(locale) ? "Bỏ qua quận lạ: " : "Unknown district ignored: ") + d);
            }
        }

        String propertyType = nullableString(input.path("propertyType"));
        if ("UNKNOWN".equals(propertyType)) propertyType = null;
        String status = nullableString(input.path("status"));
        if ("UNKNOWN".equals(status) || status == null) status = "FOR_SALE";

        Integer bedrooms = nullableInt(input.path("bedrooms"));
        Integer bathrooms = nullableInt(input.path("bathrooms"));
        BigDecimal minPrice = nullableBigDecimal(input.path("minPrice"));
        BigDecimal maxPrice = nullableBigDecimal(input.path("maxPrice"));
        BigDecimal minArea = nullableBigDecimal(input.path("minArea"));
        BigDecimal maxArea = nullableBigDecimal(input.path("maxArea"));
        String freeText = nullableString(input.path("freeText"));

        ParsedFilters filters = new ParsedFilters(
                propertyType,
                status,
                bedrooms,
                bathrooms,
                minPrice,
                maxPrice,
                minArea,
                maxArea,
                cities.isEmpty() ? null : cities,
                districts.isEmpty() ? null : districts,
                freeText
        );

        List<Chip> chips = buildChips(filters, locale);
        return new ParseResult(filters, chips, warnings);
    }

    private List<Chip> buildChips(ParsedFilters f, String locale) {
        boolean vi = isVi(locale);
        List<Chip> chips = new ArrayList<>();
        if (f.propertyType() != null) {
            chips.add(new Chip("propertyType", typeLabel(f.propertyType(), vi),
                    "propertyType", f.propertyType(), "high"));
        }
        if (f.status() != null) {
            chips.add(new Chip("status",
                    "FOR_RENT".equals(f.status()) ? (vi ? "Cho thuê" : "For rent")
                            : (vi ? "Bán" : "For sale"),
                    "status", f.status(), "high"));
        }
        if (f.bedrooms() != null) {
            chips.add(new Chip("bedrooms", f.bedrooms() + (vi ? " phòng ngủ" : " bedrooms"),
                    "bedrooms", f.bedrooms(), "high"));
        }
        if (f.bathrooms() != null) {
            chips.add(new Chip("bathrooms", f.bathrooms() + (vi ? " phòng tắm" : " bathrooms"),
                    "bathrooms", f.bathrooms(), "high"));
        }
        if (f.minPrice() != null) {
            chips.add(new Chip("minPrice", "≥ " + formatPrice(f.minPrice(), vi),
                    "minPrice", f.minPrice(), "high"));
        }
        if (f.maxPrice() != null) {
            chips.add(new Chip("maxPrice", "≤ " + formatPrice(f.maxPrice(), vi),
                    "maxPrice", f.maxPrice(), "high"));
        }
        if (f.minArea() != null) {
            chips.add(new Chip("minArea", "≥ " + f.minArea().stripTrailingZeros().toPlainString() + " m²",
                    "minArea", f.minArea(), "high"));
        }
        if (f.maxArea() != null) {
            chips.add(new Chip("maxArea", "≤ " + f.maxArea().stripTrailingZeros().toPlainString() + " m²",
                    "maxArea", f.maxArea(), "high"));
        }
        if (f.cities() != null && !f.cities().isEmpty()) {
            String label;
            if (f.cities().size() <= 3) {
                label = (vi ? "Khu vực: " : "Cities: ") + String.join(", ", f.cities());
            } else {
                label = (vi ? "Khu vực: " : "Cities: ") + f.cities().size()
                        + (vi ? " thành phố" : " cities");
            }
            chips.add(new Chip("cities", label, "cities", f.cities(), "medium"));
        }
        if (f.districts() != null && !f.districts().isEmpty()) {
            chips.add(new Chip("districts", String.join(", ", f.districts()),
                    "districts", f.districts(), "high"));
        }
        if (f.freeText() != null && !f.freeText().isBlank()) {
            chips.add(new Chip("freeText", f.freeText(), "freeText", f.freeText(), "low"));
        }
        return chips;
    }

    private static List<String> readStrings(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isArray()) return out;
        for (JsonNode el : node) {
            if (el.isTextual() && !el.asText().isBlank()) out.add(el.asText());
        }
        return out;
    }

    private static String nullableString(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        String v = n.asText();
        return v == null || v.isBlank() ? null : v;
    }

    private static Integer nullableInt(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull() || !n.canConvertToInt())
                ? null : n.asInt();
    }

    private static BigDecimal nullableBigDecimal(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        if (n.isNumber()) return n.decimalValue();
        if (n.isTextual()) {
            try {
                return new BigDecimal(n.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static boolean isVi(String locale) {
        return locale != null && locale.toLowerCase().startsWith("vi");
    }

    private static String typeLabel(String t, boolean vi) {
        return switch (t) {
            case "HOUSE" -> vi ? "Loại: Nhà" : "Type: House";
            case "APARTMENT" -> vi ? "Loại: Căn hộ" : "Type: Apartment";
            case "VILLA" -> vi ? "Loại: Biệt thự" : "Type: Villa";
            case "TOWNHOUSE" -> vi ? "Loại: Nhà phố" : "Type: Townhouse";
            default -> t;
        };
    }

    private static String formatPrice(BigDecimal amount, boolean vi) {
        BigDecimal billion = new BigDecimal("1000000000");
        BigDecimal million = new BigDecimal("1000000");
        if (amount.compareTo(billion) >= 0) {
            return amount.divide(billion).stripTrailingZeros().toPlainString()
                    + (vi ? " tỷ VND" : "B VND");
        }
        if (amount.compareTo(million) >= 0) {
            return amount.divide(million).stripTrailingZeros().toPlainString()
                    + (vi ? " triệu VND" : "M VND");
        }
        return amount.stripTrailingZeros().toPlainString() + " VND";
    }
}
