package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.DealItem

class DealNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val steamChannel = NotificationChannel(
                CHANNEL_STEAM_DEALS,
                "Знижки Steam Wishlist",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про знижки на ігри зі списку бажаного Steam"
                enableVibration(true)
            }

            val epicChannel = NotificationChannel(
                CHANNEL_EPIC_GIVEAWAYS,
                "Безкоштовні ігри Epic Games",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про безкоштовні роздачі ігор в Epic Games Store"
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(steamChannel)
            manager?.createNotificationChannel(epicChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    fun notifyNewSteamDeal(deal: DealItem) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            deal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priceText = if (deal.formattedFinalPrice.isNotBlank()) deal.formattedFinalPrice else "${deal.finalPrice / 100} грн"
        val content = "${deal.title} — знижка -${deal.discountPercent}%! Тепер ${priceText}"

        val builder = NotificationCompat.Builder(context, CHANNEL_STEAM_DEALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔥 Нова знижка у Wishlist!")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(deal.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun notifyNewEpicGiveaway(deal: DealItem) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            deal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (deal.isUpcoming) {
            "Скоро безкоштовно: ${deal.title} в Epic Games Store!"
        } else {
            "Гра «${deal.title}» зараз роздається БЕЗКОШТОВНО в Epic Games!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_EPIC_GIVEAWAYS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎁 Нова роздача в Epic Games!")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(deal.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun notifySteamDeals(deals: List<DealItem>) {
        if (!hasNotificationPermission() || deals.isEmpty()) return
        if (deals.size == 1) {
            notifyNewSteamDeal(deals.first())
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_STEAM_SUMMARY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🔥 ${deals.size} нові знижки у Steam Wishlist!"
        val firstDeal = deals.first()
        val summaryText = "${firstDeal.title} (-${firstDeal.discountPercent}%) та ще ${deals.size - 1} ігор"

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        for (deal in deals.take(5)) {
            val price = if (deal.formattedFinalPrice.isNotBlank()) deal.formattedFinalPrice else "${deal.finalPrice / 100} грн"
            inboxStyle.addLine("${deal.title} — -${deal.discountPercent}% ($price)")
        }
        if (deals.size > 5) {
            inboxStyle.setSummaryText("+ ще ${deals.size - 5} знижок")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_STEAM_DEALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(NOTIFICATION_STEAM_SUMMARY_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun notifyEpicGiveaways(deals: List<DealItem>) {
        if (!hasNotificationPermission() || deals.isEmpty()) return
        if (deals.size == 1) {
            notifyNewEpicGiveaway(deals.first())
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_EPIC_SUMMARY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎁 ${deals.size} нові безкоштовні гри в Epic Games!"
        val firstDeal = deals.first()
        val summaryText = "${firstDeal.title} та ще ${deals.size - 1} ігор"

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        for (deal in deals.take(5)) {
            val status = if (deal.isUpcoming) "Скоро" else "Зараз"
            inboxStyle.addLine("[$status] ${deal.title}")
        }
        if (deals.size > 5) {
            inboxStyle.setSummaryText("+ ще ${deals.size - 5} роздач")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_EPIC_GIVEAWAYS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(NOTIFICATION_EPIC_SUMMARY_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked
        }
    }

    fun sendTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STEAM_DEALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎮 Deal Watcher: Тест сповіщень")
            .setContentText("Сповіщення працюють відмінно! Ви будете першими дізнаватися про знижки та роздачі.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            notificationManager.notify(TEST_NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendDelayedBackgroundTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            DELAYED_TEST_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STEAM_DEALS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Фонове сповіщення (1 хв)")
            .setContentText("Тест успішний! Фонова служба працює навіть при закритому додатку.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            notificationManager.notify(DELAYED_TEST_NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_STEAM_DEALS = "channel_steam_deals"
        const val CHANNEL_EPIC_GIVEAWAYS = "channel_epic_giveaways"
        private const val TEST_NOTIFICATION_ID = 9999
        private const val DELAYED_TEST_NOTIFICATION_ID = 9998
        private const val NOTIFICATION_STEAM_SUMMARY_ID = 8881
        private const val NOTIFICATION_EPIC_SUMMARY_ID = 8882
    }
}
