package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.features.tokendetails.impl.R
import org.joda.time.DateTime

@Immutable
internal sealed class TokenDetailsNotification(val config: NotificationConfig) {

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.img_attention_20,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonsState,
            onCloseClick = onCloseClick,
        ),
    )

    sealed class Informational(
        title: TextReference,
        subtitle: TextReference,
        onCloseClick: (() -> Unit)? = null,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.ic_alert_circle_24,
            onCloseClick = onCloseClick,
        ),
    )

    data class SwapPromo(
        val startDateTime: DateTime,
        val endDateTime: DateTime,
        val onSwapClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.swap_promo_title),
            subtitle = resourceReference(id = R.string.swap_promo_text),
            iconResId = R.drawable.img_okx_dex_logo,
            onCloseClick = onCloseClick,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(id = com.tangem.core.ui.R.string.token_swap_promotion_button),
                onClick = onSwapClick,
            ),
        ),
    )

    data object NetworksUnreachable : Warning(
        title = resourceReference(R.string.warning_network_unreachable_title),
        subtitle = resourceReference(R.string.warning_network_unreachable_message),
    )

    data class NetworkFee(
        private val currency: CryptoCurrency,
        private val networkName: String,
        private val feeCurrencyName: String,
        private val feeCurrencySymbol: String,
    ) : Warning(
        title = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_title,
            formatArgs = wrappedList(feeCurrencyName),
        ),
        subtitle = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_message,
            formatArgs = wrappedList(
                currency.name,
                networkName,
                currency.name,
                feeCurrencyName,
                feeCurrencySymbol,
            ),
        ),
        iconResId = currency.networkIconResId,
    )

    data class NetworkFeeWithBuyButton(
        private val currency: CryptoCurrency,
        private val networkName: String,
        private val feeCurrencyName: String,
        private val feeCurrencySymbol: String,
        val mergeFeeNetworkName: Boolean = false,
        private val onBuyClick: () -> Unit,
    ) : Warning(
        title = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_title,
            formatArgs = wrappedList(feeCurrencyName),
        ),
        subtitle = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_message,
            formatArgs = wrappedList(
                currency.name,
                networkName,
                currency.name,
                feeCurrencyName,
                feeCurrencySymbol,
            ),
        ),
        iconResId = currency.networkIconResId,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(
                id = R.string.common_buy_currency,
                formatArgs = wrappedList(
                    if (mergeFeeNetworkName) {
                        "$feeCurrencyName ($feeCurrencySymbol)"
                    } else {
                        feeCurrencySymbol
                    },
                ),
            ),
            onClick = onBuyClick,
        ),
    )

    data class RentInfo(
        private val rentInfo: CryptoCurrencyWarning.Rent,
        private val onCloseClick: () -> Unit,
    ) : Informational(
        title = TextReference.Res(R.string.warning_rent_fee_title),
        subtitle = TextReference.Res(
            id = R.string.warning_solana_rent_fee_message,
            formatArgs = wrappedList(rentInfo.rent, rentInfo.exemptionAmount),
        ),
        onCloseClick = onCloseClick,
    )

    data class ExistentialDeposit(
        private val existentialInfo: CryptoCurrencyWarning.ExistentialDeposit,
    ) : Informational(
        title = resourceReference(R.string.warning_existential_deposit_title),
        subtitle = TextReference.Res(
            id = R.string.warning_existential_deposit_message,
            formatArgs = wrappedList(existentialInfo.currencyName, existentialInfo.edStringValueWithSymbol),
        ),
    )

    data class NetworksNoAccount(
        private val network: String,
        private val symbol: String,
        private val amount: String,
    ) : Informational(
        title = resourceReference(R.string.warning_no_account_title),
        subtitle = resourceReference(
            id = R.string.no_account_generic,
            formatArgs = wrappedList(network, amount, symbol),
        ),
    )

    data object TopUpWithoutReserve : Informational(
        title = resourceReference(id = R.string.warning_no_account_title),
        subtitle = resourceReference(id = R.string.no_account_send_to_create),
    )

    data class NetworkShutdown(private val title: TextReference, private val subtitle: TextReference) : Warning(
        title = title,
        subtitle = subtitle,
    )

    data class HederaAssociateWarning(
        private val currency: CryptoCurrency,
        private val fee: String?,
        private val feeCurrencySymbol: String?,
        private val onAssociateClick: () -> Unit,
    ) : Warning(
        title = resourceReference(R.string.warning_hedera_missing_token_association_title),
        subtitle = if (fee != null && feeCurrencySymbol != null) {
            resourceReference(
                id = R.string.warning_hedera_missing_token_association_message,
                formatArgs = wrappedList(fee, feeCurrencySymbol),
            )
        } else {
            resourceReference(R.string.warning_hedera_missing_token_association_message_brief)
        },
        iconResId = currency.networkIconResId,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.warning_hedera_missing_token_association_button_title),
            onClick = onAssociateClick,
        ),
    )

    data class RequiredTrustlineWarning(
        private val currency: CryptoCurrency,
        private val amount: String,
        private val currencySymbol: String,
        private val onOpenClick: () -> Unit,
    ) : Warning(
        title = resourceReference(id = R.string.warning_token_trustline_title),
        subtitle = resourceReference(
            id = R.string.warning_token_trustline_subtitle,
            formatArgs = wrappedList(amount, currencySymbol),
        ),
        iconResId = currency.networkIconResId,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.warning_token_trustline_button_title),
            onClick = onOpenClick,
        ),
    )

    data class KaspaIncompleteTransactionWarning(
        private val userWallet: UserWallet,
        private val currency: CryptoCurrency,
        private val amount: String,
        private val currencySymbol: String,
        private val onRetryIncompleteTransactionClick: () -> Unit,
        private val onDismissIncompleteTransactionClick: () -> Unit,
    ) : Warning(
        title = resourceReference(R.string.warning_kaspa_unfinished_token_transaction_title),
        subtitle = resourceReference(
            id = R.string.warning_kaspa_unfinished_token_transaction_message,
            formatArgs = wrappedList(amount, currencySymbol),
        ),
        iconResId = R.drawable.ic_alert_circle_red_20,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.alert_button_try_again),
            onClick = onRetryIncompleteTransactionClick,
            iconResId = walletInterationIcon(userWallet),
        ),
        onCloseClick = onDismissIncompleteTransactionClick,
    )

    data class KoinosMana(
        val manaBalanceAmount: String,
        val maxManaBalanceAmount: String,
    ) : Informational(
        title = resourceReference(id = R.string.koinos_mana_level_title),
        subtitle = resourceReference(
            id = R.string.koinos_mana_level_description,
            formatArgs = wrappedList(manaBalanceAmount, maxManaBalanceAmount),
        ),
    )

    data object MigrationMaticToPol : Warning(
        title = resourceReference(id = R.string.warning_matic_migration_title),
        subtitle = resourceReference(id = R.string.warning_matic_migration_message),
    )

    data object UsedOutdatedData : TokenDetailsNotification(
        config = NotificationConfig(
            subtitle = resourceReference(R.string.warning_some_token_balances_not_updated),
            iconResId = R.drawable.ic_error_sync_24,
        ),
    )
}