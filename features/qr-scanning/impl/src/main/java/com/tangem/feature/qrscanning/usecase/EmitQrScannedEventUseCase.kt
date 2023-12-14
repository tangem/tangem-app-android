package com.tangem.feature.qrscanning.usecase

import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScannedEventsRepository

internal class EmitQrScannedEventUseCase(
    private val repository: QrScannedEventsRepository,
) {

    operator fun invoke(type: SourceType, qrCode: String) {
        repository.emitScan(type, qrCode)
    }
}
