package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchain.common.Wallet
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.tap.common.extensions.scaleToFiat
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.ReceiptAction.RefreshReceipt
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.store
import com.tangem.utils.StringsSigns.LOWER_SIGN
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
class ReceiptReducer : SendInternalReducer {

    private lateinit var sendState: SendState
    private lateinit var amountState: AmountState
    private lateinit var feeState: FeeState

    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        this.sendState = sendState
        this.amountState = sendState.amountState
        this.feeState = sendState.feeState

        return when (action) {
            is RefreshReceipt -> handleRefresh(sendState.receiptState)
            else -> sendState
        }
    }

    private fun handleRefresh(state: ReceiptState): SendState {
        val wallet = sendState.walletManager?.wallet ?: return sendState
        val feePaidCurrency = wallet.blockchain.feePaidCurrency()

        val layoutType = determineLayoutType(amountState.mainCurrency.type, amountState.typeOfAmount, feePaidCurrency)
        val symbols = determineSymbols(wallet, amountState.typeOfAmount, feePaidCurrency)
        val showBlank = !SendState.isReadyToSend()
        val result = state.copy(
            visibleTypeOfReceipt = layoutType,
            mainCurrency = amountState.mainCurrency,
            fiat = createFiatType(symbols, showBlank),
            crypto = createCryptoType(symbols, showBlank),
            tokenFiat = createTokenFiatType(symbols, showBlank),
            tokenCrypto = createTokenCryptoType(symbols, showBlank),
            customTokenFiat = createCustomTokenFiat(symbols, showBlank),
            customTokenCrypto = createCustomTokenCrypto(symbols, showBlank),
            sameCurrencyFiat = createSameCurrencyFiat(symbols, showBlank),
            sameCurrencyCrypto = createSameCurrencyCrypto(symbols, showBlank),
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
                symbols = symbols,
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
                symbols = symbols,
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
                feeCrypto = feeCrypto.stripZeroPlainString().addPrecisionSign(),
                totalCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(amountState.amountToSendCrypto),
                symbols = symbols,
            )
        } else {
            val totalCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            return ReceiptCrypto(
                amountCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString().addPrecisionSign(),
                totalCrypto = totalCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(totalCrypto),
                symbols = symbols,
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
                feeFiat = feeFiat.scaleToFiat(true).stripZeroPlainString().addPrecisionSign(),
                totalFiat = totalFiat.scaleToFiat(true).stripZeroPlainString(),
                willSentToken = tokensToSend.stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols,
            )
        } else {
            ReceiptTokenFiat(
                amountFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                feeFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                totalFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                willSentToken = tokensToSend.stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols,
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
                symbols = symbols,
            )
        } else {
            ReceiptTokenCrypto(
                amountToken = tokensToSend.stripZeroPlainString(),
                feeCoin = feeCoin.stripZeroPlainString().addPrecisionSign(),
                totalFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                symbols = symbols,
            )
        }
    }

    private fun createCustomTokenCrypto(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptTokenCrypto {
        val feeValue = feeState.getCurrentFeeValue()
        if (showBlank) {
            return ReceiptTokenCrypto("0", feeValue.stripZeroPlainString(), "0", symbols)
        }
        val tokensToSend = amountState.amountToSendCrypto
        val amountType = amountState.typeOfAmount
        val currencyConverter = when {
            amountType is AmountType.Token && sendState.feeIsConvertible() -> sendState.tokenConverter
            amountType is AmountType.Coin && sendState.feeIsConvertible() -> sendState.coinConverter
            else -> null
        }

        return if (currencyConverter != null) {
            val currencyFiat = currencyConverter.toFiatUnscaled(tokensToSend)
            val feeFiat = sendState.customFeeConverter!!.toFiatUnscaled(feeValue)
            val totalFiat = currencyFiat.plus(feeFiat)

            ReceiptTokenCrypto(
                amountToken = tokensToSend.stripZeroPlainString(),
                feeCoin = feeValue.stripZeroPlainString().addPrecisionSign(),
                totalFiat = totalFiat.scaleToFiat(true).stripZeroPlainString(),
                symbols = symbols,
            )
        } else {
            ReceiptTokenCrypto(
                amountToken = tokensToSend.stripZeroPlainString(),
                feeCoin = feeValue.stripZeroPlainString().addPrecisionSign(),
                totalFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                symbols = symbols,
            )
        }
    }

    private fun createCustomTokenFiat(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptTokenFiat {
        val feeCoin = feeState.getCurrentFeeValue()
        if (showBlank) {
            val feeFiat = convertToFiatPrecision(feeCoin)
            return ReceiptTokenFiat("0", feeFiat, "0", "0", "0", symbols)
        }

        val tokensToSend = amountState.amountToSendCrypto

        val amountType = amountState.typeOfAmount
        val currencyConverter = when {
            amountType is AmountType.Token && sendState.feeIsConvertible() -> sendState.tokenConverter
            amountType is AmountType.Coin && sendState.feeIsConvertible() -> sendState.coinConverter
            else -> null
        }

        return if (currencyConverter != null) {
            val feeFiat = sendState.customFeeConverter!!.toFiatUnscaled(feeCoin)
            val amountFiat = currencyConverter.toFiatUnscaled(tokensToSend)
            val totalFiat = amountFiat.plus(feeFiat)
            ReceiptTokenFiat(
                amountFiat = amountFiat.scaleToFiat(true).stripZeroPlainString(),
                feeFiat = feeFiat.scaleToFiat(true).stripZeroPlainString().addPrecisionSign(),
                totalFiat = totalFiat.scaleToFiat(true).stripZeroPlainString(),
                willSentToken = tokensToSend.scaleToFiat(true).stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols,
            )
        } else {
            ReceiptTokenFiat(
                amountFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                feeFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                totalFiat = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                willSentToken = tokensToSend.stripZeroPlainString(),
                willSentFeeCoin = feeCoin.stripZeroPlainString(),
                symbols = symbols,
            )
        }
    }

    private fun createSameCurrencyCrypto(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptCrypto {
        val feeCrypto = feeState.getCurrentFeeValue()
        val feeFiat = convertToFiatPrecision(feeCrypto)
        if (showBlank) {
            return ReceiptCrypto("0", feeCrypto.stripZeroPlainString(), "0", "0", "0", symbols)
        }
        val isToken = amountState.typeOfAmount is AmountType.Token

        if (feeState.feeIsIncluded) {
            return ReceiptCrypto(
                amountCrypto = amountState.amountToSendCrypto.minus(feeCrypto).stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString().addPrecisionSign(),
                totalCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(value = amountState.amountToSendCrypto, isToken = isToken),
                symbols = symbols,
            )
        } else {
            val totalCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            return ReceiptCrypto(
                amountCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString().addPrecisionSign(),
                totalCrypto = totalCrypto.stripZeroPlainString(),
                feeFiat = feeFiat,
                willSentFiat = convertToFiatPrecision(value = totalCrypto, isToken = isToken),
                symbols = symbols,
            )
        }
    }

    private fun createSameCurrencyFiat(symbols: ReceiptSymbols, showBlank: Boolean): ReceiptFiat {
        val feeCrypto = feeState.getCurrentFeeValue()
        val isToken = amountState.typeOfAmount is AmountType.Token
        val feeFiat = convertToFiatPrecision(feeCrypto, isToken = isToken)

        if (showBlank) {
            return ReceiptFiat("0", feeFiat, "0", "0", symbols)
        }

        return if (feeState.feeIsIncluded) {
            val amountFiat = convertToFiatPrecision(amountState.amountToSendCrypto.minus(feeCrypto), isToken = isToken)
            val totalFiat = convertToFiatPrecision(amountState.amountToSendCrypto, isToken = isToken)
            ReceiptFiat(
                amountFiat = amountFiat,
                feeFiat = feeFiat,
                totalFiat = totalFiat,
                willSentCrypto = amountState.amountToSendCrypto.scaleToFiat(true).stripZeroPlainString(),
                symbols = symbols,
            )
        } else {
            val totalAmountCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            val amountFiat = convertToFiatPrecision(amountState.amountToSendCrypto, isToken = isToken)
            val totalFiat = convertToFiatPrecision(totalAmountCrypto, isToken = isToken)
            ReceiptFiat(
                amountFiat = amountFiat,
                feeFiat = feeFiat,
                totalFiat = totalFiat,
                willSentCrypto = totalAmountCrypto.scaleToFiat().stripZeroPlainString(),
                symbols = symbols,
            )
        }
    }

    private fun determineSymbols(
        wallet: Wallet,
        amountType: AmountType,
        feePaidCurrency: FeePaidCurrency,
    ): ReceiptSymbols {
        return ReceiptSymbols(
            fiat = store.state.globalState.appCurrency.code,
            crypto = wallet.blockchain.currency,
            token = when (amountType) {
                is AmountType.Token -> amountType.token.symbol
                else -> null
            },
            fee = when (feePaidCurrency) {
                FeePaidCurrency.Coin -> wallet.blockchain.currency
                FeePaidCurrency.SameCurrency -> store.state.sendState.currency?.symbol
                is FeePaidCurrency.Token -> feePaidCurrency.token.symbol
                is FeePaidCurrency.FeeResource -> feePaidCurrency.currency
            },
        )
    }

    private fun determineLayoutType(
        mainCurrencyType: MainCurrencyType,
        amountType: AmountType,
        feePaidCurrency: FeePaidCurrency,
    ): ReceiptLayoutType {
        return when (mainCurrencyType) {
            MainCurrencyType.FIAT -> determineFiatLayoutType(feePaidCurrency, amountType)
            MainCurrencyType.CRYPTO -> determineCryptoLayoutType(feePaidCurrency, amountType)
        }
    }

    private fun determineFiatLayoutType(feePaidCurrency: FeePaidCurrency, amountType: AmountType): ReceiptLayoutType {
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> {
                val amountToken = (amountType as? AmountType.Token)?.token
                val sameToken =
                    feePaidCurrency.token.contractAddress.equals(amountToken?.contractAddress, ignoreCase = true)
                if (sameToken) ReceiptLayoutType.SAME_CURRENCY_FIAT else ReceiptLayoutType.FEE_IN_CUSTOM_TOKEN_FIAT
            }
            FeePaidCurrency.Coin -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.FIAT
                is AmountType.Token -> ReceiptLayoutType.TOKEN_FIAT
                is AmountType.FeeResource,
                AmountType.Reserve,
                -> ReceiptLayoutType.UNKNOWN
            }
            FeePaidCurrency.SameCurrency -> ReceiptLayoutType.SAME_CURRENCY_FIAT
            is FeePaidCurrency.FeeResource -> ReceiptLayoutType.SAME_CURRENCY
        }
    }

    private fun determineCryptoLayoutType(feePaidCurrency: FeePaidCurrency, amountType: AmountType): ReceiptLayoutType {
        return when (feePaidCurrency) {
            is FeePaidCurrency.Token -> {
                val amountToken = (amountType as? AmountType.Token)?.token
                val sameToken =
                    feePaidCurrency.token.contractAddress.equals(amountToken?.contractAddress, ignoreCase = true)
                if (sameToken) ReceiptLayoutType.SAME_CURRENCY else ReceiptLayoutType.FEE_IN_CUSTOM_TOKEN
            }
            FeePaidCurrency.SameCurrency -> ReceiptLayoutType.SAME_CURRENCY
            FeePaidCurrency.Coin -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.CRYPTO
                is AmountType.Token -> ReceiptLayoutType.TOKEN_CRYPTO
                is AmountType.FeeResource,
                AmountType.Reserve,
                -> ReceiptLayoutType.UNKNOWN
            }
            is FeePaidCurrency.FeeResource -> ReceiptLayoutType.UNKNOWN
        }
    }

    private fun convertToFiatPrecision(value: BigDecimal, isToken: Boolean = false): String {
        return when {
            !isToken && sendState.coinIsConvertible() -> {
                sendState.coinConverter!!.toFiatWithPrecision(value).stripZeroPlainString()
            }
            isToken && sendState.tokenIsConvertible() -> {
                sendState.tokenConverter!!.toFiatWithPrecision(value).stripZeroPlainString()
            }
            else -> {
                BigDecimalFormatter.EMPTY_BALANCE_SIGN
            }
        }
    }

    private fun String.addPrecisionSign(): String {
        val result = if (feeState.feeIsApproximate) "$LOWER_SIGN $this" else this
        return result.trim()
    }
}