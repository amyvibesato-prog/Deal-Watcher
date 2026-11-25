package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.FilterOption
import com.example.model.SteamSearchSortOption
import com.example.ui.theme.DiscountGreen
import com.example.ui.theme.EpicViolet
import com.example.ui.theme.FreeGold
import com.example.ui.theme.SteamCyan

@Composable
fun FilterTabsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedFilter: FilterOption,
    onSelectFilter: (FilterOption) -> Unit,
    // Steam price search parameters
    initialMinPrice: String,
    initialMaxPrice: String,
    initialExactPrice: String,
    initialIsExact: Boolean,
    initialSort: SteamSearchSortOption,
    onApplySteamPriceFilter: (min: String, max: String, exact: String, isExact: Boolean, sort: SteamSearchSortOption) -> Unit,
    onResetSteamPriceFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedSectionTab by remember(selectedFilter) {
        mutableIntStateOf(if (selectedFilter == FilterOption.STEAM_PRICE_SEARCH) 1 else 0)
    }

    // Local form state for Steam price filter
    var minPrice by remember(initialMinPrice) { mutableStateOf(initialMinPrice) }
    var maxPrice by remember(initialMaxPrice) { mutableStateOf(initialMaxPrice) }
    var exactPrice by remember(initialExactPrice) { mutableStateOf(initialExactPrice) }
    var isExactMode by remember(initialIsExact) { mutableStateOf(initialIsExact) }
    var chosenSort by remember(initialSort) { mutableStateOf(initialSort) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("filter_tabs_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131924),
            border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.25f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SteamCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = SteamCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Вкладки та фільтри",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Категорії та фільтр цін Steam",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_filter_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section Tabs: Categories vs Steam Price Filter
                TabRow(
                    selectedTabIndex = selectedSectionTab,
                    containerColor = Color(0xFF0C1017),
                    contentColor = SteamCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSectionTab]),
                            color = SteamCyan,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        text = {
                            Text(
                                text = "Категорії",
                                fontWeight = if (selectedSectionTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Фільтр цін Steam",
                                    fontWeight = if (selectedSectionTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(DiscountGreen)
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 0: STANDARD CATEGORIES
                if (selectedSectionTab == 0) {
                    Text(
                        text = "Оберіть вкладку для відображення:",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val categories = listOf(
                        FilterOption.ALL to (Icons.Default.SportsEsports to MaterialTheme.colorScheme.primary),
                        FilterOption.SEASONAL to (Icons.Default.LocalFireDepartment to Color(0xFFFF5252)),
                        FilterOption.CRAZY_DISCOUNTS to (Icons.Default.LocalFireDepartment to Color(0xFFFF7043)),
                        FilterOption.STEAM_FREE to (Icons.Default.CardGiftcard to SteamCyan),
                        FilterOption.EPIC to (Icons.Default.CardGiftcard to EpicViolet),
                        FilterOption.UPCOMING to (Icons.Default.Schedule to FreeGold),
                        FilterOption.STEAM to (Icons.Default.SportsEsports to SteamCyan)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { (option, iconPair) ->
                            val isSelected = selectedFilter == option
                            val (icon, color) = iconPair

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color.copy(alpha = 0.18f) else Color(0xFF1B2332),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) color else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectFilter(option)
                                        onDismiss()
                                    }
                                    .testTag("filter_tab_${option.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = option.label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFE0E0E0)
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Вибрано",
                                            tint = color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 1: STEAM PRICE FILTER (STOREWIDE)
                if (selectedSectionTab == 1) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F151F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SteamCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Пошук серед усіх ігор Steam",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SteamCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Запит виконується по всій базі Steam Store в українському регіоні (₴)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mode toggle: Range vs Exact Price
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF182230))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isExactMode) SteamCyan else Color.Transparent)
                                        .clickable { isExactMode = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Діапазон цін",
                                        fontSize = 12.sp,
                                        fontWeight = if (!isExactMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isExactMode) Color(0xFF041829) else Color.LightGray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isExactMode) SteamCyan else Color.Transparent)
                                        .clickable { isExactMode = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Конкретна ціна",
                                        fontSize = 12.sp,
                                        fontWeight = if (isExactMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isExactMode) Color(0xFF041829) else Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Price Input Fields
                            if (!isExactMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                     OutlinedTextField(
                                        value = minPrice,
                                        onValueChange = { minPrice = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                        label = { Text("Ціна від (грн)", fontSize = 11.sp) },
                                        placeholder = { Text("0", fontSize = 12.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SteamCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedLabelColor = SteamCyan
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("price_min_input")
                                    )

                                     OutlinedTextField(
                                        value = maxPrice,
                                        onValueChange = { maxPrice = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                        label = { Text("Ціна до (грн)", fontSize = 11.sp) },
                                        placeholder = { Text("500", fontSize = 12.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SteamCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedLabelColor = SteamCyan
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("price_max_input")
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = exactPrice,
                                    onValueChange = { exactPrice = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                    label = { Text("Конкретна ціна у гривнях (грн)", fontSize = 11.sp) },
                                    placeholder = { Text("Наприклад: 199 або 239.60", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SteamCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedLabelColor = SteamCyan
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("price_exact_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick preset chips
                            Text(
                                text = "Швидкі пресети:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    "Безкоштовно" to ("0" to "0"),
                                    "До 100 ₴" to ("" to "100"),
                                    "До 250 ₴" to ("" to "250"),
                                    "До 500 ₴" to ("" to "500"),
                                    "До 1000 ₴" to ("" to "1000"),
                                    "100–300 ₴" to ("100" to "300")
                                )

                                presets.forEach { (label, range) ->
                                    FilterChip(
                                        selected = !isExactMode && minPrice == range.first && maxPrice == range.second,
                                        onClick = {
                                            isExactMode = false
                                            minPrice = range.first
                                            maxPrice = range.second
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SteamCyan,
                                            selectedLabelColor = Color(0xFF041829)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Full Sorting Selection
                            Text(
                                text = "Сортування знайдених ігор Steam:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Sorting selector button that opens dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1B2332),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SteamCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { sortDropdownExpanded = true }
                                        .testTag("steam_sort_selector")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = chosenSort.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = SteamCyan
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = sortDropdownExpanded,
                                    onDismissRequest = { sortDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF131924))
                                        .border(1.dp, SteamCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    SteamSearchSortOption.values().forEach { sortOpt ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = sortOpt.label,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (chosenSort == sortOpt) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (chosenSort == sortOpt) SteamCyan else Color.White
                                                    )
                                                    if (chosenSort == sortOpt) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = SteamCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                chosenSort = sortOpt
                                                sortDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                minPrice = ""
                                maxPrice = ""
                                exactPrice = ""
                                isExactMode = false
                                chosenSort = SteamSearchSortOption.POPULARITY
                                onResetSteamPriceFilter()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("reset_price_filter_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Скинути", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onApplySteamPriceFilter(minPrice, maxPrice, exactPrice, isExactMode, chosenSort)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("apply_price_filter_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SteamCyan,
                                contentColor = Color(0xFF041829)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Шукати в Steam", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
