package com.example

import com.example.model.SeasonalEventsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeasonalBannerUpdateTest {

    private fun getTimestamp(year: Int, month: Int, day: Int, hour: Int = 20): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun testCurrentActiveSeasonalEvent_isSteamScreamVFest() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        // Simulate October 28 at 18:00 UTC
        val oct28 = getTimestamp(currentYear, 10, 28, 18)

        val activeEvent = SeasonalEventsProvider.getActiveEvent(oct28)
        assertNotNull(activeEvent)
        assertEquals("steam_scream_v", activeEvent.id)
        assertTrue("Title should match official Ukrainian Steamworks title", activeEvent.title.contains("Страшно в Steam 5") || activeEvent.title.contains("Steam Scream"))
        assertEquals("26 жовтня", activeEvent.startDateFormatted)
        assertEquals("2 листопада, 20:00", activeEvent.endDateFormatted)
        assertTrue("Event should be active during October 28", activeEvent.isActive)
        assertTrue("Days remaining should be positive", (activeEvent.daysRemaining ?: 0) > 0)
        assertFalse("Description must not contain the word 'офіційний'", activeEvent.description.contains("офіційний", ignoreCase = true))
        assertNotNull("Image url must be provided", activeEvent.imageUrl)
    }

    @Test
    fun testBannerUpdatesDynamicDateShift_switchesToAutoBattlerFest() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        // Simulate November 18 at 21:00 UTC (Auto-Battler Fest active)
        val nov18 = getTimestamp(currentYear, 11, 18, 21)

        val activeEvent = SeasonalEventsProvider.getActiveEvent(nov18)
        assertNotNull(activeEvent)
        assertEquals("steam_autobattler", activeEvent.id)
        assertTrue(activeEvent.title.contains("Фестиваль рольових автобоїв"))
        assertTrue("Auto-battler event should be active in mid November", activeEvent.isActive)
        assertTrue("Should have days remaining", (activeEvent.daysRemaining ?: 0) > 0)
    }

    @Test
    fun testBannerUpdatesDynamicDateShift_switchesToWinterSale() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        // Simulate December 25 at 12:00 UTC (Winter Sale active)
        val dec25 = getTimestamp(currentYear, 12, 25, 12)

        val activeEvent = SeasonalEventsProvider.getActiveEvent(dec25)
        assertNotNull(activeEvent)
        assertEquals("steam_winter_sale", activeEvent.id)
        assertTrue(activeEvent.title.contains("Зимовий розпродаж"))
        assertTrue("Winter event should be active on Dec 25", activeEvent.isActive)
    }

    @Test
    fun testFutureEventsHaveNoBannersUntilActive() {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        // Simulate October 28 (Scream Fest is active)
        val oct28 = getTimestamp(currentYear, 10, 28, 18)

        val allEvents = SeasonalEventsProvider.getAllEvents(oct28)
        val screamEvent = allEvents.first { it.id == "steam_scream_v" }
        assertTrue("Active event must have banner", screamEvent.isActive && !screamEvent.imageUrl.isNullOrBlank())

        val upcomingEvents = allEvents.filter { !it.isActive }
        assertTrue("There must be upcoming events", upcomingEvents.isNotEmpty())
        for (upcoming in upcomingEvents) {
            // Future events must NOT have duplicate banners from active event
            assertFalse(
                "Upcoming event '${upcoming.title}' must not duplicate Scream Fest image",
                upcoming.imageUrl == screamEvent.imageUrl && !upcoming.imageUrl.isNullOrBlank()
            )
            assertEquals(
                "Upcoming event '${upcoming.title}' should have null image until active",
                null,
                upcoming.imageUrl
            )
        }
    }

    @Test
    fun testCuratedDealsUpdateInSyncWithActiveEvent() {
        // Test that Scream V deals include horror titles
        val screamDeals = com.example.repository.DealRepository::class.java
            .getDeclaredMethod("getCuratedSteamScreamFestDeals")
            .apply { isAccessible = true }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val repo = com.example.repository.DealRepository(
            dealDao = db.dealDao(),
            preferencesManager = com.example.data.PreferencesManager(context),
            notificationManager = com.example.notification.DealNotificationManager(context)
        )

        val screamList = repo.getCuratedSeasonalDeals("steam_scream_v")
        assertTrue("Scream deals should not be empty", screamList.isNotEmpty())
        assertTrue("All scream deals must have > 0 discount", screamList.all { it.discountPercent > 0 })
        assertTrue("All scream deals must be steam", screamList.all { it.store == "steam" })
        assertTrue("All scream deals must have isSeasonal true", screamList.all { it.isSeasonal })

        // Check key titles
        val titles = screamList.map { it.title }
        assertTrue("Must include Resident Evil 4 Remake", titles.any { it.contains("Resident Evil", ignoreCase = true) })
        assertTrue("Must include The Outlast Trials", titles.any { it.contains("Outlast", ignoreCase = true) })
        assertTrue("Must include Still Wakes the Deep", titles.any { it.contains("Still Wakes the Deep", ignoreCase = true) })

        // When event changes to Autumn sale, deals update in sync
        val autumnList = repo.getCuratedSeasonalDeals("steam_autumn_sale")
        assertTrue("Autumn deals should not be empty", autumnList.isNotEmpty())
        assertTrue("Must include Cyberpunk 2077 in Autumn Sale", autumnList.any { it.title.contains("Cyberpunk", ignoreCase = true) })
    }
}
