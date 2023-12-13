package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.features.tokendetails.impl.R

@Immutable
internal sealed class TokenDetailsNotification(val config: NotificationConfig) {

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.img_attention_20,
        buttonsState: NotificationConfig.ButtonsState? = null,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonsState,
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
        val onSwapClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = resourceReference(id = R.string.token_swap_promotion_title),
            subtitle = resourceReference(id = R.string.token_swap_promotion_message),
            iconResId = R.drawable.ic_swap_promo_34,
            backgroundResId = R.drawable.img_swap_promo_banner_background,
            onCloseClick = onCloseClick,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(id = com.tangem.core.ui.R.string.token_swap_promotion_button),
                onClick = onSwapClick,
            ),
        ),
    )

    object NetworksUnreachable : Warning(
        title = resourceReference(R.string.warning_network_unreachable_title),
        subtitle = resourceReference(R.string.warning_network_unreachable_message),
    )

    data class NetworkFee(
        private val feeInfo: CryptoCurrencyWarning.BalanceNotEnoughForFee,
        private val onBuyClick: () -> Unit,
    ) : Warning(
        title = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_title,
            formatArgs = wrappedList(feeInfo.coinCurrency.name),
        ),
        subtitle = TextReference.Res(
            id = R.string.warning_send_blocked_funds_for_fee_message,
            formatArgs = wrappedList(
                feeInfo.tokenCurrency.name,
                feeInfo.coinCurrency.name,
                feeInfo.tokenCurrency.name,
                feeInfo.coinCurrency.name,
                feeInfo.coinCurrency.symbol,
            ),
        ),
        iconResId = feeInfo.tokenCurrency.networkIconResId,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(
                id = R.string.common_buy_currency,
                formatArgs = wrappedList(feeInfo.coinCurrency.symbol),
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

    class NetworksNoAccount(val network: String, val symbol: String, val amount: String) : Informational(
        title = resourceReference(R.string.warning_no_account_title),
        subtitle = resourceReference(
            id = R.string.no_account_generic,
            formatArgs = wrappedList(network, amount, symbol),
        ),
    )

    class HasPendingTransactions(val coinSymbol: String) : Informational(
        title = resourceReference(R.string.warning_send_blocked_pending_transactions_title),
        subtitle = resourceReference(
            id = R.string.warning_send_blocked_pending_transactions_message,
            formatArgs = wrappedList(coinSymbol),
        ),
    )
}
