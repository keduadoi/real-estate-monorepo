package com.realestate.aisearch.parser;

import com.realestate.aisearch.dto.ParsedFilters;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexQueryParserTest {

    private final RegexQueryParser parser = new RegexQueryParser();

    @Test
    void mode_returnsRegex() {
        assertEquals(ParserMode.REGEX, parser.mode());
    }

    @Test
    void englishHouseUnder5Billion() {
        ParseResult r = parser.parse(
                "a house near the beach with 3 bedrooms, price cannot be over 5 billion vnd",
                "en");
        ParsedFilters f = r.filters();
        assertEquals("HOUSE", f.propertyType());
        assertEquals(3, f.bedrooms());
        assertEquals(0, new BigDecimal("5000000000").compareTo(f.maxPrice()));
        assertNull(f.minPrice(), "minPrice should be null when query only has 'cannot be over'");
        assertNotNull(f.cities());
        assertTrue(f.cities().contains("Đà Nẵng"));
        assertEquals("FOR_SALE", f.status());
    }

    @Test
    void vietnameseApartmentHaNoiUnder3Billion() {
        ParseResult r = parser.parse(
                "căn hộ 2PN gần trung tâm Hà Nội dưới 3 tỷ", "vi-VN");
        ParsedFilters f = r.filters();
        assertEquals("APARTMENT", f.propertyType());
        assertEquals(2, f.bedrooms());
        assertEquals(0, new BigDecimal("3000000000").compareTo(f.maxPrice()));
        assertNotNull(f.cities());
        assertTrue(f.cities().contains("Hà Nội"));
        assertEquals("FOR_SALE", f.status());
    }

    @Test
    void townhouseRentDistrict7() {
        ParseResult r = parser.parse(
                "nhà phố Quận 7 cho thuê dưới 20 triệu/tháng", "vi-VN");
        ParsedFilters f = r.filters();
        assertEquals("TOWNHOUSE", f.propertyType());
        assertEquals("FOR_RENT", f.status());
        assertEquals(0, new BigDecimal("20000000").compareTo(f.maxPrice()));
        assertNotNull(f.districts());
        assertTrue(f.districts().contains("Quận 7"));
    }

    @Test
    void villaDaNangFreeText() {
        ParseResult r = parser.parse(
                "biệt thự view biển Đà Nẵng, 4 phòng ngủ, dưới 30 tỷ", "vi-VN");
        ParsedFilters f = r.filters();
        assertEquals("VILLA", f.propertyType());
        assertEquals(4, f.bedrooms());
        assertEquals(0, new BigDecimal("30000000000").compareTo(f.maxPrice()));
        assertNotNull(f.cities());
        assertTrue(f.cities().contains("Đà Nẵng"));
    }

    @Test
    void cheapToRent_warnsAboutMissingPrice() {
        ParseResult r = parser.parse("something cheap to rent", "en");
        assertEquals("FOR_RENT", r.filters().status());
        assertNull(r.filters().maxPrice());
        // No price; should not crash and should emit a freeText chip or empty result.
    }

    @Test
    void coastalIntentExpandsToCoastalCities() {
        ParseResult r = parser.parse("house near the beach", "en");
        ParsedFilters f = r.filters();
        assertNotNull(f.cities());
        assertTrue(f.cities().size() >= 4);
        assertTrue(f.cities().contains("Đà Nẵng"));
        assertTrue(f.cities().contains("Nha Trang"));
    }
}
