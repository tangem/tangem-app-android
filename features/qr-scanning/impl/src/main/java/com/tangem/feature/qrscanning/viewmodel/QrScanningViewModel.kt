package com.tangem.feature.qrscanning.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.qrscanning.QrScanningRouter.Companion.NETWORK_KEY
import com.tangem.feature.qrscanning.QrScanningRouter.Companion.SOURCE_KEY
import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.presentation.QrScanningStateFactory
import com.tangem.feature.qrscanning.usecase.EmitQrScannedEventUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class QrScanningViewModel @Inject constructor(
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    private val dispatcher: CoroutineDispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), QrScanningClickIntents {

    private val source: SourceType = savedStateHandle[SOURCE_KEY] ?: error("Source is mandatory")
    private val network: String? = savedStateHandle[NETWORK_KEY]

    private val factory = QrScanningStateFactory(
        clickIntents = this,
    )

    var router: QrScanningInnerRouter by Delegates.notNull()

    var uiState: QrScanningState by mutableStateOf(factory.getInitialState(source, network))
        private set

    override fun onBackClick() = router.popBackStack()

    override fun onQrScanned(qrCode: String) {
        if (qrCode.isNotBlank()) {
            router.popBackStack()
            viewModelScope.launch(dispatcher.main) {
                emitQrScannedEventUseCase.invoke(source, qrCode)
            }
        }
    }

    override fun onGalleryClicked() {
        // [REDACTED_JIRA]
    }
}