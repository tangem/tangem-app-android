package com.tangem.domain.qrscanning.usecases

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import com.tangem.domain.tokens.model.CryptoCurrency

class ParseQrCodeUseCase(
    val repository: QrScanningEventsRepository,
) {
    operator fun invoke(qrCode: String, cryptoCurrency: CryptoCurrency): Either<Exception, QrResult> {
        return try {
            repository.parseQrCode(qrCode, cryptoCurrency).right()
        } catch (e: Exception) {
            e.left()
        }
    }
}