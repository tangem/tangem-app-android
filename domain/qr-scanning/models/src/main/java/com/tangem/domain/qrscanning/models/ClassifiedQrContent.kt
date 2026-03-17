package com.tangem.domain.qrscanning.models

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

sealed class ClassifiedQrContent {

    data class WalletConnect(val uri: String) : ClassifiedQrContent()

    data class PaymentUri(
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
        val matchingCurrencies: List<CryptoCurrency>,
    ) : ClassifiedQrContent()

    data class PlainAddress(
        val address: String,
        val matchingCurrencies: List<CryptoCurrency>,
    ) : ClassifiedQrContent()

    sealed class Error : ClassifiedQrContent() {

        /** QR code not recognized by any parser */
        data class Unrecognized(val raw: String) : Error()

        /** Network or token recognized but not available in user's wallet */
        data object UnsupportedNetwork : Error()
    }
}