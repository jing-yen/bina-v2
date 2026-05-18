package com.bina.ai.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.bina.ai.R

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int = 0
) {
    data object Hub : Screen("hub", "Hub", Icons.Outlined.Storefront, R.string.nav_hub)
    data object MyPocket : Screen("pocket", "My Pocket", Icons.Outlined.Inventory2, R.string.nav_pocket)
    data object OfflineSync : Screen("sync", "Offline Sync", Icons.Outlined.CellTower, R.string.nav_sync)
    data object Analytics : Screen("analytics", "Analytics", Icons.Outlined.BarChart, R.string.nav_analytics)

    data object MiniAppView : Screen("miniapp/{miniAppId}", "MiniApp", Icons.Outlined.Storefront) {
        fun createRoute(miniAppId: String) = "miniapp/$miniAppId"
    }

    data object SyncScan : Screen("sync/scan", "Scan", Icons.Outlined.CellTower)

    data object SyncShare : Screen("sync/share/{miniAppId}", "Share", Icons.Outlined.CellTower) {
        fun createRoute(miniAppId: String) = "sync/share/$miniAppId"
    }

    companion object {
        fun tabs() = listOf(Hub, MyPocket, OfflineSync, Analytics)
    }
}
