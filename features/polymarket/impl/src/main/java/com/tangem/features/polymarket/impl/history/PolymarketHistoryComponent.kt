package com.tangem.features.polymarket.impl.history

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.history.model.PolymarketHistoryModel
import com.tangem.features.polymarket.impl.history.ui.PolymarketHistoryScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

internal class PolymarketHistoryComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PolymarketHistoryModel = getOrCreateModel(params = params)

    private var exportCollectionJob: Job? = null

    init {
        lifecycle.doOnResume {
            model.onResume()
            exportCollectionJob = componentScope.launch {
                model.exports.collect(::shareExport)
            }
        }
        lifecycle.doOnPause {
            exportCollectionJob?.cancel()
            exportCollectionJob = null
            model.onPause()
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        PolymarketHistoryScreen(state = state, modifier = modifier)
    }

    private fun shareExport(file: File) {
        runCatching {
            val context = activity
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }.onFailure { model.onExportShareFailed() }
    }

    data class Params(val userWalletId: UserWalletId)
}