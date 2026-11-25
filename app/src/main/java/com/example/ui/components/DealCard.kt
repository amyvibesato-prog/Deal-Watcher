package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.model.DealItem
import com.example.ui.theme.DiscountGreen
import com.example.ui.theme.DiscountGreenContainer
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.EpicVioletContainer
import com.example.ui.theme.FreeGold
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamCyanContainer
import androidx.compose.ui.res.painterResource
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DealCard(
    deal: DealItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isImageVisible: Boolean = true
) {
    val context = LocalContext.current
    val isSteam = deal.store == "steam"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = if (deal.isFree && !deal.isUpcoming) DiscountGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("deal_card_${deal.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Header with overlay badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(460f / 215f)
                    .background(Color(0xFF0F141C))
            ) {
                if (isImageVisible) {
                    GameCoverImage(
                        deal = deal,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FallbackGameCover(
                        title = deal.title,
                        store = deal.store,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark gradient overlay for bottom text contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000)),
                                startY = 60f
                            )
                        )
                )

                // Top-left Store / Status badge & Historical Low badge underneath
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        color = when {
                            deal.isUpcoming -> Color(0xFF382305)
                            isSteam && deal.isFree -> Color(0xFF003847)
                            isSteam -> SteamCyanContainer.copy(alpha = 0.9f)
                            else -> EpicVioletContainer.copy(alpha = 0.9f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    deal.isUpcoming -> Icons.Default.CalendarMonth
                                    isSteam && deal.isFree -> Icons.Default.CardGiftcard
                                    isSteam -> Icons.Default.SportsEsports
                                    else -> Icons.Default.CardGiftcard
                                },
                                contentDescription = null,
                                tint = when {
                                    deal.isUpcoming -> FreeGold
                                    isSteam -> SteamCyan
                                    else -> EpicViolet
                                },
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    deal.isUpcoming -> "Скоро в Epic"
                                    isSteam && deal.isFree -> "Steam Free"
                                    isSteam -> "Steam"
                                    else -> "Epic Games"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    deal.isUpcoming -> FreeGold
                                    isSteam -> SteamCyan
                                    else -> EpicViolet
                                },
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Historical Low badge under store badge in same style & Free green color (icon only)
                    if (deal.isHistoricalLow && !deal.isUpcoming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 10.dp, topEnd = 10.dp),
                            color = DiscountGreenContainer.copy(alpha = 0.9f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Історичний мінімум",
                                    tint = DiscountGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // Top-right Discount Tag Pill (no upcoming badge to avoid duplicate label in upcoming tab)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    if (!deal.isUpcoming) {
                        if (deal.isFree) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DiscountGreen
                            ) {
                                Text(
                                    text = "FREE 100%",
                                    color = Color(0xFF04200E),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        } else if (deal.discountPercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DiscountGreen
                            ) {
                                Text(
                                    text = "-${deal.discountPercent}%",
                                    color = Color(0xFF04200E),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        } else if (deal.isWishlist) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SteamCyanContainer.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "ВІШЛІСТ",
                                    color = SteamCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }


            }

            // Card Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Game Title - fixed 2 lines height for uniform sizing across all cards
                Text(
                    text = deal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price Row - uniform fixed height for both free and paid deals
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (deal.isFree) (if (deal.isUpcoming) "Скоро безкоштовно" else "БЕЗКОШТОВНО") else deal.formattedFinalPrice,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (deal.isFree) (if (deal.isUpcoming) FreeGold else DiscountGreen) else MaterialTheme.colorScheme.onSurface,
                            fontSize = if (deal.isFree) 13.sp else 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val strikethroughPrice = when {
                            deal.formattedInitialPrice.isNotBlank() && deal.formattedInitialPrice != deal.formattedFinalPrice -> deal.formattedInitialPrice
                            deal.isFree && deal.initialPrice > 0 -> "${deal.initialPrice / 100} грн"
                            else -> null
                        }

                        if (strikethroughPrice != null) {
                            Text(
                                text = strikethroughPrice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = " ",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Share quick button
                    IconButton(
                        onClick = { shareDeal(context, deal) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поділитися",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer row - uniform 20dp height across all platforms
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                ) {
                    if (deal.store == "epic") {
                        val dateText = formatEpicDates(deal)
                        if (dateText.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (deal.isUpcoming) FreeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (deal.isUpcoming) FreeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = EpicViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Epic Games",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (deal.isFree) Icons.Default.CardGiftcard else Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = SteamCyan.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                deal.isFree -> "Безкоштовно в Steam"
                                deal.isWishlist -> "Steam Wishlist"
                                else -> "Steam"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatEpicDates(deal: DealItem): String {
    val formatter = SimpleDateFormat("dd.MM HH:mm", Locale("uk"))
    return when {
        deal.isUpcoming && deal.startDate != null -> "Роздача з ${formatter.format(Date(deal.startDate))}"
        !deal.isUpcoming && deal.endDate != null -> "До ${formatter.format(Date(deal.endDate))}"
        else -> ""
    }
}

fun openDealUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

fun shareDeal(context: Context, deal: DealItem) {
    val text = buildString {
        if (deal.isFree) {
            append("🎁 Гра «${deal.title}» зараз роздається БЕЗКОШТОВНО в Epic Games!\n")
        } else {
            append("🔥 Знижка -${deal.discountPercent}% на «${deal.title}» (${deal.formattedFinalPrice}) у Steam!\n")
        }
        append("Посилання: ${deal.storeUrl}")
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Поділитися знижкою")
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(shareIntent)
}
