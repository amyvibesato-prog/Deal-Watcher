package com.example.model

import java.util.Calendar
import java.util.TimeZone

data class SeasonalEvent(
    val id: String,
    val title: String,
    val store: String, // "steam", "epic", "all"
    val description: String = "",
    val startDateFormatted: String,
    val endDateFormatted: String,
    val isActive: Boolean = false,
    val daysRemaining: Int? = null,
    val badgeText: String = "",
    val imageUrl: String? = null,
    val imageResId: Int? = null,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L
)

object SeasonalEventsProvider {

    // Dynamic banners fetched live from Steam
    private val dynamicBanners = mutableMapOf<String, String>()

    fun setDynamicBanner(eventId: String, bannerUrl: String) {
        if (bannerUrl.isNotBlank()) {
            dynamicBanners[eventId] = bannerUrl
        }
    }

    /**
     * Official Steam CDN banner URLs for events when they are actively running.
     * During the active event window, Steam serves these promotional headers.
     */
    fun getActiveSteamBanner(id: String): String {
        return when (id) {
            "steam_scream_v" -> "https://clan.akamai.steamstatic.com/images/41316928/8963334676d9ac39b0f4bb923e61989734533941.jpg"
            "steam_autobattler" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_autobattler2026/header.jpg"
            "steam_winter_sale" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_winter2026/header.jpg"
            "steam_desktop_companion" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_desktopcompanion/header.jpg"
            "steam_shopkeeper" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_shopkeeper/header.jpg"
            "steam_sheep" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_sheep/header.jpg"
            "steam_local_coop" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_localcoop/header.jpg"
            "steam_next_fest_feb" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_nextfest_feb2027/header.jpg"
            "steam_rhythm" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_rhythm/header.jpg"
            "steam_spring_sale" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_spring2027/header.jpg"
            "steam_dino_robots" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_dinorobots/header.jpg"
            "steam_racing" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_racing/header.jpg"
            "steam_witches" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_witches/header.jpg"
            "steam_fighting" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_fighting/header.jpg"
            "steam_rts" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_rts/header.jpg"
            "steam_climbing" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_climbing/header.jpg"
            "steam_next_fest_june" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_nextfest_june2027/header.jpg"
            "steam_summer_sale" -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_summer2027/header.jpg"
            else -> "https://cdn.cloudflare.steamstatic.com/steam/clusters/sale_${id.removePrefix("steam_")}/header.jpg"
        }
    }

    fun getActiveImageResId(id: String): Int? {
        return when (id) {
            "steam_scream_v" -> com.example.R.drawable.banner_steam_scream
            else -> null
        }
    }

