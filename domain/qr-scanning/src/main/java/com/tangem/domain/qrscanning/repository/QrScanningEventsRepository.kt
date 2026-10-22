package com.tangem.domain.qrscanning.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.models.SourceType
import kotlinx.coroutines.flow.Flow

interface QrScanningEventsRepository {

    suspend fun emitResult(qrCode: RawQrResult)

    fun subscribeToScanningResults(type: SourceType): Flow<RawQrResult>

    fun parseQrCode(qrCode: String, cryptoCurrency: CryptoCurrency): QrResult

    fun classify(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent

    /**
     * Extracts the recipient address from a scanned QR code without knowing its currency.
     * Returns an empty string when the payment URI calls a contract function other than `transfer`, or a
     * `transfer` lacks the `address` parameter, so a call target is never mistaken for the recipient.
     */
    fun extractRecipientAddress(qrCode: String): String
}