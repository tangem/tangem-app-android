package com.tangem.feature.qrscanning.viewmodel

import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.qrscanning.usecases.EmitQrScannedEventUseCase
import com.tangem.feature.qrscanning.presentation.QrScanningStateController
import com.tangem.feature.qrscanning.presentation.transformers.DismissBottomSheetTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface QrScanningClickIntents {

    val launchGallery: SharedFlow<Unit>

    fun onBackClick()

    fun onQrScanned(qrCode: String)

    fun onGalleryClicked()

    fun onSettingsClick()
}

@ViewModelScoped
internal class QrScanningClickIntentsImplementor @Inject constructor(
    private val stateHolder: QrScanningStateController,
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    private val settingsManager: SettingsManager,
    private val dispatcher: CoroutineDispatcherProvider,
) : BaseQrScanningClickIntents(), QrScanningClickIntents {

    private var isScanned = false

    override val launchGallery = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    override fun onBackClick() = router.popBackStack()

    override fun onQrScanned(qrCode: String) {
        if (qrCode.isNotBlank()) {
            viewModelScope.launch(dispatcher.mainImmediate) {
                emitQrScannedEventUseCase.invoke(source, qrCode)
            }
            if (!isScanned) {
                router.popBackStack()
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
        settingsManager.openSettings()
    }
}