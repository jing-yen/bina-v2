package com.bina.ai.ui.screens.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.sync.RecipeImporter

@Composable
fun rememberSyncViewModel(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    owner: ViewModelStoreOwner
): SyncViewModel {
    val context = LocalContext.current
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val importer = RecipeImporter(filesDir = context.filesDir, miniAppRepository = miniAppRepository)
            return SyncViewModel(miniAppRepository, installStore, importer) as T
        }
    }
    return viewModel(viewModelStoreOwner = owner, factory = factory)
}
