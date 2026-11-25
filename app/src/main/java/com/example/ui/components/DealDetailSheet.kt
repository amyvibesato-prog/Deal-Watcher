package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealDetailSheet(
    deal: DealItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isSteam = deal.store == "steam"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(460f / 215f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F141C))
            ) {
                GameCoverImage(
                    deal = deal,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Store badge
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                    color = if (isSteam) SteamCyanContainer else EpicVioletContainer,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isSteam) Icons.Default.SportsEsports else Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = if (isSteam) SteamCyan else EpicViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSteam) "Steam Store" else "Epic Games Store",
                            fontWeight = FontWeight.Bold,
                            color = if (isSteam) SteamCyan else EpicViolet,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Title
            Text(
                text = deal.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Deal Card Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ціна зі знижкою",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (deal.isFree) (if (deal.isUpcoming) "Скоро безкоштовно" else "БЕЗКОШТОВНО") else deal.formattedFinalPrice,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (deal.isFree) (if (deal.isUpcoming) FreeGold else DiscountGreen) else MaterialTheme.colorScheme.onSurface
                                )
                                if (deal.formattedInitialPrice.isNotBlank() && deal.formattedInitialPrice != deal.formattedFinalPrice) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = deal.formattedInitialPrice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                }
                            }
                        }

                        // Discount badge (hidden for upcoming deals to remove the yellow "Скоро" frame)
                        if (deal.discountPercent > 0 && !deal.isUpcoming) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DiscountGreen
                            ) {
                                Text(
                                    text = if (deal.isFree) "-100%" else "-${deal.discountPercent}%",
                                    color = Color(0xFF04200E),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Calculation breakdown if available
                    if (deal.initialPrice > deal.finalPrice && deal.finalPrice > 0) {
                        val savingsCents = deal.initialPrice - deal.finalPrice
                        val savingsUah = savingsCents / 100.0
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = DiscountGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ваша економія: %.2f грн".format(savingsUah),
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscountGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Historical Low indicator
                    if (deal.isHistoricalLow && !deal.isUpcoming) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = DiscountGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Найбільша знижка за весь час (Історичний мінімум)!",
                                style = MaterialTheme.typography.bodySmall,
                                color = DiscountGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Epic date range
                    if (deal.store == "epic") {
                        val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("uk"))
                        if (deal.endDate != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Діє до: ${formatter.format(Date(deal.endDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (deal.startDate != null && deal.isUpcoming) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = FreeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Початок роздачі: ${formatter.format(Date(deal.startDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FreeGold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { openDealUrl(context, deal.storeUrl) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSteam) SteamCyan else EpicViolet,
                        contentColor = Color(0xFF04121F)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (deal.isFree) "Отримати гру" else "В магазин",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { shareDeal(context, deal) },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Поділитись")
                }
            }
        }
    }
}
