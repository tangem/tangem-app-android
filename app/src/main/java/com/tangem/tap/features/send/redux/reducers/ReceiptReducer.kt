package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.extensions.scaleToFiat
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.ReceiptAction.RefreshReceipt
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.store
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
class ReceiptReducer : SendInternalReducer {

    companion object {
        const val EMPTY = "-"
    }

    private lateinit var sendState: SendState
    private lateinit var amountState: AmountState
    private lateinit var feeState: FeeState

    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        this.sendState = sendState
        this.amountState = sendState.amountState
        this.feeState = sendState.feeState

        return when (action) {
            is RefreshReceipt -> handleRefresh(action, sendState.receiptState)
            else -> sendState
        }
    }


    private fun handleRefresh(action: RefreshReceipt, state: ReceiptState): SendState {
        val wallet = sendState.walletManager?.wallet ?: return sendState

        val layoutType = determineLayoutType(amountState.mainCurrency.type, amountState.typeOfAmount)
        val symbols = determineSymbols(wallet, amountState.typeOfAmount)
        val showBlank = !SendState.isReadyToSend()
        val result = state.copy(
            visibleTypeOfReceipt = layoutType,
            mainCurrency = amountState.mainCurrency,
            fiat = createFiatType(symbols, showBlank),
            crypto = createCryptoType(symbols, showBlank),
            tokenFiat = createTokenFiatType(symbols, showBlank),
            tokenCrypto = createTokenCryptoType(symbols, showBlank)
        )
        return updateLastState(sendState.copy(receiptState = result), result)
    }

    private fun createFiatType(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptFiat {
        val feeCrypto = feeState.getCurrentFeeValue()
        val feeFiat = convertToFiatPrecision(feeCrypto)

        if (showBlank) {
            return ReceiptFiat("0", feeFiat, "0", "0", symbols)
        }

        return if (feeState.feeIsIncluded) {
            val amountFiat = convertToFiatPrecision(amountState.amountToSendCrypto.minus(feeCrypto))
            val totalFiat = convertToFiatPrecision(amountState.amountToSendCrypto)
            ReceiptFiat(
                amountFiat = amountFiat,
                feeFiat = feeFiat,
                totalFiat = totalFiat,
                willSentCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                symbols = symbols
            )
        } else {
            val totalAmountCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            val amountFiat = convertToFiatPrecision(amountState.amountToSendCrypto)
            val totalFiat = convertToFiatPrecision(totalAmountCrypto)
            ReceiptFiat(
                amountFiat = amountFiat,
                feeFiat = feeFiat,
                totalFiat = totalFiat,
                willSentCrypto = totalAmountCrypto.stripZeroPlainString(),
                symbols = symbols
            )
        }
    }

    private fun createCryptoType(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptCrypto {
        val feeCrypto = feeState.getCurrentFeeValue()
        val feeFiat = convertToFiatPrecision(feeCrypto)
        if (showBlank) {
            return ReceiptCrypto("0", feeCrypto.stripZeroPlainString(), "0", "0", "0", symbols)
        }

        if (feeState.feeIsIncluded) {
            return ReceiptCrypto(
                amountCrypto = amountState.amountToSendCrypto.minus(feeCrypto).stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString(),
                totalCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(amountState.amountToSendCrypto),
                symbols = symbols
            )
        } else {
            val totalCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            return ReceiptCrypto(
                amountCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString(),
                totalCrypto = totalCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(totalCrypto),
                symbols = symbols
            )
        }
    }

    private fun createTokenFiatType(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptTokenFiat {
        val feeCoin = feeState.getCurrentFeeValue()
        if (showBlank) {
            val feeFiat = convertToFiatPrecision(feeCoin)
            return ReceiptTokenFiat("0", feeFiat, "0", "0", "0", symbols)
        }

        val tokensToSend = amountState.amountToSendCrypto

        return if (sendState.coinIsConvertible() && sendState.tokenIsConvertible()) {
            val feeFiat = sendState.coinConverter!!.toFiatUnscaled(feeCoin)
            val amountFiat = sendState.tokenConverter!!.toFiatUnscaled(tokensToSend)
            val totalFiat = amountFiat.plus(feeFiat)
            ReceiptTokenFiat(
                amountFiat = amountFiat.scaleToFiat(true).stripZeroPlainString(),
                feeFiat = feeFiat.scaleToFiat(true).stripZeroPlainString(),
                totalFiat = totalFiat.scaleToFiat(true).stripZeroPlainString(),
                willSentToken = tokensToSend.stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols
            )
        } else {
            ReceiptTokenFiat(
                amountFiat = EMPTY,
                feeFiat = EMPTY,
                totalFiat = EMPTY,
                willSentToken = tokensToSend.stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols
            )
        }
    }

    private fun createTokenCryptoType(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptTokenCrypto {
        val feeCoin = feeState.getCurrentFeeValue()
        if (showBlank) {
            return ReceiptTokenCrypto("0", feeCoin.stripZeroPlainString(), "0", symbols)
        }
        val tokensToSend = amountState.amountToSendCrypto

        return if (sendState.coinIsConvertible() && sendState.tokenIsConvertible()) {
            val tokenFiat = sendState.tokenConverter!!.toFiatUnscaled(tokensToSend)
            val feeFiat = sendState.coinConverter!!.toFiatUnscaled(feeCoin)
            val totalFiat = tokenFiat.plus(feeFiat)

            ReceiptTokenCrypto(
                amountToken = tokensToSend.stripZeroPlainString(),
                feeCoin = feeCoin.stripZeroPlainString().addPrecisionSign(),
                totalFiat = totalFiat.scaleToFiat(true).stripZeroPlainString(),
                symbols = symbols
            )
        } else {
            ReceiptTokenCrypto(
                amountToken = tokensToSend.stripZeroPlainString(),
                feeCoin = feeCoin.stripZeroPlainString().addPrecisionSign(),
                totalFiat = EMPTY,
                symbols = symbols
            )
        }

    }

    private fun determineSymbols(wallet: Wallet, amountType: AmountType): ReceiptSymbols {
        return ReceiptSymbols(
            fiat = store.state.globalState.appCurrency.code,
            crypto = wallet.blockchain.currency,
            token = when (amountType) {
                is AmountType.Token -> amountType.token.symbol
                else -> null
            }
        )
    }

    private fun determineLayoutType(mainCurrencyType: MainCurrencyType, amountType: AmountType): ReceiptLayoutType {
        return when (mainCurrencyType) {
            MainCurrencyType.FIAT -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.FIAT
                is AmountType.Token -> ReceiptLayoutType.TOKEN_FIAT
                AmountType.Reserve -> ReceiptLayoutType.UNKNOWN
            }
            MainCurrencyType.CRYPTO -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.CRYPTO
                is AmountType.Token -> ReceiptLayoutType.TOKEN_CRYPTO
                AmountType.Reserve -> ReceiptLayoutType.UNKNOWN
            }
        }
    }

    private fun convertToFiatPrecision(value: BigDecimal, isToken: Boolean = false): String {
        return when {
            !isToken && sendState.coinIsConvertible() -> sendState.coinConverter!!.toFiatWithPrecision(value).stripZeroPlainString()
            isToken && sendState.tokenIsConvertible() -> sendState.tokenConverter!!.toFiatWithPrecision(value).stripZeroPlainString()
            else -> EMPTY
        }
    }

    private fun String.addPrecisionSign(): String =
        ("${feeState.feePrecision.symbol} $this").trim()
}
