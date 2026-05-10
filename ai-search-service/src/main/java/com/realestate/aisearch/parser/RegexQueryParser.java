package com.realestate.aisearch.parser;

import com.realestate.aisearch.dto.Chip;
import com.realestate.aisearch.dto.ParsedFilters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-process regex parser. No external calls, no API keys. Handles the patterns
 * we explicitly encode (price magnitudes, bed/bath counts, area, property type,
 * status keywords, curated city/district names). Anything not consumed becomes
 * {@code freeText} or a warning.
 */
@Slf4j
@Component("regexQueryParser")
public class RegexQueryParser implements QueryParser {

    // ---- Price ----------------------------------------------------------------
    // tỷ / ty / billion / b   -> 1e9
    // triệu / trieu / million / m / tr  -> 1e6
    private static final Pattern PRICE_MAX = Pattern.compile(
            "(?:duoi|under|max|toi da|khong qua|≤|<=|<|cannot be over|no more than|not over)\\s*"
                    + "(\\d+[\\.,]?\\d*)\\s*(ty|tỷ|billion|b|trieu|triệu|million|m|tr|đ|d|vnd)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRICE_MIN = Pattern.compile(
            "(?:tren|over|min|toi thieu|≥|>=|>|at least|from|tu)\\s*"
                    + "(\\d+[\\.,]?\\d*)\\s*(ty|tỷ|billion|b|trieu|triệu|million|m|tr|đ|d|vnd)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRICE_RANGE = Pattern.compile(
            "(?:tu|from)\\s*(\\d+[\\.,]?\\d*)\\s*(ty|tỷ|billion|b|trieu|triệu|million|m|tr)?\\s*"
                    + "(?:den|to|-)\\s*(\\d+[\\.,]?\\d*)\\s*(ty|tỷ|billion|b|trieu|triệu|million|m|tr)?",
            Pattern.CASE_INSENSITIVE);

    // ---- Beds / baths ---------------------------------------------------------
    private static final Pattern BEDS = Pattern.compile(
            "(\\d{1,2})\\s*(?:pn|phong ngu|phòng ngủ|bedrooms?|beds?|br)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BATHS = Pattern.compile(
            "(\\d{1,2})\\s*(?:wc|toilet|phong tam|phòng tắm|bathrooms?|baths?|ba)\\b",
            Pattern.CASE_INSENSITIVE);

    // ---- Area -----------------------------------------------------------------
    private static final Pattern AREA = Pattern.compile(
            "(\\d+[\\.,]?\\d*)\\s*(?:m2|m²|sqm|m\\^2)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AREA_MAX = Pattern.compile(
            "(?:duoi|under|max|≤|<=|<)\\s*(\\d+[\\.,]?\\d*)\\s*(?:m2|m²|sqm)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AREA_MIN = Pattern.compile(
            "(?:tren|over|min|≥|>=|>|at least)\\s*(\\d+[\\.,]?\\d*)\\s*(?:m2|m²|sqm)\\b",
            Pattern.CASE_INSENSITIVE);

    // ---- Property type --------------------------------------------------------
    // Order matters: more specific patterns first (townhouse before house).
    private static final List<TypePattern> TYPE_PATTERNS = List.of(
            new TypePattern("TOWNHOUSE", Pattern.compile(
                    "\\b(nha pho|nhà phố|nha mat pho|townhouse|town house|shophouse)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new TypePattern("VILLA", Pattern.compile(
                    "\\b(biet thu|biệt thự|villa)\\b", Pattern.CASE_INSENSITIVE)),
            new TypePattern("APARTMENT", Pattern.compile(
                    "\\b(can ho|căn hộ|chung cu|chung cư|apartment|condo|flat)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new TypePattern("HOUSE", Pattern.compile(
                    "\\b(nha|nhà|house|home)\\b", Pattern.CASE_INSENSITIVE))
    );

    // ---- Status ---------------------------------------------------------------
    private static final Pattern RENT_KEYWORDS = Pattern.compile(
            "/thang|/tháng|moi thang|mỗi tháng|cho thue|cho thuê|for rent|to rent\\b|rent\\b|monthly\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SALE_KEYWORDS = Pattern.compile(
            "\\b(ban|bán|for sale|to sell|to buy|mua)\\b",
            Pattern.CASE_INSENSITIVE);

    private record TypePattern(String type, Pattern pattern) {}

    @Override
    public ParserMode mode() {
        return ParserMode.REGEX;
    }

