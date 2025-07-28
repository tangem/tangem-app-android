package com.tangem.domain.qrscanning.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListenToQrScanningUseCase(
    val repository: QrScanningEventsRepository,
) {

    operator fun invoke(type: SourceType): Either<Exception, Flow<String>> {
        return try {
            repository.subscribeToScanningResults(type).map { it.qrCode }.right()
        } catch (e: Exception) {
            e.left()
        }
    }

    fun listen(type: SourceType): Either<Exception, Flow<RawQrResult>> {
        return try {
            repository.subscribeToScanningResults(type).right()
        } catch (e: Exception) {
            e.left()
        }
    }
}