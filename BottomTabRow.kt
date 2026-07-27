package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.PosTab

@Composable
fun BottomTabRow(
    activeTab: PosTab,
    onTabSelected: (PosTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_tab_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        PosTab.values().forEach { tab ->
            val icon: ImageVector = when (tab) {
                PosTab.PDV -> Icons.Default.PointOfSale
                PosTab.TABLES -> Icons.Default.TableRestaurant
                PosTab.PRODUCTS -> Icons.Default.RestaurantMenu
                PosTab.TERMINALS -> Icons.Default.CreditCard
                PosTab.CASHIER -> Icons.Default.AccountBalanceWallet
                PosTab.AI_ASSISTANT -> Icons.Default.AutoAwesome
            }

            NavigationBarItem(
                selected = (activeTab == tab),
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = tab.title) },
                label = { Text(text = tab.title, maxLines = 1) },
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}
