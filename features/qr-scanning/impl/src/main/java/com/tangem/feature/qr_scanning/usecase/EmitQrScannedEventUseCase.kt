package com.tangem.feature.qr_scanning.usecase

import com.tangem.feature.qr_scanning.SourceType
import com.tangem.feature.qr_scanning.repo.QrScannedEventsRepository

internal class EmitQrScannedEventUseCase(
    private val repository: QrScannedEventsRepository
) {

    operator fun invoke(type: SourceType, qrCode: String) {
        repository.emitScan(type, qrCode)
    }

}