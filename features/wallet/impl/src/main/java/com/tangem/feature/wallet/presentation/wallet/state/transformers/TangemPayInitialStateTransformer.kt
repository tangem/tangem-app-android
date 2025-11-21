package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus.CANCELED
import com.tangem.domain.pay.model.OrderStatus.UNKNOWN
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueAvailableState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueProgressState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createKycInProgressState
import java.util.Currency

/**
 * Hardcode Polygon chain id only for F&F.
 * Later chain id will be fetched from BFF.
 */
private const val POLYGON_CHAIN_ID = 137

internal class TangemPayInitialStateTransformer(
    private val value: MainScreenCustomerInfo? = null,
    private val cardFrozenState: TangemPayCardFrozenState,
    private val onClickIssue: () -> Unit = {},
    private val onClickKyc: () -> Unit = {},
    private val openDetails: (config: TangemPayDetailsConfig) -> Unit = {},
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val tangemPayState = createInitialState()
        return prevState.copy(tangemPayState = tangemPayState)
    }

    private fun createInitialState(): TangemPayState {
        val cardInfo = value?.info?.cardInfo
        val productInstance = value?.info?.productInstance
        return when {
            value == null -> TangemPayState.Empty
            !value.info.isKycApproved -> createKycInProgressState(onClickKyc)
            cardInfo != null && productInstance != null -> getCardInfoState(cardInfo, productInstance)
            value.orderStatus == UNKNOWN || value.orderStatus == CANCELED -> createIssueAvailableState(onClickIssue)
            else -> createIssueProgressState()
        }
    }

    private fun getCardInfoState(cardInfo: CardInfo, productInstance: ProductInstance): TangemPayState =
        TangemPayState.Card(
            lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
            balanceText = TextReference.Str(getBalanceText(cardInfo)),
            onClick = {
                openDetails(
                    TangemPayDetailsConfig(
                        cardId = productInstance.cardId,
                        cardFrozenState = cardFrozenState,
                        customerWalletAddress = cardInfo.customerWalletAddress,
                        cardNumberEnd = cardInfo.lastFourDigits,
                        chainId = POLYGON_CHAIN_ID,
                        depositAddress = cardInfo.depositAddress,
                    ),
                )
            },
        )

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}