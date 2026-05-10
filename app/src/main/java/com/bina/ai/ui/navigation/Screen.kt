package com.bina.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Hub : Screen("hub", "Hub", Icons.Outlined.Storefront)
    data object MyPocket : Screen("pocket", "My Pocket", Icons.Outlined.Inventory2)
    data object OfflineSync : Screen("sync", "Offline Sync", Icons.Outlined.CellTower)
    data object Analytics : Screen("analytics", "Analytics", Icons.Outlined.BarChart)

    data object MiniAppView : Screen("miniapp/{miniAppId}", "MiniApp", Icons.Outlined.Storefront) {
        fun createRoute(miniAppId: String) = "miniapp/$miniAppId"
    }

    data object Configurator : Screen("configurator/{miniAppId}", "Configurator", Icons.Outlined.Storefront) {
        fun createRoute(miniAppId: String) = "configurator/$miniAppId"
    }

    companion object {
        fun tabs() = listOf(Hub, MyPocket, OfflineSync, Analytics)
    }
}
