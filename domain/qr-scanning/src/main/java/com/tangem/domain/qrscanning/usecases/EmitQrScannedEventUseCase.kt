package com.tangem.domain.qrscanning.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository

class EmitQrScannedEventUseCase(
    private val repository: QrScanningEventsRepository,
) {
    suspend operator fun invoke(type: SourceType, qrCode: String): Either<Exception, Unit> {
        return try {
            repository.emitResult(type, qrCode)
            Unit.right()
        } catch (e: Exception) {
            e.left()
        }
    }
}