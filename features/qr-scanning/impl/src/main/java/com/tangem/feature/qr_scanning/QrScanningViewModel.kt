package com.tangem.feature.qr_scanning

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tangem.feature.qr_scanning.QrScanRouter.Companion.SOURCE_KEY
import com.tangem.feature.qr_scanning.usecase.EmitQrScannedEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class QrScanningViewModel @Inject constructor(
    val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val source: SourceType = savedStateHandle[SOURCE_KEY] ?: error("Source is mandatory")

    fun onQrScanned(qrCode: String) {
        emitQrScannedEventUseCase.invoke(source, qrCode)
    }

}

