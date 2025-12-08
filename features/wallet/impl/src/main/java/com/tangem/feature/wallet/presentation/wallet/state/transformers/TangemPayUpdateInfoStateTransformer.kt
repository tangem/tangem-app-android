package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.CustomerInfo.ProductInstance
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createCancelledState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueProgressState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createKycInProgressState
import java.util.Currency

/**
 * Hardcode Polygon chain id only for F&F.
 * Later chain id will be fetched from BFF.
 */
private const val POLYGON_CHAIN_ID = 137

internal class TangemPayUpdateInfoStateTransformer(
    userWalletId: UserWalletId,
    private val value: MainScreenCustomerInfo? = null,
    private val cardFrozenState: TangemPayCardFrozenState,
    private val onClickKyc: () -> Unit = {},
    private val onIssuingCard: () -> Unit = {},
    private val onIssuingFailed: () -> Unit = {},
    private val openDetails: (config: TangemPayDetailsConfig) -> Unit = {},
) : WalletStateTransformer(userWalletId = userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        val tangemPayState = createInitialState()
        return if (prevState is WalletState.MultiCurrency.Content) {
            prevState.copy(tangemPayState = tangemPayState)
        } else {
            prevState
        }
    }

    private fun createInitialState(): TangemPayState {
        val cardInfo = value?.info?.cardInfo
        val productInstance = value?.info?.productInstance

        // when statement copied to WalletTangemPayAnalyticsEventSender. Be careful when editing.
        return when {
            value == null -> TangemPayState.Empty
            value.orderStatus == OrderStatus.CANCELED -> createCancelledState(onIssuingFailed)
            !value.info.isKycApproved -> createKycInProgressState(onClickKyc)
            cardInfo != null && productInstance != null -> getCardInfoState(cardInfo, productInstance)
            else -> createIssueProgressState(onIssuingCard)
        }
    }

    private fun getCardInfoState(cardInfo: CardInfo, productInstance: ProductInstance): TangemPayState =
        TangemPayState.Card(
            lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
            balanceText = TextReference.Str(getBalanceText(cardInfo)),
            balanceSymbol = stringReference("USDC"), // TODO hardcode for now
            onClick = {
                openDetails(
                    TangemPayDetailsConfig(
                        cardId = productInstance.cardId,
                        cardFrozenState = cardFrozenState,
                        customerWalletAddress = cardInfo.customerWalletAddress,
                        cardNumberEnd = cardInfo.lastFourDigits,
                        chainId = POLYGON_CHAIN_ID,
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