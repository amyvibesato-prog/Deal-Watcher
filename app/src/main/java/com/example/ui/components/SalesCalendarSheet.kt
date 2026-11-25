package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DealItem
import com.example.model.SeasonalEvent
import com.example.model.SeasonalEventsProvider
import com.example.ui.theme.DiscountGreen
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.FreeGold
import com.example.ui.theme.SteamCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesCalendarSheet(
    activeDeals: List<DealItem>,
    onSetReminder: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var setRemindersSet by remember { mutableStateOf(setOf<String>()) }

    val expiringDeals = remember(activeDeals) {
        activeDeals.filter { !it.isUpcoming && (it.store == "epic" || it.discountPercent > 0) }
            .take(6)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D1826),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(SteamCyan, Color(0xFF171A21)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Календар розпродажів",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Сезонні акції Steam, Epic та нагадування",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрити",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Seasonal Events
                item {
                    Text(
                        text = "🔥 Сезонні розпродажі 2026",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SteamCyan,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    items = SeasonalEventsProvider.allEvents,
                    key = { it.id }
                ) { event ->
                    val isReminderSet = setRemindersSet.contains(event.id)
                    SeasonalEventCard(
                        event = event,
                        isReminderSet = isReminderSet,
                        onToggleReminder = {
                            val next = if (isReminderSet) setRemindersSet - event.id else setRemindersSet + event.id
                            setRemindersSet = next
                            val msg = if (!isReminderSet) {
                                "Нагадування для «${event.title}» збережено!"
                            } else {
                                "Нагадування скасовано"
                            }
                            onSetReminder(msg)
                        }
                    )
                }

                // Section 2: Expiring Deals & Freebies
                if (expiringDeals.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⏳ Завершення поточних знижок та роздач",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreeGold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(
                        items = expiringDeals,
                        key = { "expire_${it.id}" }
                    ) { deal ->
                        val reminderKey = "deal_${deal.id}"
                        val isReminderSet = setRemindersSet.contains(reminderKey)

                        ExpiringDealCard(
                            deal = deal,
                            isReminderSet = isReminderSet,
                            onToggleReminder = {
                                val next = if (isReminderSet) setRemindersSet - reminderKey else setRemindersSet + reminderKey
                                setRemindersSet = next
                                val msg = if (!isReminderSet) {
                                    "Нагадування про завершення «${deal.title}» встановлено!"
                                } else {
                                    "Нагадування скасовано"
                                }
                                onSetReminder(msg)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonalEventCard(
    event: SeasonalEvent,
    isReminderSet: Boolean,
    onToggleReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (event.isActive) DiscountGreen else Color(0xFF2A3A4E)
    val cardBg = if (event.isActive) Color(0xFF102638) else Color(0xFF121B27)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column {
            // Event Banner Header Image: Only show banner for active event when available from Steam
            if (event.isActive && (event.imageResId != null || !event.imageUrl.isNullOrBlank())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                ) {
                    AsyncImage(
                        model = event.imageResId ?: event.imageUrl,
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x33000000),
                                        Color(0xEE121B27)
                                    )
                                )
                            )
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Store Tag & Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (event.store == "steam") Color(0xFF171A21) else EpicViolet
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (event.store == "steam") Icons.Default.SportsEsports else Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = if (event.store == "steam") SteamCyan else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (event.store == "steam") "Steam" else "Epic Games",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (event.isActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DiscountGreen.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DiscountGreen)
                            ) {
                                Text(
                                    text = "АКТИВНИЙ ЗАРАЗ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DiscountGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else if (event.daysRemaining != null) {
                            Text(
                                text = "через ${event.daysRemaining} дн",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = event.badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FreeGold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )

                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SteamCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${event.startDateFormatted} – ${event.endDateFormatted}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC0D8E8)
                        )
                    }

                    Button(
                        onClick = onToggleReminder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReminderSet) DiscountGreen else SteamCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isReminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF041829)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReminderSet) "Нагадаємо" else "Нагадати",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF041829)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiringDealCard(
    deal: DealItem,
    isReminderSet: Boolean,
    onToggleReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF233549), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141F2D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (deal.store == "steam") SteamCyan.copy(alpha = 0.2f) else EpicViolet.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (deal.store == "steam") "Steam" else "Epic Games",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (deal.store == "steam") SteamCyan else EpicViolet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (deal.discountPercent > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DiscountGreen
                        ) {
                            Text(
                                text = "-${deal.discountPercent}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF041829),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deal.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (deal.store == "epic") "Завершується роздача у четвер о 18:00" else "Акційна ціна закінчується незабаром",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onToggleReminder) {
                Icon(
                    imageVector = if (isReminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                    tint = if (isReminderSet) DiscountGreen else SteamCyan,
                    contentDescription = "Нагадати"
                )
            }
        }
    }
}
