package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.features.send.redux.ReceiptAction.RefreshReceipt
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.*

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
        val result = state.copy(
                visibleTypeOfReceipt = layoutType,
                mainCurrencyType = sendState.amountState.mainCurrency,
                mainLayoutIsVisible = sendState.isReadyToSend(),
                fiat = createFiatType(converter, amountState, feeState, symbols),
                crypto = createCryptoType(converter, amountState, feeState, symbols),
                tokenFiat = createTokenFiatType(converter, amountState, feeState, symbols),
                tokenCrypto = createTokenCryptoType(converter, amountState, feeState, symbols)
        )
        return updateLastState(sendState.copy(receiptState = result), result)
    }

    private fun createFiatType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols
    ): ReceiptFiat {
        val feeCrypto = feeState.getCurrentFee()
        val feeFiat = converter.toFiat(feeCrypto)

        return if (feeState.feeIsIncluded) {
            val amountFiat = converter.toFiat(amountState.amountToSendCrypto.minus(feeCrypto))
            val totalFiat = converter.toFiat(amountState.amountToSendCrypto)
            ReceiptFiat(
                    amountFiat = amountFiat,
                    feeFiat = feeFiat,
                    totalFiat = totalFiat,
                    willSentCrypto = amountState.amountToSendCrypto.stripTrailingZeros(),
                    symbols = symbols
            )
        } else {
            val totalAmountCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            val amountFiat = converter.toFiat(amountState.amountToSendCrypto)
            val totalFiat = converter.toFiat(totalAmountCrypto)
            ReceiptFiat(
                    amountFiat = amountFiat,
                    feeFiat = feeFiat,
                    totalFiat = totalFiat,
                    willSentCrypto = totalAmountCrypto.stripTrailingZeros(),
                    symbols = symbols
            )
        }
    }

    private fun createCryptoType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols
    ): ReceiptCrypto {
        val feeCrypto = feeState.getCurrentFee()

        if (feeState.feeIsIncluded) {
            return ReceiptCrypto(
                    amountCrypto = amountState.amountToSendCrypto.minus(feeCrypto).stripTrailingZeros(),
                    feeCrypto = feeCrypto.stripTrailingZeros(),
                    totalCrypto = amountState.amountToSendCrypto.stripTrailingZeros(),
                    willSentFiat = converter.toFiat(amountState.amountToSendCrypto),
                    symbols = symbols
            )
        } else {
            val totalCrypto = amountState.amountToSendCrypto.plus(feeCrypto)
            return ReceiptCrypto(
                    amountCrypto = amountState.amountToSendCrypto.stripTrailingZeros(),
                    feeCrypto = feeCrypto.stripTrailingZeros(),
                    totalCrypto = totalCrypto.stripTrailingZeros(),
                    willSentFiat = converter.toFiat(totalCrypto),
                    symbols = symbols
            )
        }
    }

    private fun createTokenFiatType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols
    ): ReceiptTokenFiat {
        val feeCrypto = feeState.getCurrentFee()
        val amountFiat = converter.toFiat(amountState.amountToSendCrypto)
        val feeFiat = converter.toFiat(feeCrypto)
        return ReceiptTokenFiat(
                amountFiat = amountFiat,
                feeFiat = feeFiat,
                totalFiat = amountFiat.plus(feeFiat),
                willSentTokenCrypto = amountState.amountToSendCrypto.stripTrailingZeros(),
                willSentFeeCrypto = feeCrypto.stripTrailingZeros(),
                symbols = symbols
        )
    }

    private fun createTokenCryptoType(
            converter: CurrencyConverter,
            amountState: AmountState,
            feeState: FeeState,
            symbols: ReceiptSymbols
    ): ReceiptTokenCrypto {
        val feeCrypto = feeState.getCurrentFee()
        val totalFiat = converter.toFiat(amountState.amountToSendCrypto).plus(converter.toFiat(feeCrypto))
        return ReceiptTokenCrypto(
                amountToken = amountState.amountToSendCrypto.stripTrailingZeros(),
                feeCrypto = feeCrypto.stripTrailingZeros(),
                totalFiat = totalFiat.stripTrailingZeros(),
                symbols = symbols
        )
    }

    private fun determineSymbols(wallet: Wallet): ReceiptSymbols {
        return ReceiptSymbols(
                fiat = TapCurrency.main,
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