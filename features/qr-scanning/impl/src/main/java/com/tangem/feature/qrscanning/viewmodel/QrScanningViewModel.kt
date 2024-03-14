package com.tangem.feature.qrscanning.viewmodel

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.qrscanning.QrScanningRouter.Companion.NETWORK_KEY
import com.tangem.feature.qrscanning.QrScanningRouter.Companion.SOURCE_KEY
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.presentation.QrScanningStateController
import com.tangem.feature.qrscanning.presentation.transformers.ShowCameraDeniedBottomSheetTransformer
import com.tangem.feature.qrscanning.presentation.transformers.InitializeQrScanningStateTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class QrScanningViewModel @Inject constructor(
    private val stateHolder: QrScanningStateController,
    private val clickIntents: QrScanningClickIntentsImplementor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val source: SourceType = savedStateHandle[SOURCE_KEY] ?: error("Source is mandatory")
    private val network: String? = savedStateHandle[NETWORK_KEY]

    val uiState: StateFlow<QrScanningState> = stateHolder.uiState

    fun setRouter(router: QrScanningInnerRouter, galleryLauncher: ActivityResultLauncher<String>) {
        clickIntents.initialize(
            router = router,
            source = source,
            galleryLauncher = galleryLauncher,
            coroutineScope = viewModelScope,
        )
        stateHolder.update(InitializeQrScanningStateTransformer(clickIntents, source, network))
    }

    fun onQrScanned(qrCode: String) = clickIntents.onQrScanned(qrCode)

    fun onCameraDeniedState() {
        stateHolder.update(ShowCameraDeniedBottomSheetTransformer(clickIntents))
    }
}
