package com.tangem.feature.wallet.presentation.wallet.qr

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

internal sealed class ClassifiedQrContent {

    data class WalletConnect(val uri: String) : ClassifiedQrContent()

    data class PaymentUri(
        val currency: CryptoCurrency,
        val address: String,
        val amount: BigDecimal?,
        val memo: String?,
    ) : ClassifiedQrContent()

    data class PlainAddress(
        val address: String,
        val matchingCurrencies: List<CryptoCurrency>,
    ) : ClassifiedQrContent()

    data class Unknown(val raw: String) : ClassifiedQrContent()
}