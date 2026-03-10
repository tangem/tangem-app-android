package com.tangem.domain.qrscanning.usecases

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository

class ClassifyQrCodeUseCase(
    private val repository: QrScanningEventsRepository,
) {
    operator fun invoke(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent {
        return repository.classify(qrCode, userCurrencies)
    }
}