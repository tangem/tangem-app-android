package com.tangem.feature.qrscanning.presentation

import com.tangem.feature.qrscanning.presentation.transformers.QrScanningTransformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class QrScanningStateController @Inject constructor() {

    val uiState: StateFlow<QrScanningState> get() = mutableUiState

    val value: QrScanningState get() = uiState.value

    private val mutableUiState: MutableStateFlow<QrScanningState> = MutableStateFlow(value = getInitialState())

    fun update(function: (QrScanningState) -> QrScanningState) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: QrScanningTransformer) {
        mutableUiState.update(function = transformer::transform)
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