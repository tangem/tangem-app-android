package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.removeBy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TokenDetailsNotificationConverter(
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<Set<CryptoCurrencyWarning>, ImmutableList<TokenDetailsNotification>> {

    override fun convert(value: Set<CryptoCurrencyWarning>): ImmutableList<TokenDetailsNotification> {
        return value.map(::mapToNotification).toImmutableList()
    }

    fun removeRentInfo(currentState: TokenDetailsState): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is TokenDetailsNotification.RentInfo }
        return newNotifications.toImmutableList()
    }

    private fun mapToNotification(warning: CryptoCurrencyWarning): TokenDetailsNotification {
        return when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> TokenDetailsNotification.NetworkFee(
                feeInfo = warning,
                onBuyClick = { clickIntents.onBuyCoinClick(warning.coinCurrency) },
            )
            is CryptoCurrencyWarning.ExistentialDeposit -> TokenDetailsNotification.ExistentialDeposit(
                existentialInfo = warning,
            )
            is CryptoCurrencyWarning.Rent -> TokenDetailsNotification.RentInfo(
                rentInfo = warning,
                onCloseClick = clickIntents::onCloseRentInfoNotification,
            )
            CryptoCurrencyWarning.SomeNetworksUnreachable -> TokenDetailsNotification.NetworksUnreachable
            is CryptoCurrencyWarning.SomeNetworksNoAccount -> TokenDetailsNotification.NetworksNoAccount(
                network = warning.amountCurrency.name,
                amount = warning.amountToCreateAccount.toString(),
                symbol = warning.amountCurrency.symbol,
            )
            is CryptoCurrencyWarning.HasPendingTransactions -> TokenDetailsNotification.HasPendingTransactions(
                coinSymbol = warning.blockchainSymbol,
            )
            is CryptoCurrencyWarning.SwapPromo -> TokenDetailsNotification.SwapPromo(
                onSwapClick = clickIntents::onSwapPromoClick,
                onCloseClick = clickIntents::onSwapPromoDismiss,
            )
        }
    }
}