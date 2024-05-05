package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification.*
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
        newNotifications.removeBy { it is RentInfo }
        return newNotifications.toImmutableList()
    }

    private fun mapToNotification(warning: CryptoCurrencyWarning): TokenDetailsNotification {
        return when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> NetworkFeeWithBuyButton(
                currency = warning.tokenCurrency,
                networkName = warning.coinCurrency.network.name,
                feeCurrencyName = warning.coinCurrency.name,
                feeCurrencySymbol = warning.coinCurrency.symbol,
                mergeFeeNetworkName = warning.coinCurrency.shouldMergeFeeNetworkName(),
                onBuyClick = { clickIntents.onBuyCoinClick(warning.coinCurrency) },
            )
            is CryptoCurrencyWarning.CustomTokenNotEnoughForFee -> {
                val feeCurrency = warning.feeCurrency
                if (feeCurrency != null) {
                    NetworkFeeWithBuyButton(
                        currency = warning.currency,
                        networkName = feeCurrency.network.name,
                        feeCurrencyName = warning.feeCurrencyName,
                        feeCurrencySymbol = warning.feeCurrencySymbol,
                        mergeFeeNetworkName = warning.currency.shouldMergeFeeNetworkName(),
                        onBuyClick = { clickIntents.onBuyCoinClick(feeCurrency) },
                    )
                } else {
                    NetworkFee(
                        currency = warning.currency,
                        networkName = warning.networkName,
                        feeCurrencyName = warning.feeCurrencyName,
                        feeCurrencySymbol = warning.feeCurrencySymbol,
                    )
                }
            }
            is CryptoCurrencyWarning.ExistentialDeposit -> ExistentialDeposit(existentialInfo = warning)
            is CryptoCurrencyWarning.Rent -> RentInfo(
                rentInfo = warning,
                onCloseClick = clickIntents::onCloseRentInfoNotification,
            )
            CryptoCurrencyWarning.SomeNetworksUnreachable -> NetworksUnreachable
            is CryptoCurrencyWarning.SomeNetworksNoAccount -> NetworksNoAccount(
                network = warning.amountCurrency.name,
                amount = warning.amountToCreateAccount.toString(),
                symbol = warning.amountCurrency.symbol,
            )
            is CryptoCurrencyWarning.TopUpWithoutReserve -> TopUpWithoutReserve
            is CryptoCurrencyWarning.HasPendingTransactions -> HasPendingTransactions(
                coinSymbol = warning.blockchainSymbol,
            )
            is CryptoCurrencyWarning.SwapPromo -> SwapPromo(
                startDateTime = warning.startDateTime,
                endDateTime = warning.endDateTime,
                onSwapClick = clickIntents::onSwapPromoClick,
                onCloseClick = clickIntents::onSwapPromoDismiss,
            )
        }
    }

    // workaround for networks that users have misunderstanding
    private fun CryptoCurrency.shouldMergeFeeNetworkName(): Boolean {
        return Blockchain.fromNetworkId(this.network.backendId) == Blockchain.Arbitrum
    }
}
