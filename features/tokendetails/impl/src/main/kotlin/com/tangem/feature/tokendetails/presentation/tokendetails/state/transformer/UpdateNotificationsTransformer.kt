package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.DynamicAddressesWarnings
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.res.R as CoreResR

internal class UpdateNotificationsTransformer(
    private val warnings: Set<CryptoCurrencyWarning>,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val notifications = warnings.mapNotNull(::mapWarning).toImmutableList()
        return prevState.copy(notifications = notifications)
    }

    @Suppress("LongMethod")
    private fun mapWarning(warning: CryptoCurrencyWarning): TangemMessageUM? {
        return when (warning) {
            is CryptoCurrencyWarning.SomeNetworksUnreachable -> TangemMessageUM(
                id = "networks_unreachable",
                title = resourceReference(CoreResR.string.warning_network_unreachable_title),
                subtitle = resourceReference(CoreResR.string.warning_network_unreachable_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
            )
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> createFeeWarning(
                FeeWarningParams(
                    id = "balance_not_enough_for_fee",
                    currency = warning.tokenCurrency,
                    networkName = warning.coinCurrency.network.name,
                    feeCurrencyName = warning.coinCurrency.name,
                    feeCurrencySymbol = warning.coinCurrency.symbol,
                    buyCurrency = warning.coinCurrency,
                ),
            )
            is CryptoCurrencyWarning.CustomTokenNotEnoughForFee -> createFeeWarning(
                FeeWarningParams(
                    id = "custom_token_not_enough_for_fee",
                    currency = warning.currency,
                    networkName = warning.feeCurrency?.network?.name ?: warning.networkName,
                    feeCurrencyName = warning.feeCurrencyName,
                    feeCurrencySymbol = warning.feeCurrencySymbol,
                    buyCurrency = warning.feeCurrency,
                ),
            )
            is CryptoCurrencyWarning.BeaconChainShutdown -> TangemMessageUM(
                id = "beacon_chain_shutdown",
                title = resourceReference(CoreResR.string.warning_beacon_chain_retirement_title),
                subtitle = resourceReference(CoreResR.string.warning_beacon_chain_retirement_content),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
            )
            is HederaWarnings.AssociateWarning -> TangemMessageUM(
                id = "hedera_associate",
                title = resourceReference(CoreResR.string.warning_hedera_missing_token_association_title),
                subtitle = resourceReference(CoreResR.string.warning_hedera_missing_token_association_message_brief),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_hedera_missing_token_association_button_title),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onAssociateClick,
                    ),
                ),
            )
            is HederaWarnings.AssociateWarningWithFee -> TangemMessageUM(
                id = "hedera_associate_fee",
                title = resourceReference(CoreResR.string.warning_hedera_missing_token_association_title),
                subtitle = resourceReference(
                    CoreResR.string.warning_hedera_missing_token_association_message,
                    wrappedList(
                        warning.fee.format { crypto(symbol = "", decimals = warning.feeCurrencyDecimals) },
                        warning.feeCurrencySymbol,
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_hedera_missing_token_association_button_title),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onAssociateClick,
                    ),
                ),
            )
            is CryptoCurrencyWarning.RequiredTrustline -> TangemMessageUM(
                id = "required_trustline",
                title = resourceReference(CoreResR.string.warning_token_trustline_title),
                subtitle = resourceReference(
                    CoreResR.string.warning_token_trustline_subtitle,
                    wrappedList(
                        warning.requiredAmount.format { crypto(symbol = "", warning.currencyDecimals) }.trim(),
                        warning.currencySymbol,
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_token_trustline_button_title),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onOpenTrustlineClick,
                    ),
                ),
            )
            is KaspaWarnings.IncompleteTransaction -> TangemMessageUM(
                id = "kaspa_incomplete",
                title = resourceReference(CoreResR.string.warning_kaspa_unfinished_token_transaction_title),
                subtitle = resourceReference(
                    CoreResR.string.warning_kaspa_unfinished_token_transaction_message,
                    wrappedList(
                        warning.amount.format { crypto(symbol = "", decimals = warning.currencyDecimals) },
                        warning.currencySymbol,
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.alert_button_try_again),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onRetryIncompleteTransactionClick,
                    ),
                ),
                onCloseClick = clickIntents::onDismissIncompleteTransactionClick,
            )
            is CryptoCurrencyWarning.MigrationMaticToPol -> TangemMessageUM(
                id = "migration_matic_pol",
                title = resourceReference(CoreResR.string.warning_matic_migration_title),
                subtitle = resourceReference(CoreResR.string.warning_matic_migration_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
            )
            is CryptoCurrencyWarning.MigrationClore -> TangemMessageUM(
                id = "migration_clore",
                title = resourceReference(CoreResR.string.warning_clore_migration_title),
                subtitle = resourceReference(CoreResR.string.warning_clore_migration_description),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_clore_migration_button),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onCloreMigrationClick,
                    ),
                ),
            )
            is DynamicAddressesWarnings.FundsFound -> TangemMessageUM(
                id = "dynamic_addresses_funds_found",
                title = resourceReference(CoreResR.string.dynamic_addresses_notification_funds_found_title),
                subtitle = resourceReference(CoreResR.string.dynamic_addresses_notification_funds_found_description),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.common_learn_more),
                        type = TangemButtonType.Primary,
                        onClick = clickIntents::onDynamicAddressesFundsFoundLearnMoreClick,
                    ),
                ),
            )
            // Non-warning types — skip for redesign
            is CryptoCurrencyWarning.ExistentialDeposit,
            is CryptoCurrencyWarning.Rent,
            is CryptoCurrencyWarning.SomeNetworksNoAccount,
            is CryptoCurrencyWarning.TopUpWithoutReserve,
            is CryptoCurrencyWarning.FeeResourceInfo,
            is CryptoCurrencyWarning.UsedOutdatedDataWarning,
            -> null
        }
    }

    private fun createFeeWarning(params: FeeWarningParams): TangemMessageUM {
        val buttons = if (params.buyCurrency != null) {
            persistentListOf(
                TangemMessageButtonUM(
                    text = resourceReference(
                        CoreResR.string.common_buy_currency,
                        wrappedList(params.feeCurrencySymbol),
                    ),
                    type = TangemButtonType.Primary,
                    onClick = { clickIntents.onBuyCoinClick(params.buyCurrency) },
                ),
            )
        } else {
            persistentListOf()
        }

        return TangemMessageUM(
            id = params.id,
            title = resourceReference(
                CoreResR.string.warning_send_blocked_funds_for_fee_title,
                wrappedList(params.feeCurrencyName),
            ),
            subtitle = resourceReference(
                CoreResR.string.warning_send_blocked_funds_for_fee_message,
                wrappedList(
                    params.currency.name,
                    params.networkName,
                    params.currency.name,
                    params.feeCurrencyName,
                    params.feeCurrencySymbol,
                ),
            ),
            messageEffect = TangemMessageEffect.None,
            iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
            buttonsUM = buttons,
        )
    }

    private data class FeeWarningParams(
        val id: String,
        val currency: CryptoCurrency,
        val networkName: String,
        val feeCurrencyName: String,
        val feeCurrencySymbol: String,
        val buyCurrency: CryptoCurrency?,
    )
}