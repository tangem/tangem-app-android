package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.shorted
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification.*
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

    fun removeRequiredTrustlineWarning(currentState: TokenDetailsState): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is RequiredTrustlineWarning }
        return newNotifications.toImmutableList()
    }

    fun removeKaspaIncompleteTransactionWarning(
        currentState: TokenDetailsState,
    ): ImmutableList<TokenDetailsNotification> {
        val newNotifications = currentState.notifications.toMutableList()
        newNotifications.removeBy { it is KaspaIncompleteTransactionWarning }
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
                amount = warning.amountToCreateAccount.format {
                    crypto(symbol = "", decimals = warning.amountCurrency.decimals)
                },
                symbol = warning.amountCurrency.symbol,
            )
            is CryptoCurrencyWarning.TopUpWithoutReserve -> TopUpWithoutReserve
            is CryptoCurrencyWarning.SwapPromo -> SwapPromo(
                startDateTime = warning.startDateTime,
                endDateTime = warning.endDateTime,
                onSwapClick = { clickIntents.onSwapPromoClick(warning.promoId) },
                onCloseClick = { clickIntents.onSwapPromoDismiss(warning.promoId) },
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
                fee = warning.fee.format { crypto(symbol = "", decimals = warning.feeCurrencyDecimals) },
                feeCurrencySymbol = warning.feeCurrencySymbol,
                onAssociateClick = clickIntents::onAssociateClick,
            )
            is CryptoCurrencyWarning.RequiredTrustline -> RequiredTrustlineWarning(
                currency = warning.currency,
                amount = warning.requiredAmount.format { crypto(symbol = "", warning.currencyDecimals) },
                currencySymbol = warning.currencySymbol,
                onOpenClick = clickIntents::onOpenTrustlineClick,
            )
            is KaspaWarnings.IncompleteTransaction -> KaspaIncompleteTransactionWarning(
                currency = warning.currency,
                amount = warning.amount.format { crypto(symbol = "", decimals = warning.currencyDecimals) },
                currencySymbol = warning.currencySymbol,
                onRetryIncompleteTransactionClick = clickIntents::onRetryIncompleteTransactionClick,
                onDismissIncompleteTransactionClick = clickIntents::onDismissIncompleteTransactionClick,
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
            is CryptoCurrencyWarning.MigrationMaticToPol -> MigrationMaticToPol
            is CryptoCurrencyWarning.UsedOutdatedDataWarning -> UsedOutdatedData
        }
    }

    private fun formatMana(amount: BigDecimal): String {
        return amount.format { crypto("", Blockchain.Koinos.decimals()).shorted() }
    }

    // workaround for networks that users have misunderstanding
    private fun CryptoCurrency.shouldMergeFeeNetworkName(): Boolean {
        return Blockchain.fromNetworkId(this.network.backendId) == Blockchain.Arbitrum
    }
}