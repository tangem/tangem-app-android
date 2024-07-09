package com.tangem.feature.qrscanning.viewmodel

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

interface QrScanningClickIntents {

    val launchGallery: SharedFlow<GalleryRequest>

    fun onBackClick()

    fun onQrScanned(qrCode: String)

    fun onGalleryClicked()
}

@JvmInline
value class GalleryRequest(
    val imageFilter: String,
)

@ViewModelScoped
internal class QrScanningClickIntentsImplementor @Inject constructor(
    private val stateHolder: QrScanningStateController,
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    private val dispatcher: CoroutineDispatcherProvider,
) : BaseQrScanningClickIntents(), QrScanningClickIntents {

    private var isScanned = false

    override val launchGallery = MutableSharedFlow<GalleryRequest>(
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
        launchGallery.tryEmit(GalleryRequest(imageFilter = GALLERY_IMAGE_FILTER))
        if (stateHolder.value.bottomSheetConfig != null) {
            stateHolder.update(DismissBottomSheetTransformer())
        }
    }

    companion object {
        private const val GALLERY_IMAGE_FILTER = "image/*"
    }
}