package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.blockchain.common.Blockchain
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
import com.tangem.core.ui.format.bigdecimal.shorted
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.DynamicAddressesWarnings
import com.tangem.domain.tokens.model.warnings.HederaWarnings
import com.tangem.domain.tokens.model.warnings.KaspaWarnings
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import com.tangem.core.res.R as CoreResR

@Suppress("LargeClass")
internal class UpdateNotificationsTransformer(
    private val warnings: Set<CryptoCurrencyWarning>,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val notifications = warnings.map(::mapWarning).toImmutableList()
        return prevState.copy(notifications = notifications)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun mapWarning(warning: CryptoCurrencyWarning): TangemMessageUM {
        return when (warning) {
            is CryptoCurrencyWarning.SomeNetworksUnreachable -> TangemMessageUM(
                id = "networks_unreachable",
                title = resourceReference(CoreResR.string.warning_network_unreachable_title),
                subtitle = resourceReference(CoreResR.string.warning_network_unreachable_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.status.attention },
                ),
            )
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> createFeeWarning(
                FeeWarningParams(
                    id = "balance_not_enough_for_fee",
                    currency = warning.tokenCurrency,
                    networkName = warning.coinCurrency.network.name,
                    feeCurrencyName = warning.coinCurrency.name,
                    feeCurrencySymbol = warning.coinCurrency.symbol,
                    buyCurrency = warning.coinCurrency,
                    iconResId = R.drawable.ic_attention_default_24,
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
                    iconResId = R.drawable.ic_attention_default_24,
                ),
            )
            is CryptoCurrencyWarning.BeaconChainShutdown -> TangemMessageUM(
                id = "beacon_chain_shutdown",
                title = resourceReference(CoreResR.string.warning_beacon_chain_retirement_title),
                subtitle = resourceReference(CoreResR.string.warning_beacon_chain_retirement_content),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.status.attention },
                ),
            )
            is HederaWarnings.AssociateWarning -> TangemMessageUM(
                id = "hedera_associate",
                title = resourceReference(CoreResR.string.warning_hedera_missing_token_association_title),
                subtitle = resourceReference(CoreResR.string.warning_hedera_missing_token_association_message_brief),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_hedera_missing_token_association_button_title),
                        type = TangemButtonType.PrimaryInverse,
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
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_hedera_missing_token_association_button_title),
                        type = TangemButtonType.PrimaryInverse,
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
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_token_trustline_button_title),
                        type = TangemButtonType.PrimaryInverse,
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
                messageEffect = TangemMessageEffect.Warning,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.common_cancel),
                        type = TangemButtonType.Secondary,
                        onClick = clickIntents::onDismissIncompleteTransactionClick,
                    ),
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.alert_button_try_again),
                        type = TangemButtonType.Primary,
                        tangemIconUM = TangemIconUM.Icon(
                            R.drawable.ic_tangem_24,
                            tintReference = { TangemTheme.colors2.graphic.neutral.primaryInverted },
                        ),
                        onClick = clickIntents::onRetryIncompleteTransactionClick,
                    ),
                ),
            )
            is CryptoCurrencyWarning.MigrationMaticToPol -> TangemMessageUM(
                id = "migration_matic_pol",
                title = resourceReference(CoreResR.string.warning_matic_migration_title),
                subtitle = resourceReference(CoreResR.string.warning_matic_migration_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.status.attention },
                ),
            )
            is CryptoCurrencyWarning.MigrationClore -> TangemMessageUM(
                id = "migration_clore",
                title = resourceReference(CoreResR.string.warning_clore_migration_title),
                subtitle = resourceReference(CoreResR.string.warning_clore_migration_description),
                messageEffect = TangemMessageEffect.Warning,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.warning_clore_migration_button),
                        type = TangemButtonType.PrimaryInverse,
                        onClick = clickIntents::onCloreMigrationClick,
                    ),
                ),
            )
            is DynamicAddressesWarnings.FundsFound -> TangemMessageUM(
                id = "dynamic_addresses_funds_found",
                title = resourceReference(CoreResR.string.dynamic_addresses_notification_funds_found_title),
                subtitle = resourceReference(CoreResR.string.dynamic_addresses_notification_funds_found_description),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_attention_default_24,
                    tintReference = { TangemTheme.colors2.graphic.status.attention },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.common_learn_more),
                        type = TangemButtonType.PrimaryInverse,
                        onClick = clickIntents::onDynamicAddressesFundsFoundLearnMoreClick,
                    ),
                ),
            )
            is CryptoCurrencyWarning.ExistentialDeposit -> TangemMessageUM(
                id = "existential_deposit",
                title = resourceReference(CoreResR.string.warning_existential_deposit_title),
                subtitle = resourceReference(
                    CoreResR.string.warning_existential_deposit_message,
                    wrappedList(warning.currencyName, warning.edStringValueWithSymbol),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_warning_default_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
            )
            is CryptoCurrencyWarning.Rent -> TangemMessageUM(
                id = "rent_info",
                title = resourceReference(CoreResR.string.warning_rent_fee_title),
                subtitle = resourceReference(
                    CoreResR.string.warning_solana_rent_fee_message,
                    wrappedList(
                        warning.rent,
                        warning.exemptionAmount.format { crypto(warning.cryptoCurrency) },
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_warning_default_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
                buttonsUM = persistentListOf(
                    TangemMessageButtonUM(
                        text = resourceReference(CoreResR.string.common_later),
                        type = TangemButtonType.Secondary,
                        onClick = clickIntents::onCloseRentInfoNotification,
                    ),
                ),
            )
            is CryptoCurrencyWarning.SomeNetworksNoAccount -> TangemMessageUM(
                id = "networks_no_account",
                title = resourceReference(CoreResR.string.warning_no_account_title),
                subtitle = resourceReference(
                    CoreResR.string.no_account_generic,
                    wrappedList(
                        warning.amountCurrency.network.name,
                        warning.amountToCreateAccount.format {
                            crypto(symbol = "", decimals = warning.amountCurrency.decimals)
                        }.trim(),
                        warning.amountCurrency.network.currencySymbol,
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_warning_default_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
            )
            is CryptoCurrencyWarning.TopUpWithoutReserve -> TangemMessageUM(
                id = "top_up_without_reserve",
                title = resourceReference(CoreResR.string.warning_no_account_title),
                subtitle = resourceReference(CoreResR.string.no_account_send_to_create),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_warning_default_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
            )
            is CryptoCurrencyWarning.FeeResourceInfo -> TangemMessageUM(
                id = "fee_resource_info",
                title = resourceReference(CoreResR.string.koinos_mana_level_title),
                subtitle = resourceReference(
                    CoreResR.string.koinos_mana_level_description,
                    wrappedList(
                        formatMana(warning.amount),
                        warning.maxAmount?.let(::formatMana) ?: run {
                            TangemLogger.e(
                                "FeeResource maxAmount cannot be null in Koinos. Check KoinosWalletManager",
                            )
                            ""
                        },
                    ),
                ),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_warning_default_32,
                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                ),
            )
            is CryptoCurrencyWarning.UsedOutdatedDataWarning -> TangemMessageUM(
                id = "used_outdated_data",
                title = resourceReference(CoreResR.string.warning_outdated_data_title),
                subtitle = resourceReference(CoreResR.string.warning_outdated_data_message),
                messageEffect = TangemMessageEffect.None,
                iconUM = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_error_sync_default_32,
                    tintReference = { TangemTheme.colors2.graphic.status.attention },
                ),
            )
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
                    type = TangemButtonType.PrimaryInverse,
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
            iconUM = TangemIconUM.Icon(
                iconRes = params.iconResId,
                tintReference = { TangemTheme.colors2.graphic.neutral.primary },
            ),
            buttonsUM = buttons,
        )
    }

    private fun formatMana(amount: BigDecimal): String {
        return amount.format { crypto(symbol = "", decimals = Blockchain.Koinos.decimals()).shorted() }
    }
}

private data class FeeWarningParams(
    val id: String,
    val currency: CryptoCurrency,
    val networkName: String,
    val feeCurrencyName: String,
    val feeCurrencySymbol: String,
    val buyCurrency: CryptoCurrency?,
    val iconResId: Int,
)