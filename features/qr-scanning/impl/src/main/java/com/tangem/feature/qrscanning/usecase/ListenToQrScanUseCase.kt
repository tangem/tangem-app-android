package com.tangem.feature.qrscanning.usecase

import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScannedEventsRepository
import kotlinx.coroutines.flow.Flow

class ListenToQrScanUseCase(
    val repository: QrScannedEventsRepository,
) {

    operator fun invoke(type: SourceType): Flow<String> {
        return repository.subscribeToScan(type)
    }
}