    private fun createTimestamp(year: Int, month: Int, day: Int, hour: Int = 20): Long {
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

    fun getAllEvents(now: Long = System.currentTimeMillis()): List<SeasonalEvent> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = cal.get(Calendar.YEAR)

        val screamStart = createTimestamp(currentYear, 10, 26, 17)
        val screamEnd = createTimestamp(currentYear, 11, 2, 20)

        val autoBattlerStart = createTimestamp(currentYear, 11, 16, 20)
        val autoBattlerEnd = createTimestamp(currentYear, 11, 23, 20)

        val winterStart = createTimestamp(currentYear, 12, 17, 20)
        val winterEnd = createTimestamp(currentYear + 1, 1, 4, 20)

        val desktopCompanionStart = createTimestamp(currentYear + 1, 1, 14, 20)
        val desktopCompanionEnd = createTimestamp(currentYear + 1, 1, 18, 20)

        val shopStart = createTimestamp(currentYear + 1, 1, 25, 20)
        val shopEnd = createTimestamp(currentYear + 1, 2, 1, 20)

        val sheepStart = createTimestamp(currentYear + 1, 2, 4, 20)
        val sheepEnd = createTimestamp(currentYear + 1, 2, 8, 20)

        val localCoopStart = createTimestamp(currentYear + 1, 2, 8, 20)
        val localCoopEnd = createTimestamp(currentYear + 1, 2, 15, 20)

        val nextFestFebStart = createTimestamp(currentYear + 1, 2, 22, 20)
        val nextFestFebEnd = createTimestamp(currentYear + 1, 3, 1, 20)

        val rhythmStart = createTimestamp(currentYear + 1, 3, 8, 20)
        val rhythmEnd = createTimestamp(currentYear + 1, 3, 15, 20)

        val springStart = createTimestamp(currentYear + 1, 3, 18, 20)
        val springEnd = createTimestamp(currentYear + 1, 3, 25, 20)

        val dinoRobotsStart = createTimestamp(currentYear + 1, 3, 29, 20)
        val dinoRobotsEnd = createTimestamp(currentYear + 1, 4, 5, 20)

        val racingStart = createTimestamp(currentYear + 1, 4, 12, 20)
        val racingEnd = createTimestamp(currentYear + 1, 4, 19, 20)

        val witchStart = createTimestamp(currentYear + 1, 4, 22, 20)
        val witchEnd = createTimestamp(currentYear + 1, 4, 26, 20)

        val fightStart = createTimestamp(currentYear + 1, 4, 26, 20)
        val fightEnd = createTimestamp(currentYear + 1, 5, 3, 20)

        val rtsStart = createTimestamp(currentYear + 1, 5, 10, 20)
        val rtsEnd = createTimestamp(currentYear + 1, 5, 17, 20)

        val climbingStart = createTimestamp(currentYear + 1, 5, 31, 20)
        val climbingEnd = createTimestamp(currentYear + 1, 6, 3, 20)

        val nextFestJuneStart = createTimestamp(currentYear + 1, 6, 14, 20)
        val nextFestJuneEnd = createTimestamp(currentYear + 1, 6, 21, 20)

        val summerStart = createTimestamp(currentYear + 1, 6, 24, 20)
        val summerEnd = createTimestamp(currentYear + 1, 7, 8, 20)

        val rawList = listOf(
            SeasonalEvent(
                id = "steam_scream_v",
                title = "Фестиваль «Страшно в Steam 5»",
                store = "steam",
                description = "Ігри, які належать до категорії жахів, а також явно присвячені Геловіну.",
                startDateFormatted = "26 жовтня",
                endDateFormatted = "2 листопада, 20:00",
                badgeText = "АКТУАЛЬНИЙ СЕЗОН В STEAM",
                startTimeMillis = screamStart,
                endTimeMillis = screamEnd
            ),
            SeasonalEvent(
                id = "steam_autobattler",
                title = "Фестиваль рольових автобоїв",
                store = "steam",
                description = "Ігри, де бої відбуваються автоматично, але для перемоги вам потрібно готувати й покращувати свої бойові одиниці.",
                startDateFormatted = "16 листопада",
                endDateFormatted = "23 листопада, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = autoBattlerStart,
                endTimeMillis = autoBattlerEnd
            ),
            SeasonalEvent(
                id = "steam_winter_sale",
                title = "Зимовий розпродаж Steam 2026",
                store = "steam",
                description = "Чотири рази на рік ми проводимо сезонні розпродажі в Steam. Головний зимовий розпродаж року.",
                startDateFormatted = "17 грудня",
                endDateFormatted = "4 січня, 20:00",
                badgeText = "ГОЛОВНИЙ РОЗПРОДАЖ РОКУ",
                startTimeMillis = winterStart,
                endTimeMillis = winterEnd
            ),
            SeasonalEvent(
                id = "steam_desktop_companion",
                title = "Фестиваль настільних компаньйонів",
                store = "steam",
                description = "Ігри, які тусуються на вашому робочому столі та складають вам товариство, поки ви зайняті іншими справами.",
                startDateFormatted = "14 січня",
                endDateFormatted = "18 січня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = desktopCompanionStart,
                endTimeMillis = desktopCompanionEnd
            ),
            SeasonalEvent(
                id = "steam_shopkeeper",
                title = "Фестиваль крамарства",
                store = "steam",
                description = "Ігри про управління крамницею.",
                startDateFormatted = "25 січня",
                endDateFormatted = "1 лютого, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = shopStart,
                endTimeMillis = shopEnd
            ),
            SeasonalEvent(
                id = "steam_sheep",
                title = "Фестиваль овець",
                store = "steam",
                description = "Ігри, присвячені вівцям, козам або іншим тваринам підродини козлових.",
                startDateFormatted = "4 лютого",
                endDateFormatted = "8 лютого, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = sheepStart,
                endTimeMillis = sheepEnd
            ),
            SeasonalEvent(
                id = "steam_local_coop",
                title = "Фестиваль локальних кооперативних ігор",
                store = "steam",
                description = "Ігри, у які можна грати разом на одному пристрої (з підтримкою локального кооперативного режиму).",
                startDateFormatted = "8 лютого",
                endDateFormatted = "15 лютого, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = localCoopStart,
                endTimeMillis = localCoopEnd
            ),
            SeasonalEvent(
                id = "steam_next_fest_feb",
                title = "Фестиваль новинок Steam",
                store = "steam",
                description = "Демоверсії майбутніх невипущених ігор, трансляції наживо та знайомство з розробниками.",
                startDateFormatted = "22 лютого",
                endDateFormatted = "1 березня, 20:00",
                badgeText = "ФЕСТИВАЛЬ НОВИНОК",
                startTimeMillis = nextFestFebStart,
                endTimeMillis = nextFestFebEnd
            ),
            SeasonalEvent(
                id = "steam_rhythm",
                title = "Фестиваль ритму",
                store = "steam",
                description = "Ігри, у яких гравця винагороджують за натискання на ноти, стрілки, кулі тощо — в такт.",
                startDateFormatted = "8 березня",
                endDateFormatted = "15 березня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = rhythmStart,
                endTimeMillis = rhythmEnd
            ),
            SeasonalEvent(
                id = "steam_spring_sale",
                title = "Весняний розпродаж 2027",
                store = "steam",
                description = "Чотири рази на рік ми проводимо сезонні розпродажі в Steam. Великий сезонний весняний розпродаж.",
                startDateFormatted = "18 березня",
                endDateFormatted = "25 березня, 20:00",
                badgeText = "СЕЗОННИЙ РОЗПРОДАЖ",
                startTimeMillis = springStart,
                endTimeMillis = springEnd
            ),
            SeasonalEvent(
                id = "steam_dino_robots",
                title = "Фестиваль «Динозаври проти роботів»",
                store = "steam",
                description = "Ігри про динозаврів чи роботів. Або і тих, і інших. Чи ще краще — динороботів!",
                startDateFormatted = "29 березня",
                endDateFormatted = "5 квітня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = dinoRobotsStart,
                endTimeMillis = dinoRobotsEnd
            ),
            SeasonalEvent(
                id = "steam_racing",
                title = "Фестиваль перегонів",
                store = "steam",
                description = "Ігри про шалену швидкість. Швидше за інших людей, швидше за комп’ютер, швидше за вчорашнього себе.",
                startDateFormatted = "12 квітня",
                endDateFormatted = "19 квітня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = racingStart,
                endTimeMillis = racingEnd
            ),
            SeasonalEvent(
                id = "steam_witches",
                title = "Фестиваль відьом",
                store = "steam",
                description = "Відьомські ігри!",
                startDateFormatted = "22 квітня",
                endDateFormatted = "26 квітня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = witchStart,
                endTimeMillis = witchEnd
            ),
            SeasonalEvent(
                id = "steam_fighting",
                title = "Фестиваль поєдинків",
                store = "steam",
                description = "Ігри-поєдинки — не просто ігри із бійками, а ті, у які грає спільнота гравців у файтинги.",
                startDateFormatted = "26 квітня",
                endDateFormatted = "3 травня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = fightStart,
                endTimeMillis = fightEnd
            ),
            SeasonalEvent(
                id = "steam_rts",
                title = "Фестиваль стратегій у реальному часі",
                store = "steam",
                description = "Ігри в реальному часі та зі стратегічними аспектами високого рівня, планування будівництва й управління ресурсами.",
                startDateFormatted = "10 травня",
                endDateFormatted = "17 травня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = rtsStart,
                endTimeMillis = rtsEnd
            ),
            SeasonalEvent(
                id = "steam_climbing",
                title = "Фестиваль альпінізму",
                store = "steam",
                description = "Ігри про сходження на гори.",
                startDateFormatted = "31 травня",
                endDateFormatted = "3 червня, 20:00",
                badgeText = "ТЕМАТИЧНИЙ ФЕСТИВАЛЬ",
                startTimeMillis = climbingStart,
                endTimeMillis = climbingEnd
            ),
            SeasonalEvent(
                id = "steam_next_fest_june",
                title = "Фестиваль новинок Steam (червень)",
                store = "steam",
                description = "Демоверсії майбутніх невипущених ігор, трансляції наживо та знайомство з розробниками.",
                startDateFormatted = "14 червня",
                endDateFormatted = "21 червня, 20:00",
                badgeText = "ФЕСТИВАЛЬ НОВИНОК",
                startTimeMillis = nextFestJuneStart,
                endTimeMillis = nextFestJuneEnd
            ),
            SeasonalEvent(
                id = "steam_summer_sale",
                title = "Літній розпродаж 2027",
                store = "steam",
                description = "Головний літній сезонний розпродаж у Steam.",
                startDateFormatted = "24 червня",
                endDateFormatted = "8 липня, 20:00",
                badgeText = "ГОЛОВНИЙ РОЗПРОДАЖ РОКУ",
                startTimeMillis = summerStart,
                endTimeMillis = summerEnd
            )
        )

        return rawList.map { event ->
            val isActive = now in event.startTimeMillis..event.endTimeMillis
            val days = when {
                isActive -> {
                    val diff = event.endTimeMillis - now
                    maxOf(1, (diff / (1000 * 60 * 60 * 24)).toInt())
                }
                now < event.startTimeMillis -> {
                    val diff = event.startTimeMillis - now
                    maxOf(1, (diff / (1000 * 60 * 60 * 24)).toInt())
                }
                else -> null
            }

            // Назва, опис і дати беруться з сайту заздалегідь.
            // А в дату коли івент актуальний (isActive == true), додаток підтягує банер зі Steam.
            // Для майбутніх подій bannerUrl = null, щоб картинка не дублювалася.
            val bannerUrl = if (isActive) {
                dynamicBanners[event.id] ?: getActiveSteamBanner(event.id)
            } else {
                null
            }

            val resId = if (isActive) {
                getActiveImageResId(event.id)
            } else {
                null
            }

            event.copy(
                isActive = isActive,
                daysRemaining = days,
                imageUrl = bannerUrl,
                imageResId = resId
            )
        }
    }

    fun getActiveEvent(now: Long = System.currentTimeMillis()): SeasonalEvent {
        val events = getAllEvents(now)
        val currentSteam = events.firstOrNull { it.store == "steam" && it.isActive }
        if (currentSteam != null) return currentSteam

        val current = events.firstOrNull { it.isActive }
        if (current != null) return current

        val upcomingSteam = events.filter { it.store == "steam" && it.startTimeMillis > now }.minByOrNull { it.startTimeMillis }
        if (upcomingSteam != null) return upcomingSteam

        val upcoming = events.filter { it.startTimeMillis > now }.minByOrNull { it.startTimeMillis }
        if (upcoming != null) return upcoming

        return events.first()
    }

    val currentActiveEvent: SeasonalEvent
        get() = getActiveEvent()

    val allEvents: List<SeasonalEvent>
        get() = getAllEvents()
}
