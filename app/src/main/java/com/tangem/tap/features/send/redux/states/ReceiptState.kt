package com.tangem.tap.features.send.redux.states

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
    val mainCurrency: MainCurrency? = null
) : SendScreenState {
    override val stateId: StateId = StateId.RECEIPT
}

data class ReceiptSymbols(
    val fiat: String,
    val crypto: String,
    val token: String? = null
)

data class ReceiptFiat(
    val amountFiat: String,
    val feeFiat: String,
    val totalFiat: String,
    val willSentCrypto: String,
    val symbols: ReceiptSymbols
)

data class ReceiptCrypto(
    val amountCrypto: String,
    val feeCrypto: String,
    val totalCrypto: String,
    val feeFiat: String,
    val willSentFiat: String,
    val symbols: ReceiptSymbols
)

data class ReceiptTokenCrypto(
    val amountToken: String,
    val feeCoin: String,
    val totalFiat: String,
    val symbols: ReceiptSymbols
)

data class ReceiptTokenFiat(
    val amountFiat: String,
    val feeFiat: String,
    val totalFiat: String,
    val willSentToken: String,
    val willSentFeeCoin: String,
    val symbols: ReceiptSymbols
)