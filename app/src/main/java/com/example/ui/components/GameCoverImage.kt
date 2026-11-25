package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.DealItem
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.EpicVioletContainer
import com.example.ui.theme.SteamCyan
import com.example.ui.theme.SteamCyanContainer

fun cleanImageUrl(raw: String): String {
    var url = raw.trim()
    if (url.isEmpty()) return ""
    if (url.startsWith("//")) {
        url = "https:$url"
    } else if (url.startsWith("http://")) {
        url = "https://" + url.removePrefix("http://")
    }
    return url.replace(" ", "%20")
}

fun getCandidateImageUrls(deal: DealItem): List<String> {
    val list = mutableListOf<String>()
    val isSteam = deal.store.equals("steam", ignoreCase = true)

    if (isSteam) {
        val appId = deal.appId?.trim() ?: deal.id.removePrefix("steam_").trim()

        if (deal.imageUrl.isNotBlank()) {
            list.add(cleanImageUrl(deal.imageUrl))
        }

        if (appId.isNotEmpty()) {
            // Steam static CDN variants that cover all store formats
            list.add("https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/header.jpg")
            list.add("https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/header.jpg")
            list.add("https://steamcdn-a.akamaihd.net/steam/apps/$appId/header.jpg")
            list.add("https://cdn.akamai.steamstatic.com/steam/apps/$appId/header.jpg")
            list.add("https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/capsule_616x353.jpg")
            list.add("https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/capsule_616x353.jpg")
            list.add("https://cdn.akamai.steamstatic.com/steam/apps/$appId/capsule_616x353.jpg")
            list.add("https://cdn.cloudflare.steamstatic.com/steam/apps/$appId/capsule_231x87.jpg")
        }
    } else {
        // Epic Games - support pipe-delimited candidates
        if (deal.imageUrl.contains("|")) {
            deal.imageUrl.split("|").forEach { u ->
                val cleaned = cleanImageUrl(u)
                if (cleaned.isNotBlank()) list.add(cleaned)
            }
        } else if (deal.imageUrl.isNotBlank()) {
            list.add(cleanImageUrl(deal.imageUrl))
        }
    }

    return list.filter { it.isNotBlank() }.distinct()
}

@Composable
fun GameCoverImage(
    deal: DealItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val candidates = remember(deal.id, deal.imageUrl, deal.appId) {
        getCandidateImageUrls(deal)
    }

    var candidateIndex by remember(deal.id, deal.imageUrl, deal.appId) { mutableIntStateOf(0) }
    var allCandidatesFailed by remember(deal.id, deal.imageUrl, deal.appId) { mutableStateOf(candidates.isEmpty()) }

    if (allCandidatesFailed || candidates.isEmpty()) {
        FallbackGameCover(
            title = deal.title,
            store = deal.store,
            modifier = modifier
        )
    } else {
        val currentUrl = candidates.getOrElse(candidateIndex) { "" }

        if (currentUrl.isBlank()) {
            FallbackGameCover(
                title = deal.title,
                store = deal.store,
                modifier = modifier
            )
        } else {
            val imageRequest = remember(currentUrl) {
                ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(150)
                    .allowHardware(false)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
            }

            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = deal.title,
                modifier = modifier,
                contentScale = contentScale,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                onError = {
                    if (candidateIndex < candidates.lastIndex) {
                        candidateIndex++
                    } else {
                        allCandidatesFailed = true
                    }
                },
                error = {
                    FallbackGameCover(
                        title = deal.title,
                        store = deal.store,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }
    }
}

@Composable
fun FallbackGameCover(
    title: String,
    store: String,
    modifier: Modifier = Modifier
) {
    val isSteam = store.equals("steam", ignoreCase = true)
    val gradientColors = if (isSteam) {
        listOf(SteamCyanContainer, Color(0xFF091724), Color(0xFF060D15))
    } else {
        listOf(EpicVioletContainer, Color(0xFF1B0B33), Color(0xFF0D0618))
    }

    Box(
        modifier = modifier
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSteam) SteamCyan.copy(alpha = 0.2f) else EpicViolet.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSteam) Icons.Default.SportsEsports else Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = if (isSteam) SteamCyan else EpicViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}
