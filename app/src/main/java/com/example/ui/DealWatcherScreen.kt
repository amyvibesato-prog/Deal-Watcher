package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.CalendarMonth
import com.example.model.FilterOption
import com.example.model.SeasonalEventsProvider
import com.example.model.SortOption
import androidx.compose.material.icons.filled.FilterList
import com.example.ui.components.CurrentSeasonCard
import com.example.ui.components.DealCard
import com.example.ui.components.DealDetailSheet
import com.example.ui.components.FilterTabsDialog
import com.example.ui.components.SalesCalendarSheet
import com.example.ui.components.SearchDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.DiscountGreen
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.FreeGold
import com.example.ui.theme.SteamCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealWatcherScreen(
    viewModel: DealViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 200
        }
    }

    // Memory optimization: at most 300 active games around the viewport have visible images
    val activeWindowStart by remember {
        derivedStateOf {
            val approxDealIndex = (gridState.firstVisibleItemIndex - 3).coerceAtLeast(0)
            (approxDealIndex - 50).coerceAtLeast(0)
        }
    }
    val activeWindowEnd by remember {
        derivedStateOf { activeWindowStart + 250 }
    }

    // Android 13+ Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.updateNotificationPermissionStatus()
    }

    // Show messages in snackbar
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Spin animation for refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Знайдено: ${uiState.displayedDeals.size}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // Filter & Tabs popup button
                    IconButton(
                        onClick = { viewModel.openFilterMenu(true) },
                        modifier = Modifier.testTag("filter_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Вкладки та фільтри",
                            tint = if (uiState.selectedFilter != FilterOption.ALL) SteamCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Search button in header - opens popup search dialog
                    IconButton(
                        onClick = { viewModel.toggleSearchExpanded(true) },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Пошук",
                            tint = if (uiState.searchQuery.isNotEmpty()) SteamCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sales Calendar button
                    IconButton(
                        onClick = { viewModel.openSalesCalendar(true) },
                        modifier = Modifier.testTag("calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Календар розпродажів",
                            tint = SteamCyan
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh(force = true) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Оновити",
                            tint = SteamCyan,
                            modifier = Modifier.rotate(if (uiState.isLoading) angle else 0f)
                        )
                    }

                    // Settings button
                    IconButton(
                        onClick = { viewModel.openSettings(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Налаштування",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    },
                    containerColor = SteamCyan,
                    contentColor = Color(0xFF041829),
                    modifier = Modifier.testTag("scroll_to_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Прогорнути вгору",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Linear loading indicator during fast sync
            AnimatedVisibility(visible = uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = SteamCyan,
                    trackColor = Color(0xFF131924)
                )
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Stats and Overview Header (Span all columns)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StatsHeader(
                        steamCount = uiState.steamDealsCount,
                        wishlistCount = uiState.steamWishlistCount,
                        isSteamConfigured = uiState.isSteamConfigured,
                        epicCount = uiState.epicDealsCount,
                        maxDiscount = uiState.maxDiscountPercent,
                        lastUpdated = uiState.lastUpdatedTime
                    )
                }

                // 2. Notification Permission Banner if not granted (Span all columns)
                if (!uiState.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NotificationPermissionBanner(
                            onRequestPermission = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        )
                    }
                }

                // 3. Current Seasonal Sale Banner in place of old search bar (Span all columns)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CurrentSeasonCard(
                        event = uiState.currentSeasonalEvent,
                        onCardClick = { viewModel.setFilter(FilterOption.SEASONAL) },
                        onOpenCalendar = { viewModel.openSalesCalendar(true) },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("current_season_card")
                    )
                }


                // 4. Filters and Sorting row (Span all columns)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Prominent Popup Window Button with all available tabs and Steam price filter
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF131924),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) SteamCyan else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.openFilterMenu(true) }
                                .testTag("open_filter_menu_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val currentIcon = when (uiState.selectedFilter) {
                                        FilterOption.STEAM_PRICE_SEARCH -> Icons.Default.Search
                                        FilterOption.SEASONAL -> Icons.Default.LocalFireDepartment
                                        FilterOption.CRAZY_DISCOUNTS -> Icons.Default.LocalFireDepartment
                                        FilterOption.STEAM_FREE -> Icons.Default.CardGiftcard
                                        FilterOption.UPCOMING -> Icons.Default.Schedule
                                        FilterOption.EPIC -> Icons.Default.CardGiftcard
                                        FilterOption.STEAM -> Icons.Default.SportsEsports
                                        FilterOption.ALL -> Icons.Default.FilterList
                                    }
                                    val iconTint = when (uiState.selectedFilter) {
                                        FilterOption.STEAM_PRICE_SEARCH -> SteamCyan
                                        FilterOption.SEASONAL -> Color(0xFFFF5252)
                                        FilterOption.CRAZY_DISCOUNTS -> Color(0xFFFF7043)
                                        FilterOption.STEAM_FREE -> SteamCyan
                                        FilterOption.UPCOMING -> FreeGold
                                        FilterOption.EPIC -> EpicViolet
                                        FilterOption.STEAM -> SteamCyan
                                        FilterOption.ALL -> SteamCyan
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(iconTint.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = currentIcon,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) "Фільтр цін Steam" else uiState.selectedFilter.label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Змінити ▾",
                                                fontSize = 11.sp,
                                                color = SteamCyan,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) {
                                                uiState.activePriceFilterSummary ?: "Пошук серед усіх ігор Steam"
                                            } else {
                                                "Натисніть для відкриття вікна з усіма вкладками"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (uiState.selectedFilter != FilterOption.ALL) {
                                    IconButton(
                                        onClick = {
                                            if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) {
                                                viewModel.resetSteamPriceFilter()
                                            } else {
                                                viewModel.setFilter(FilterOption.ALL)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Скинути фільтр",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }


                        // Active search query chip if searching
                        if (uiState.searchQuery.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SteamCyan.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.toggleSearchExpanded(true) }
                                    .testTag("active_search_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = SteamCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Пошук: «${uiState.searchQuery}» (${uiState.displayedDeals.size})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SteamCyan,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Очистити пошук",
                                            tint = SteamCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Sorting row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) "Знайдено в Steam: ${uiState.displayedDeals.size}" else "Знайдено: ${uiState.displayedDeals.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )

                            if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1B2332),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clickable { viewModel.openFilterMenu(true) }
                                        .testTag("steam_price_sort_pill")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = uiState.selectedSteamSort.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SteamCyan,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = SteamCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.clickable { sortMenuExpanded = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = uiState.selectedSort.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        SortOption.values().forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option.label,
                                                        fontWeight = if (uiState.selectedSort == option) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (uiState.selectedSort == option) SteamCyan else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setSort(option)
                                                    sortMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Deal Cards or Empty State
                if (uiState.displayedDeals.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyStateCard(
                            isSearching = uiState.searchQuery.isNotEmpty(),
                            selectedFilter = uiState.selectedFilter,
                            isSteamConfigured = uiState.isSteamConfigured,
                            onOpenSettings = { viewModel.openSettings(true) },
                            onRefresh = { viewModel.refresh(force = true) }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.displayedDeals,
                        key = { _, deal -> deal.id }
                    ) { index, deal ->
                        val isImageActive = if (uiState.displayedDeals.size <= 250) {
                            true
                        } else {
                            index in activeWindowStart until activeWindowEnd
                        }
                        DealCard(
                            deal = deal,
                            onClick = { viewModel.selectDeal(deal) },
                            isImageVisible = isImageActive
                        )
                    }

                    // 6. "Завантажити ще" button for Steam price search pagination
                    if (uiState.selectedFilter == FilterOption.STEAM_PRICE_SEARCH) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isSteamLoadingMore) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = SteamCyan,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Завантаження ще ігор зі Steam...",
                                            color = SteamCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else if (uiState.canLoadMoreSteamGames) {
                                    Button(
                                        onClick = { viewModel.loadMoreSteamPriceGames() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SteamCyan,
                                            contentColor = Color(0xFF041829)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(48.dp)
                                            .testTag("load_more_steam_deals_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Завантажити ще",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF131924),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = "✅ Усі доступні ігри за запитом завантажено (${uiState.displayedDeals.size})",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    uiState.selectedDealForDetail?.let { deal ->
        DealDetailSheet(
            deal = deal,
            onDismiss = { viewModel.selectDeal(null) }
        )
    }

    // Settings Dialog
    if (uiState.isSettingsOpen) {
        val prefs = viewModel.preferencesManager
        SettingsDialog(
            initialSteamId = prefs.steamId,
            initialApiKey = prefs.steamApiKey,
            initialNotifySteam = prefs.notifySteam,
            initialNotifyEpic = prefs.notifyEpic,
            initialMinDiscount = prefs.minDiscountPercent,
            isTestingConnection = uiState.isTestingConnection,
            connectionStatus = uiState.connectionTestStatus,
            onTestConnection = { sId, aKey -> viewModel.testSteamConnection(sId, aKey) },
            onSave = { sid, key, nSteam, nEpic, minDisc ->
                viewModel.saveSettings(sid, key, nSteam, nEpic, minDisc)
            },
            onDismiss = { viewModel.openSettings(false) },
            onTestNotification = { viewModel.sendTestNotification() },
            onDelayedNotificationTest = { viewModel.scheduleDelayedNotificationTest() },
            onClearCache = { viewModel.clearAllData() }
        )
    }

    // Sales Calendar Sheet
    if (uiState.isSalesCalendarOpen) {
        SalesCalendarSheet(
            activeDeals = uiState.rawDealsList,
            onSetReminder = { msg -> viewModel.showUserMessage(msg) },
            onDismiss = { viewModel.openSalesCalendar(false) }
        )
    }

    // Search Popup Dialog
    if (uiState.isSearchExpanded) {
        SearchDialog(
            searchQuery = uiState.searchQuery,
            dealsCount = uiState.displayedDeals.size,
            matchingDeals = uiState.displayedDeals,
            onQueryChange = { viewModel.setSearchQuery(it) },
            onSelectDeal = { deal ->
                viewModel.selectDeal(deal)
                viewModel.toggleSearchExpanded(false)
            },
            onDismiss = { viewModel.toggleSearchExpanded(false) },
            onClear = { viewModel.setSearchQuery("") }
        )
    }

    // Filter & Tabs Dialog (Popup modal with all tabs and Steam price filter)
    if (uiState.isFilterMenuOpen) {
        FilterTabsDialog(
            isOpen = uiState.isFilterMenuOpen,
            onDismiss = { viewModel.openFilterMenu(false) },
            selectedFilter = uiState.selectedFilter,
            onSelectFilter = { viewModel.setFilter(it) },
            initialMinPrice = uiState.steamMinPriceInput,
            initialMaxPrice = uiState.steamMaxPriceInput,
            initialExactPrice = uiState.steamExactPriceInput,
            initialIsExact = uiState.isExactPriceMode,
            initialSort = uiState.selectedSteamSort,
            onApplySteamPriceFilter = { min, max, exact, isExact, sort ->
                viewModel.applySteamPriceFilter(min, max, exact, isExact, sort)
            },
            onResetSteamPriceFilter = {
                viewModel.resetSteamPriceFilter()
            }
        )
    }
}

@Composable
fun StatsHeader(
    steamCount: Int,
    wishlistCount: Int,
    isSteamConfigured: Boolean,
    epicCount: Int,
    maxDiscount: Int,
    lastUpdated: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steam Count Pill
                StatPill(
                    icon = Icons.Default.SportsEsports,
                    iconTint = SteamCyan,
                    label = if (isSteamConfigured && wishlistCount > 0) "У вішлісті" else "Steam знижки",
                    value = if (isSteamConfigured && wishlistCount > 0) "$wishlistCount" else "$steamCount",
                    valueColor = SteamCyan
                )

                // Epic Count Pill
                StatPill(
                    icon = Icons.Default.CardGiftcard,
                    iconTint = EpicViolet,
                    label = "Epic роздачі",
                    value = "$epicCount",
                    valueColor = EpicViolet
                )

                // Max Discount Pill
                StatPill(
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = DiscountGreen,
                    label = "Макс. знижка",
                    value = if (maxDiscount > 0) "-$maxDiscount%" else "0%",
                    valueColor = DiscountGreen
                )
            }

            if (lastUpdated > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateFormatted = SimpleDateFormat("dd.MM HH:mm", Locale("uk")).format(Date(lastUpdated))
                    Text(
                        text = "Оновлено: $dateFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SteamCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1E2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SteamCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Увімкніть сповіщення",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Дізнавайтесь про знижки та роздачі миттєво",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = SteamCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Дозволити",
                    color = Color(0xFF041829),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    isSearching: Boolean,
    selectedFilter: FilterOption = FilterOption.ALL,
    isSteamConfigured: Boolean = false,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isSearching -> Icons.Default.Search
                    selectedFilter == FilterOption.STEAM_PRICE_SEARCH -> Icons.Default.Search
                    selectedFilter == FilterOption.UPCOMING -> Icons.Default.Schedule
                    selectedFilter == FilterOption.STEAM_FREE -> Icons.Default.CardGiftcard
                    selectedFilter == FilterOption.CRAZY_DISCOUNTS -> Icons.Default.LocalFireDepartment
                    else -> Icons.Default.SportsEsports
                },
                contentDescription = null,
                tint = when {
                    selectedFilter == FilterOption.STEAM_PRICE_SEARCH -> SteamCyan
                    selectedFilter == FilterOption.UPCOMING -> FreeGold
                    selectedFilter == FilterOption.STEAM_FREE -> SteamCyan
                    selectedFilter == FilterOption.CRAZY_DISCOUNTS -> Color(0xFFFF5252)
                    selectedFilter == FilterOption.STEAM -> SteamCyan
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val title = when {
                isSearching -> "Ігор не знайдено"
                selectedFilter == FilterOption.STEAM_PRICE_SEARCH -> "За вказаним фільтром цін у Steam ігор не знайдено"
                selectedFilter == FilterOption.UPCOMING -> "Немає анонсованих майбутніх роздач"
                selectedFilter == FilterOption.STEAM_FREE -> "Немає активних знижок -100% у Steam"
                selectedFilter == FilterOption.CRAZY_DISCOUNTS -> "Немає ігор зі знижкою від 75%"
                selectedFilter == FilterOption.STEAM && !isSteamConfigured -> "Steam Wishlist не під'єднано"
                selectedFilter == FilterOption.STEAM && isSteamConfigured -> "У списку бажаного ігор не знайдено"
                else -> "Немає активних знижок"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            val subtitle = when {
                isSearching -> "Спробуйте змінити пошуковий запит або скинути фільтри."
                selectedFilter == FilterOption.STEAM_PRICE_SEARCH -> "Спробуйте розширити діапазон цін або обрати інше сортування у спливаючому меню."
                selectedFilter == FilterOption.UPCOMING -> "Майбутні роздачі зазвичай розкриваються щочетверга ввечері. Як тільки з'явиться новий анонс, він одразу з'явиться тут."
                selectedFilter == FilterOption.STEAM_FREE -> "Тимчасові роздачі платних ігор зі 100% знижкою в Steam з'являються періодично. Як тільки з'явиться нова акція, вона з'явиться тут і надійде сповіщення!"
                selectedFilter == FilterOption.CRAZY_DISCOUNTS -> "Зараз немає пропозицій зі знижкою 75%+. Спробуйте оновити пізніше або перевірити вкладку «Всі»."
                selectedFilter == FilterOption.STEAM && !isSteamConfigured -> "Вкажіть ваш Steam ID (або код друга) та Steam Web API Key у налаштуваннях, щоб підключити свій список бажаного."
                selectedFilter == FilterOption.STEAM && isSteamConfigured -> "Перевірте налаштування: увійдіть у «Налаштування» та натисніть «Перевірити зв'язок». Переконайтесь, що «Деталі про ігри» у приватності Steam встановлено як «Загальнодоступні»."
                else -> "Перевірте ваш Steam ID у налаштуваннях або натисніть оновити."
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = SteamCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("↻ Оновити", color = Color(0xFF041829), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (selectedFilter == FilterOption.STEAM && isSteamConfigured) "⚙ Діагностика" else "⚙ Налаштування",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
