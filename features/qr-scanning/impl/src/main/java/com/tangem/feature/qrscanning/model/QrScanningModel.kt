package com.tangem.feature.qrscanning.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.qrscanning.usecases.EmitQrScannedEventUseCase
import com.tangem.feature.qrscanning.QrScanningComponent
import com.tangem.feature.qrscanning.presentation.QrScanningState
import com.tangem.feature.qrscanning.presentation.QrScanningStateController
import com.tangem.feature.qrscanning.presentation.transformers.DismissBottomSheetTransformer
import com.tangem.feature.qrscanning.presentation.transformers.InitializeQrScanningStateTransformer
import com.tangem.feature.qrscanning.presentation.transformers.ShowCameraDeniedBottomSheetTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ComponentScoped
internal class QrScanningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val stateHolder: QrScanningStateController,
    private val cardSdkProvider: CardSdkProvider,
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    private val settingsManager: SettingsManager,
    private val appRouter: AppRouter,
) : Model(), QrScanningClickIntents {

    private val params = paramsContainer.require<QrScanningComponent.Params>()
    val uiState: StateFlow<QrScanningState> = stateHolder.uiState
    private var isScanned = false

    override val launchGallery = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    init {
        // samsung for some reason disables reader mode, and then it works unstable
        // to prevent this disable ir manually before scan QR
        cardSdkProvider.sdk.forceDisableReaderMode()
        stateHolder.update(InitializeQrScanningStateTransformer(this, params.source, params.networkName))
    }

    fun onCameraDeniedState() {
        stateHolder.update(ShowCameraDeniedBottomSheetTransformer(this))
    }

    fun onDismissBottomSheetState() {
        stateHolder.update(DismissBottomSheetTransformer())
    }

    override fun onBackClick() = appRouter.pop()

    override fun onQrScanned(qrCode: String) {
        if (qrCode.isNotBlank()) {
            modelScope.launch(dispatchers.mainImmediate) {
                emitQrScannedEventUseCase.invoke(params.source, qrCode)
            }
            if (!isScanned) {
                appRouter.pop()
                isScanned = true
            }
        }
    }

    override fun onGalleryClicked() {
        launchGallery.tryEmit(Unit)
        if (stateHolder.value.bottomSheetConfig != null) {
            stateHolder.update(DismissBottomSheetTransformer())
        }
    }

    override fun onSettingsClick() {
        settingsManager.openAppSettings()
    }

    override fun onDestroy() {
        super.onDestroy()
        // don't forget enable reader mode after scan complete
        cardSdkProvider.sdk.forceEnableReaderMode()
    }
}