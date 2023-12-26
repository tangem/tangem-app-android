package com.tangem.feature.qrscanning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.qrscanning.QrScanningRouter.Companion.SOURCE_KEY
import com.tangem.feature.qrscanning.usecase.EmitQrScannedEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class QrScanningViewModel @Inject constructor(
    private val emitQrScannedEventUseCase: EmitQrScannedEventUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val source: SourceType = savedStateHandle[SOURCE_KEY] ?: error("Source is mandatory")

    fun onQrScanned(qrCode: String) {
        viewModelScope.launch {
            emitQrScannedEventUseCase.invoke(source, qrCode)
        }
    }
}