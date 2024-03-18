package com.tangem.feature.qrscanning.viewmodel

import com.tangem.domain.qrscanning.usecases.EmitQrScannedEventUseCase
import com.tangem.feature.qrscanning.presentation.QrScanningStateController
import com.tangem.feature.qrscanning.presentation.transformers.DismissBottomSheetTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

interface QrScanningClickIntents {

    fun onBackClick()

    fun onQrScanned(qrCode: String)

    fun onGalleryClicked()
}

@ViewModelScoped
internal class QrScanningClickIntentsImplementor @Inject constructor(
    private val stateHolder: QrScanningStateController,
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    private val dispatcher: CoroutineDispatcherProvider,
) : BaseQrScanningClickIntents(), QrScanningClickIntents {

    private var isScanned = false

    override fun onBackClick() = router.popBackStack()

    override fun onQrScanned(qrCode: String) {
        if (qrCode.isNotBlank()) {
            if (!isScanned) {
                router.popBackStack()
                isScanned = true
            }
            viewModelScope.launch(dispatcher.main) {
                emitQrScannedEventUseCase.invoke(source, qrCode)
            }
        }
    }

    override fun onGalleryClicked() {
        galleryLauncher.launch(GALLERY_IMAGE_FILTER)
        if (stateHolder.value.bottomSheetConfig != null) {
            stateHolder.update(DismissBottomSheetTransformer())
        }
    }

    companion object {
        private const val GALLERY_IMAGE_FILTER = "image/*"
    }
}
