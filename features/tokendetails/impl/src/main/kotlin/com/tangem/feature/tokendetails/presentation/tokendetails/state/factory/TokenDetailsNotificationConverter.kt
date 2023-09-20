package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.domain.tokens.models.warnings.CryptoCurrencyWarning
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

    fun removeExistentialDeposit(currentState: TokenDetailsState): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is TokenDetailsNotification.ExistentialDeposit }
        return newNotifications.toImmutableList()
    }

    fun removeRentInfo(currentState: TokenDetailsState): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is TokenDetailsNotification.RentInfo }
        return newNotifications.toImmutableList()
    }

    private fun mapToNotification(warning: CryptoCurrencyWarning): TokenDetailsNotification {
        return when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> TokenDetailsNotification.NetworkFeeFee(
                feeInfo = warning,
                onBuyClick = clickIntents::onBuyClick,
            )
            is CryptoCurrencyWarning.ExistentialDeposit -> TokenDetailsNotification.ExistentialDeposit(
                existentialInfo = warning,
                onCloseClick = clickIntents::onCloseExistentialDepositNotification,
            )
            is CryptoCurrencyWarning.Rent -> TokenDetailsNotification.RentInfo(
                rentInfo = warning,
                onCloseClick = clickIntents::onCloseRentInfoNotification,
            )
            CryptoCurrencyWarning.SomeNetworksUnreachable -> TokenDetailsNotification.NetworksUnreachable
        }
    }
}