    @Override
    public ParseResult parse(String query, String locale) {
        long start = System.nanoTime();
        String original = query == null ? "" : query.trim();
        String folded = Locations.fold(original);

        List<String> warnings = new ArrayList<>();
        List<Chip> chips = new ArrayList<>();

        // --- Status (rent vs sale) ------------------------------------------
        boolean rentMatched = RENT_KEYWORDS.matcher(folded).find();
        boolean saleMatched = SALE_KEYWORDS.matcher(folded).find();
        String status = rentMatched ? "FOR_RENT" : (saleMatched ? "FOR_SALE" : "FOR_SALE");
        if (rentMatched) {
            chips.add(new Chip("status", labelFor("status", "FOR_RENT", locale),
                    "status", "FOR_RENT", "high"));
        } else if (saleMatched) {
            chips.add(new Chip("status", labelFor("status", "FOR_SALE", locale),
                    "status", "FOR_SALE", "high"));
        }

        // --- Price ----------------------------------------------------------
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        Matcher rangeM = PRICE_RANGE.matcher(folded);
        String priceWorking = folded;
        if (rangeM.find()) {
            minPrice = parseAmount(rangeM.group(1), rangeM.group(2));
            maxPrice = parseAmount(rangeM.group(3),
                    rangeM.group(4) != null ? rangeM.group(4) : rangeM.group(2));
        } else {
            // Match PRICE_MAX first and erase its span so its qualifier (e.g. "over"
            // inside "cannot be over") doesn't double-match in PRICE_MIN.
            Matcher maxM = PRICE_MAX.matcher(priceWorking);
            if (maxM.find()) {
                maxPrice = parseAmount(maxM.group(1), maxM.group(2));
                priceWorking = priceWorking.substring(0, maxM.start())
                        + " ".repeat(maxM.end() - maxM.start())
                        + priceWorking.substring(maxM.end());
            }
            Matcher minM = PRICE_MIN.matcher(priceWorking);
            if (minM.find()) {
                minPrice = parseAmount(minM.group(1), minM.group(2));
            }
        }
        if (minPrice != null) {
            chips.add(new Chip("minPrice", "≥ " + formatPrice(minPrice, locale),
                    "minPrice", minPrice, "high"));
        }
        if (maxPrice != null) {
            chips.add(new Chip("maxPrice", "≤ " + formatPrice(maxPrice, locale),
                    "maxPrice", maxPrice, "high"));
        }

        // --- Beds / baths ---------------------------------------------------
        Integer bedrooms = null;
        Integer bathrooms = null;
        Matcher bedsM = BEDS.matcher(folded);
        if (bedsM.find()) {
            int n = Integer.parseInt(bedsM.group(1));
            if (n >= 0 && n <= 20) {
                bedrooms = n;
                chips.add(new Chip("bedrooms", n + (isVi(locale) ? " phòng ngủ" : " bedrooms"),
                        "bedrooms", n, "high"));
            }
        }
        Matcher bathsM = BATHS.matcher(folded);
        if (bathsM.find()) {
            int n = Integer.parseInt(bathsM.group(1));
            if (n >= 0 && n <= 20) {
                bathrooms = n;
                chips.add(new Chip("bathrooms", n + (isVi(locale) ? " phòng tắm" : " bathrooms"),
                        "bathrooms", n, "high"));
            }
        }

        // --- Area -----------------------------------------------------------
        BigDecimal minArea = null;
        BigDecimal maxArea = null;
        Matcher areaMaxM = AREA_MAX.matcher(folded);
        if (areaMaxM.find()) {
            maxArea = new BigDecimal(areaMaxM.group(1).replace(',', '.'));
            chips.add(new Chip("maxArea", "≤ " + maxArea.stripTrailingZeros().toPlainString() + " m²",
                    "maxArea", maxArea, "high"));
        }
        Matcher areaMinM = AREA_MIN.matcher(folded);
        if (areaMinM.find()) {
            minArea = new BigDecimal(areaMinM.group(1).replace(',', '.'));
            chips.add(new Chip("minArea", "≥ " + minArea.stripTrailingZeros().toPlainString() + " m²",
                    "minArea", minArea, "high"));
        }
        if (minArea == null && maxArea == null) {
            Matcher areaM = AREA.matcher(folded);
            if (areaM.find()) {
                BigDecimal area = new BigDecimal(areaM.group(1).replace(',', '.'));
                minArea = area;
                chips.add(new Chip("minArea", "≥ " + area.stripTrailingZeros().toPlainString() + " m²",
                        "minArea", area, "low"));
            }
        }

        // --- Property type --------------------------------------------------
        String propertyType = null;
        for (TypePattern tp : TYPE_PATTERNS) {
            if (tp.pattern.matcher(folded).find()) {
                propertyType = tp.type;
                chips.add(new Chip("propertyType", labelFor("propertyType", tp.type, locale),
                        "propertyType", tp.type, "high"));
                break;
            }
        }

        // --- Cities (curated alias map + coast keyword expansion) -----------
        Set<String> cityHits = new LinkedHashSet<>();
        for (var entry : Locations.CITY_ALIASES.entrySet()) {
            if (containsWord(folded, entry.getKey())) {
                cityHits.add(entry.getValue());
            }
        }
        for (String canonical : Locations.CITIES) {
            if (containsWord(folded, Locations.fold(canonical))) {
                cityHits.add(canonical);
            }
        }
        boolean coastalIntent = Locations.COAST_KEYWORDS.stream().anyMatch(k -> containsWord(folded, k));
        if (coastalIntent && cityHits.isEmpty()) {
            cityHits.addAll(Locations.COASTAL_CITIES);
            chips.add(new Chip("cities",
                    (isVi(locale) ? "Vùng ven biển: " : "Coastal: ") + cityHits.size()
                            + (isVi(locale) ? " thành phố" : " cities"),
                    "cities", new ArrayList<>(cityHits), "medium"));
        } else if (!cityHits.isEmpty()) {
            String label = String.join(", ", cityHits);
            chips.add(new Chip("cities", (isVi(locale) ? "Khu vực: " : "Cities: ") + label,
                    "cities", new ArrayList<>(cityHits), "high"));
        }

        // --- Districts ------------------------------------------------------
        Set<String> districtHits = new LinkedHashSet<>();
        for (var entry : Locations.DISTRICT_ALIASES.entrySet()) {
            if (containsWord(folded, entry.getKey())) {
                districtHits.add(entry.getValue());
            }
        }
        if (!districtHits.isEmpty()) {
            chips.add(new Chip("districts", String.join(", ", districtHits),
                    "districts", new ArrayList<>(districtHits), "high"));
        }

        // --- Free text & warnings ------------------------------------------
        // Heuristic: if user mentioned a price number with no qualifier, warn.
        if (minPrice == null && maxPrice == null) {
            Pattern bareNumber = Pattern.compile(
                    "\\b\\d+[\\.,]?\\d*\\s*(?:ty|tỷ|billion|trieu|triệu|million)\\b",
                    Pattern.CASE_INSENSITIVE);
            if (bareNumber.matcher(folded).find()) {
                warnings.add(isVi(locale)
                        ? "Số tiền không rõ là tối đa hay tối thiểu — bỏ qua"
                        : "Ambiguous price (min/max not specified) — ignored");
            }
        }

        String freeText = chips.isEmpty() ? original : null;
        if (freeText != null) {
            chips.add(new Chip("freeText", freeText, "freeText", freeText, "low"));
        }

        ParsedFilters filters = new ParsedFilters(
                propertyType,
                status,
                bedrooms,
                bathrooms,
                minPrice,
                maxPrice,
                minArea,
                maxArea,
                cityHits.isEmpty() ? null : new ArrayList<>(cityHits),
                districtHits.isEmpty() ? null : new ArrayList<>(districtHits),
                freeText
        );

        if (log.isDebugEnabled()) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.debug("Regex parse done in {} ms — chips={}, warnings={}", ms, chips.size(), warnings.size());
        }
        return new ParseResult(filters, chips, warnings);
    }

    // -------------------------------------------------------------------------

    private static BigDecimal parseAmount(String numStr, String unit) {
        BigDecimal n = new BigDecimal(numStr.replace(",", "."));
        BigDecimal multiplier = magnitudeMultiplier(unit);
        return n.multiply(multiplier);
    }

    private static BigDecimal magnitudeMultiplier(String unit) {
        if (unit == null) return BigDecimal.ONE;
        String u = unit.toLowerCase();
        return switch (u) {
            case "ty", "tỷ", "billion", "b" -> new BigDecimal("1000000000");
            case "trieu", "triệu", "million", "m", "tr" -> new BigDecimal("1000000");
            default -> BigDecimal.ONE; // đ, d, vnd
        };
    }

    private static boolean containsWord(String haystack, String needle) {
        if (needle == null || needle.isBlank()) return false;
        // Use simple substring with word boundary fallback for short tokens.
        if (needle.length() <= 2) {
            return Pattern.compile("\\b" + Pattern.quote(needle) + "\\b").matcher(haystack).find();
        }
        return haystack.contains(needle);
    }

    private static boolean isVi(String locale) {
        return locale != null && locale.toLowerCase().startsWith("vi");
    }

    private static String labelFor(String field, Object value, String locale) {
        boolean vi = isVi(locale);
        if ("propertyType".equals(field)) {
            String v = String.valueOf(value);
            return switch (v) {
                case "HOUSE" -> vi ? "Loại: Nhà" : "Type: House";
                case "APARTMENT" -> vi ? "Loại: Căn hộ" : "Type: Apartment";
                case "VILLA" -> vi ? "Loại: Biệt thự" : "Type: Villa";
                case "TOWNHOUSE" -> vi ? "Loại: Nhà phố" : "Type: Townhouse";
                default -> v;
            };
        }
        if ("status".equals(field)) {
            String v = String.valueOf(value);
            return switch (v) {
                case "FOR_SALE" -> vi ? "Bán" : "For sale";
                case "FOR_RENT" -> vi ? "Cho thuê" : "For rent";
                default -> v;
            };
        }
        return String.valueOf(value);
    }

    private static String formatPrice(BigDecimal amount, String locale) {
        BigDecimal billion = new BigDecimal("1000000000");
        BigDecimal million = new BigDecimal("1000000");
        boolean vi = isVi(locale);
        if (amount.compareTo(billion) >= 0) {
            BigDecimal v = amount.divide(billion);
            return v.stripTrailingZeros().toPlainString() + (vi ? " tỷ VND" : "B VND");
        }
        if (amount.compareTo(million) >= 0) {
            BigDecimal v = amount.divide(million);
            return v.stripTrailingZeros().toPlainString() + (vi ? " triệu VND" : "M VND");
        }
        return amount.stripTrailingZeros().toPlainString() + " VND";
    }
}
