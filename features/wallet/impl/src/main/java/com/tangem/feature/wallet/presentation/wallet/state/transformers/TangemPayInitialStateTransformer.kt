package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.CustomerInfo.CardInfo
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.OrderStatus.CANCELED
import com.tangem.domain.pay.model.OrderStatus.UNKNOWN
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueAvailableState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createIssueProgressState
import com.tangem.feature.wallet.presentation.wallet.state.util.TangemPayStateCreator.createKycInProgressState
import java.util.Currency

internal class TangemPayInitialStateTransformer(
    private val value: MainScreenCustomerInfo? = null,
    private val onClickIssue: () -> Unit = {},
    private val onClickKyc: () -> Unit = {},
    private val openDetails: (customerWalletAddress: String, cardNumberEnd: String) -> Unit = { _, _ -> },
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val tangemPayState = createInitialState()
        return prevState.copy(tangemPayState = tangemPayState)
    }

    private fun createInitialState(): TangemPayState {
        val cardInfo = value?.info?.cardInfo
        return when {
            value == null -> TangemPayState.Empty
            !value.info.isKycApproved -> createKycInProgressState(onClickKyc)
            cardInfo != null -> getCardInfoState(cardInfo)
            value.orderStatus == UNKNOWN || value.orderStatus == CANCELED -> createIssueAvailableState(onClickIssue)
            else -> createIssueProgressState()
        }
    }

    private fun getCardInfoState(cardInfo: CardInfo): TangemPayState = TangemPayState.Card(
        lastFourDigits = TextReference.Str("*${cardInfo.lastFourDigits}"),
        balanceText = TextReference.Str(getBalanceText(cardInfo)),
        onClick = { openDetails(cardInfo.customerWalletAddress, cardInfo.lastFourDigits) },
    )

    private fun getBalanceText(cardInfo: CardInfo): String {
        val currency = Currency.getInstance(cardInfo.currencyCode)
        return cardInfo.balance.format {
            fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
        }
    }
}