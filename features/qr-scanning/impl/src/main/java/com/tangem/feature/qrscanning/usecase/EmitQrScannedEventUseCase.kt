package com.tangem.feature.qrscanning.usecase

import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository

internal class EmitQrScannedEventUseCase(
    private val repository: QrScanningEventsRepository,
) {

    operator fun invoke(type: SourceType, qrCode: String) {
        repository.emitResult(type, qrCode)
    }
}
