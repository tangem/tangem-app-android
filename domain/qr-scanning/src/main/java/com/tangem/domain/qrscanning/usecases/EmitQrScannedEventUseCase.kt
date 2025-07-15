package com.tangem.domain.qrscanning.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository

class EmitQrScannedEventUseCase(
    private val repository: QrScanningEventsRepository,
) {
    suspend operator fun invoke(qrCode: RawQrResult): Either<Exception, Unit> {
        return try {
            repository.emitResult(qrCode)
            Unit.right()
        } catch (e: Exception) {
            e.left()
        }
    }
}