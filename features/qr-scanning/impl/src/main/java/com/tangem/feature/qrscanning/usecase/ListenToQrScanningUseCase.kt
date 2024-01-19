package com.tangem.feature.qrscanning.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class ListenToQrScanningUseCase(
    val repository: QrScanningEventsRepository,
) {

    operator fun invoke(type: SourceType): Either<Exception, Flow<String>> {
        return try {
            repository.subscribeToScanningResults(type).right()
        } catch (e: Exception) {
            e.left()
        }
    }
}