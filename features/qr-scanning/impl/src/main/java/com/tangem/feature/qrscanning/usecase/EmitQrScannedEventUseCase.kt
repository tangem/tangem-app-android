package com.tangem.feature.qrscanning.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.feature.qrscanning.SourceType
import com.tangem.feature.qrscanning.repo.QrScanningEventsRepository

internal class EmitQrScannedEventUseCase(
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