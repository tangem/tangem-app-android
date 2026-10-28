package com.tangem.feature.qrscanning.presentation

import com.tangem.feature.qrscanning.presentation.transformers.QrScanningTransformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class QrScanningStateController @Inject constructor() {

    val uiState: StateFlow<QrScanningState>
        field = MutableStateFlow(value = getInitialState())

    val value: QrScanningState get() = uiState.value

    fun update(function: (QrScanningState) -> QrScanningState) {
        uiState.update(function = function)
    }

    fun update(transformer: QrScanningTransformer) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): QrScanningState {
        return QrScanningState(
            topBarConfig = TopBarConfig(title = null, startIcon = 0),
            message = null,
            onBackClick = {},
            onQrScanned = {},
            onGalleryClick = {},
        )
    }
}