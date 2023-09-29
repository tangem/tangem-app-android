package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.common.extensions.cast
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TokenDetailsNotificationConverter(
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<Set<CryptoCurrencyWarning>, ImmutableList<TokenDetailsNotification>> {

    override fun convert(value: Set<CryptoCurrencyWarning>): ImmutableList<TokenDetailsNotification> {
        return value.map(::mapToNotification).toImmutableList()
    }

    fun getStateRentInfoVisibility(
        currentState: TokenDetailsState,
        isVisible: Boolean,
    ): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        val oldNotification = newNotifications.find { it is TokenDetailsNotification.RentInfo }
        oldNotification?.let {
            newNotifications.add(it.cast<TokenDetailsNotification.RentInfo>().copy(isVisible = isVisible))
        }
        return newNotifications.toImmutableList()
    }

    private fun mapToNotification(warning: CryptoCurrencyWarning): TokenDetailsNotification {
        return when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> TokenDetailsNotification.NetworkFee(
                feeInfo = warning,
                onBuyClick = clickIntents::onBuyClick,
            )
            is CryptoCurrencyWarning.ExistentialDeposit -> TokenDetailsNotification.ExistentialDeposit(
                existentialInfo = warning,
            )
            is CryptoCurrencyWarning.Rent -> TokenDetailsNotification.RentInfo(
                rentInfo = warning,
                onCloseClick = clickIntents::onCloseRentInfoNotification,
            )
            CryptoCurrencyWarning.SomeNetworksUnreachable -> TokenDetailsNotification.NetworksUnreachable
        }
    }
}
