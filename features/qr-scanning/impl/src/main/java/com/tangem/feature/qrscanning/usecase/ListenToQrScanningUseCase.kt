package com.tangem.feature.qrscanning.usecase

import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository
import kotlinx.coroutines.flow.Flow

class ListenToQrScanningUseCase(
    val repository: QrScanningEventsRepository,
) {

    operator fun invoke(type: SourceType): Flow<String> {
        return repository.subscribeToScanningResults(type)
    }
}
