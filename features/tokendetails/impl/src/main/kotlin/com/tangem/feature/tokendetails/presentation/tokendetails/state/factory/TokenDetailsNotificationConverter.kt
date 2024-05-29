package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification.*
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.removeBy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import java.math.BigDecimal

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

    fun removeHederaAssociateWarning(currentState: TokenDetailsState): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is HederaAssociateWarning }
        return newNotifications.toImmutableList()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
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
                amount = BigDecimalFormatter.formatCryptoAmount(
                    cryptoAmount = warning.amountToCreateAccount,
                    cryptoCurrency = "",
                    decimals = warning.amountCurrency.decimals,
                ),
                symbol = warning.amountCurrency.symbol,
            )
            is CryptoCurrencyWarning.TopUpWithoutReserve -> TopUpWithoutReserve
            is CryptoCurrencyWarning.SwapPromo -> SwapPromo(
                startDateTime = warning.startDateTime,
                endDateTime = warning.endDateTime,
                onSwapClick = clickIntents::onSwapPromoClick,
                onCloseClick = clickIntents::onSwapPromoDismiss,
            )
            is CryptoCurrencyWarning.BeaconChainShutdown -> NetworkShutdown(
                title = resourceReference(R.string.warning_beacon_chain_retirement_title),
                subtitle = resourceReference(R.string.warning_beacon_chain_retirement_content),
            )

            is HederaWarnings.AssociateWarning -> HederaAssociateWarning(
                currency = warning.currency,
                fee = null,
                feeCurrencySymbol = null,
                onAssociateClick = clickIntents::onAssociateClick,
            )
            is HederaWarnings.AssociateWarningWithFee -> HederaAssociateWarning(
                currency = warning.currency,
                fee = BigDecimalFormatter.formatCryptoAmount(
                    cryptoAmount = warning.fee,
                    cryptoCurrency = "",
                    decimals = warning.feeCurrencyDecimals,
                ),
                feeCurrencySymbol = warning.feeCurrencySymbol,
                onAssociateClick = clickIntents::onAssociateClick,
            )
            is CryptoCurrencyWarning.FeeResourceInfo -> KoinosMana(
                manaBalanceAmount = formatMana(warning.amount),
                maxManaBalanceAmount = warning.maxAmount?.let {
                    formatMana(it)
                } ?: run {
                    Timber.e("FeeResource maxAmount cannot be null in Koinos. Check KoinosWalletManager")
                    ""
                },
            )
        }
    }

    private fun formatMana(amount: BigDecimal): String {
        return BigDecimalFormatter.formatCryptoAmountShorted(
            cryptoAmount = amount,
            cryptoCurrency = "",
            decimals = Blockchain.Koinos.decimals(),
        )
    }

    // workaround for networks that users have misunderstanding
    private fun CryptoCurrency.shouldMergeFeeNetworkName(): Boolean {
        return Blockchain.fromNetworkId(this.network.backendId) == Blockchain.Arbitrum
    }
}
