package com.tangem.tap.features.send.redux.states

import org.rekotlin.StateType
import java.math.BigDecimal

// Shows only one type of the layout
enum class ReceiptLayoutType {
    UNKNOWN, FIAT, CRYPTO, TOKEN_FIAT, TOKEN_CRYPTO
}

data class ReceiptState(
        val visibleTypeOfReceipt: ReceiptLayoutType? = null,
        val fiat: ReceiptFiat? = null,
        val crypto: ReceiptCrypto? = null,
        val tokenFiat: ReceiptTokenFiat? = null,
        val tokenCrypto: ReceiptTokenCrypto? = null,
        val mainCurrencyType: Value<MainCurrencyType>? = null,
        val mainLayoutIsVisible: Boolean = false,
) : StateType

data class ReceiptSymbols(
        val fiat: String,
        val crypto: String,
        val token: String? = null
)

data class ReceiptFiat(
        val amountFiat: BigDecimal,
        val feeFiat: BigDecimal,
        val totalFiat: BigDecimal,
        val willSentCrypto: BigDecimal,
        val symbols: ReceiptSymbols
)

data class ReceiptCrypto(
        val amountCrypto: BigDecimal,
        val feeCrypto: BigDecimal,
        val totalCrypto: BigDecimal,
        val willSentFiat: BigDecimal,
        val symbols: ReceiptSymbols
)

data class ReceiptTokenCrypto(
        val amountToken: BigDecimal,
        val feeCrypto: BigDecimal,
        val totalFiat: BigDecimal,
        val symbols: ReceiptSymbols
)

data class ReceiptTokenFiat(
        val amountFiat: BigDecimal,
        val feeFiat: BigDecimal,
        val totalFiat: BigDecimal,
        val willSentTokenCrypto: BigDecimal,
        val willSentFeeCrypto: BigDecimal,
        val symbols: ReceiptSymbols
)