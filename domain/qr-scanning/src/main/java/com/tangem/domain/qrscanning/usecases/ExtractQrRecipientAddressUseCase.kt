package com.tangem.domain.qrscanning.usecases

import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository

/** Resolves the recipient address of a scanned QR code when the target currency is not known yet. */
class ExtractQrRecipientAddressUseCase(
    private val repository: QrScanningEventsRepository,
) {
    operator fun invoke(qrCode: String): String = repository.extractRecipientAddress(qrCode)
}