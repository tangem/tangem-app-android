package com.tangem.feature.qrscanning.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.AppRoute
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.presentation.QrScanningStateController
import com.tangem.feature.qrscanning.presentation.transformers.DismissBottomSheetTransformer
import com.tangem.feature.qrscanning.presentation.transformers.InitializeQrScanningStateTransformer
import com.tangem.feature.qrscanning.presentation.transformers.ShowCameraDeniedBottomSheetTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class QrScanningViewModel @Inject constructor(
    private val stateHolder: QrScanningStateController,
    private val clickIntents: QrScanningClickIntentsImplementor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val source: SourceType = savedStateHandle.get<Int>(AppRoute.QrScanning.SOURCE_KEY)
        ?.let { SourceType.entries[it] }
        ?: error("Source is mandatory")
    private val network: String? = savedStateHandle[AppRoute.QrScanning.NETWORK_KEY]

    val uiState: StateFlow<QrScanningState> = stateHolder.uiState
    val launchGalleryEvent: SharedFlow<GalleryRequest> = clickIntents.launchGallery

    fun setRouter(router: QrScanningInnerRouter) {
        clickIntents.initialize(
            router = router,
            source = source,
            coroutineScope = viewModelScope,
        )
        stateHolder.update(InitializeQrScanningStateTransformer(clickIntents, source, network))
    }

    fun onQrScanned(qrCode: String) = clickIntents.onQrScanned(qrCode)

    fun onCameraDeniedState() {
        stateHolder.update(ShowCameraDeniedBottomSheetTransformer(clickIntents))
    }

    fun onDismissBottomSheetState() {
        stateHolder.update(DismissBottomSheetTransformer())
    }
}