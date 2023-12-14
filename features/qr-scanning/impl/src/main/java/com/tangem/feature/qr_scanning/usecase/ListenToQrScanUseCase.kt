package com.tangem.feature.qr_scanning.usecase

import com.tangem.feature.qr_scanning.SourceType
import com.tangem.feature.qr_scanning.repo.QrScannedEventsRepository
import kotlinx.coroutines.flow.Flow

class ListenToQrScanUseCase(
    val repository: QrScannedEventsRepository
) {

    operator fun invoke(type: SourceType) : Flow<String> {
        return repository.subscribeToScan(type)
    }
    
}