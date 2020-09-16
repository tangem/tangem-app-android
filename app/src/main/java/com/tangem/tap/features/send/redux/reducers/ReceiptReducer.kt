package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.features.send.redux.ReceiptAction.RefreshReceipt
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
class ReceiptReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        return when (action) {
            is RefreshReceipt -> handleRefresh(action, sendState, sendState.receiptState)
            else -> sendState
        }
    }


    private fun handleRefresh(action: RefreshReceipt, sendState: SendState, state: ReceiptState): SendState {
        val wallet = sendState.walletManager?.wallet ?: return sendState

        val converter = sendState.currencyConverter
        val amountState = sendState.amountState
        val feeState = sendState.feeState

        val layoutType = determineLayoutType(amountState.mainCurrency.value, amountState.typeOfAmount)
        val symbols = determineSymbols(wallet)
        val showBlank = !sendState.isReadyToSend()
        val result = state.copy(
                visibleTypeOfReceipt = layoutType,
                mainCurrencyType = sendState.amountState.mainCurrency,
                fiat = createFiatType(converter, amountState, feeState, symbols, showBlank),
                crypto = createCryptoType(converter, amountState, feeState, symbols, showBlank),
                tokenFiat = createTokenFiatType(converter, amountState, feeState, symbols, showBlank),
                tokenCrypto = createTokenCryptoType(converter, amountState, feeState, symbols, showBlank)
        )
        return updateLastState(sendState.copy(receiptState = result), result)
    }

    private fun createFiatType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols,
            showBlank: Boolean
    ): ReceiptFiat {
        val feeCrypto = feeState.getCurrentFee()
        val feeFiat = converter.toFiat(feeCrypto)

        if (showBlank) {
            return ReceiptFiat("0", feeFiat.stripZeroPlainString(), feeFiat.stripZeroPlainString(), "0", symbols)
        }

        return if (feeState.feeIsIncluded) {
            val amountFiat = converter.toFiat(amountState.amountToSendCrypto.minus(feeCrypto))
            val totalFiat = converter.toFiat(amountState.amountToSendCrypto)
            ReceiptFiat(
                    amountFiat = amountFiat.stripZeroPlainString(),
                    feeFiat = feeFiat.stripZeroPlainString(),
                    totalFiat = totalFiat.stripZeroPlainString(),
                    willSentCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                    symbols = symbols
            )
        } else {
            val totalAmountCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            val amountFiat = converter.toFiat(amountState.amountToSendCrypto)
            val totalFiat = converter.toFiat(totalAmountCrypto)
            ReceiptFiat(
                    amountFiat = amountFiat.stripZeroPlainString(),
                    feeFiat = feeFiat.stripZeroPlainString(),
                    totalFiat = totalFiat.stripZeroPlainString(),
                    willSentCrypto = totalAmountCrypto.stripZeroPlainString(),
                    symbols = symbols
            )
        }
    }

    private fun createCryptoType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols,
            showBlank: Boolean
    ): ReceiptCrypto {
        val feeCrypto = feeState.getCurrentFee()

        if (showBlank) {
            return ReceiptCrypto("0", feeCrypto.stripZeroPlainString(), "0", converter.toFiat(feeCrypto).stripZeroPlainString(), "0", symbols)
        }

        if (feeState.feeIsIncluded) {
            return ReceiptCrypto(
                    amountCrypto = amountState.amountToSendCrypto.minus(feeCrypto).stripZeroPlainString(),
                    feeCrypto = feeCrypto.stripZeroPlainString(),
                    totalCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                    feeFiat = converter.toFiat(feeCrypto).stripZeroPlainString(),
                    willSentFiat = converter.toFiat(amountState.amountToSendCrypto).stripZeroPlainString(),
                    symbols = symbols
            )
        } else {
            val totalCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            return ReceiptCrypto(
                    amountCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                    feeCrypto = feeCrypto.stripZeroPlainString(),
                    totalCrypto = totalCrypto.stripZeroPlainString(),
                    feeFiat = converter.toFiat(feeCrypto).stripZeroPlainString(),
                    willSentFiat = converter.toFiat(totalCrypto).stripZeroPlainString(),
                    symbols = symbols
            )
        }
    }

    private fun createTokenFiatType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols,
            showBlank: Boolean
    ): ReceiptTokenFiat {
        val feeCrypto = feeState.getCurrentFee()
        val amountFiat = converter.toFiat(amountState.amountToSendCrypto)
        val feeFiat = converter.toFiat(feeCrypto)

        if (showBlank) {
            return ReceiptTokenFiat("0", feeFiat.stripZeroPlainString(), "0", "0", "0", symbols)
        }

        return ReceiptTokenFiat(
                amountFiat = amountFiat.stripZeroPlainString(),
                feeFiat = feeFiat.stripZeroPlainString(),
                totalFiat = amountFiat.plus(feeFiat).stripZeroPlainString(),
                willSentTokenCrypto = amountState.amountToSendCrypto.stripZeroPlainString(),
                willSentFeeCrypto = feeCrypto.stripZeroPlainString(),
                symbols = symbols
        )
    }

    private fun createTokenCryptoType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols,
            showBlank: Boolean
    ): ReceiptTokenCrypto {
        val feeCrypto = feeState.getCurrentFee()
        val totalFiat = converter.toFiat(amountState.amountToSendCrypto).plus(converter.toFiat(feeCrypto))
        if (showBlank) {
            return ReceiptTokenCrypto("0", feeCrypto.stripZeroPlainString(), "0", symbols)
        }

        return ReceiptTokenCrypto(
                amountToken = amountState.amountToSendCrypto.stripZeroPlainString(),
                feeCrypto = feeCrypto.stripZeroPlainString(),
                totalFiat = totalFiat.stripZeroPlainString(),
                symbols = symbols
        )
    }

    private fun determineSymbols(wallet: Wallet): ReceiptSymbols {
        return ReceiptSymbols(
                fiat = store.state.globalState.appCurrency,
                crypto = wallet.blockchain.currency,
                token = wallet.amounts[AmountType.Token]?.currencySymbol
        )
    }

    private fun determineLayoutType(mainCurrencyType: MainCurrencyType, amountType: AmountType): ReceiptLayoutType {
        return when (mainCurrencyType) {
            MainCurrencyType.FIAT -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.FIAT
                AmountType.Token -> ReceiptLayoutType.TOKEN_FIAT
                AmountType.Reserve -> ReceiptLayoutType.UNKNOWN
            }
            MainCurrencyType.CRYPTO -> when (amountType) {
                AmountType.Coin -> ReceiptLayoutType.CRYPTO
                AmountType.Token -> ReceiptLayoutType.TOKEN_CRYPTO
                AmountType.Reserve -> ReceiptLayoutType.UNKNOWN
            }
        }
    }
}