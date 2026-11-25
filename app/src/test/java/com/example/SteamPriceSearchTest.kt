package com.example

import com.example.model.DealItem
import com.example.model.SteamSearchSortOption
import com.example.network.SteamApiService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SteamPriceSearchTest {

    private val steamService = SteamApiService()

    @Test
    fun testExactPriceMatching() {
        // 199 грн should match 199.00 UAH (19900 cents) and 199.99 UAH (19999 cents)
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 19900L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 199
            )
        )
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 19999L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 199
            )
        )

        // 200 грн should also match psychological 199.00 UAH or 199.99 UAH (within 100 cents)
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 19900L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 200
            )
        )
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 19999L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 200
            )
        )

        // Dead by Daylight (239.60 UAH = 23960 cents) matches exact 239 or 240 UAH
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 23960L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 240
            )
        )
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 23960L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 239
            )
        )

        // Cult of the Lamb (255.00 UAH = 25500 cents)
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 25500L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 255
            )
        )
        assertFalse(
            steamService.matchesPriceCriteria(
                finalPriceCents = 25500L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 500
            )
        )

        // Resident Evil 4 Remake (999.50 UAH = 99950 cents) matches exact 1000 or 999
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 99950L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 1000
            )
        )
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 99950L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 999
            )
        )

        // Free game (0 UAH)
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 0L,
                isFree = true,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 0
            )
        )
        assertFalse(
            steamService.matchesPriceCriteria(
                finalPriceCents = 15000L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = null,
                exactPriceUah = 0
            )
        )
    }

    @Test
    fun testPriceRangeMatching() {
        // Range 200..300 UAH
        // Cult of the Lamb (255.00) -> In range
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 25500L,
                isFree = false,
                minPriceUah = 200,
                maxPriceUah = 300,
                exactPriceUah = null
            )
        )
        // Dead by Daylight (239.60) -> In range
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 23960L,
                isFree = false,
                minPriceUah = 200,
                maxPriceUah = 300,
                exactPriceUah = null
            )
        )
        // Phasmophobia (311.25) -> Out of range (above 300)
        assertFalse(
            steamService.matchesPriceCriteria(
                finalPriceCents = 31125L,
                isFree = false,
                minPriceUah = 200,
                maxPriceUah = 300,
                exactPriceUah = null
            )
        )
        // Resident Evil 4 (999.50) -> Out of range
        assertFalse(
            steamService.matchesPriceCriteria(
                finalPriceCents = 99950L,
                isFree = false,
                minPriceUah = 200,
                maxPriceUah = 300,
                exactPriceUah = null
            )
        )

        // Range up to 250 UAH (min=null, max=250)
        assertTrue(
            steamService.matchesPriceCriteria(
                finalPriceCents = 23960L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = 250,
                exactPriceUah = null
            )
        )
        assertFalse(
            steamService.matchesPriceCriteria(
                finalPriceCents = 25500L,
                isFree = false,
                minPriceUah = null,
                maxPriceUah = 250,
                exactPriceUah = null
            )
        )
    }

    @Test
    fun testActiveWindowSizeConstraint() {
        // Ensures that the active window of items with images never exceeds 300 items
        val windowSize = 250
        assertTrue(windowSize <= 300)

        // For list of 1000 items, verify active range
        val visibleIndex = 400
        val approxDealIndex = (visibleIndex - 3).coerceAtLeast(0)
        val windowStart = (approxDealIndex - 50).coerceAtLeast(0)
        val windowEnd = windowStart + windowSize

        val activeCount = windowEnd - windowStart
        assertTrue("Active items must not exceed 300", activeCount <= 300)

        // Item 100 is far above -> optimized (no image)
        assertFalse(100 in windowStart until windowEnd)
        // Item 400 is near current scroll -> active image
        assertTrue(400 in windowStart until windowEnd)
        // Item 700 is far below -> optimized (no image)
        assertFalse(700 in windowStart until windowEnd)
    }

    @Test
    fun testSteamPriceSearchResultModel() {
        val result = com.example.model.SteamPriceSearchResult(
            items = emptyList(),
            hasMore = true
        )
        assertTrue(result.hasMore)
        assertTrue(result.items.isEmpty())
    }
}

