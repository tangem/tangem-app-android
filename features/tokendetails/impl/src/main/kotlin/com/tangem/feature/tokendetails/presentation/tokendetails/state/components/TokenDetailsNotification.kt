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
internal sealed class TokenDetailsNotification {

    abstract val config: NotificationConfig

    sealed class Informational : TokenDetailsNotification()
    sealed class Warning : TokenDetailsNotification()

    data class RentInfo(
        private val rentInfo: CryptoCurrencyWarning.Rent,
        private val onCloseClick: () -> Unit,
    ) : Warning() {
        override val config = NotificationConfig(
            title = TextReference.Res(R.string.warning_rent_fee_title),
            subtitle = TextReference.Res(
                id = R.string.warning_solana_rent_fee_message,
                formatArgs = wrappedList(rentInfo.rent, rentInfo.exemptionAmount),
            ),
            iconResId = R.drawable.img_attention_20,
            onCloseClick = onCloseClick,
        )
    }

    data class ExistentialDeposit(
        private val existentialInfo: CryptoCurrencyWarning.ExistentialDeposit,
    ) : Informational() {
        override val config = NotificationConfig(
            title = resourceReference(R.string.warning_existential_deposit_title),
            subtitle = TextReference.Res(
                id = R.string.warning_existential_deposit_message,
                formatArgs = wrappedList(existentialInfo.currencyName, existentialInfo.edStringValueWithSymbol),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
        )
    }

    data class NetworkFee(
        private val feeInfo: CryptoCurrencyWarning.BalanceNotEnoughForFee,
        private val onBuyClick: () -> Unit,
    ) : Warning() {
        override val config = NotificationConfig(
            title = TextReference.Res(
                id = R.string.warning_send_blocked_funds_for_fee_title,
                formatArgs = wrappedList(feeInfo.blockchainFullName),
            ),
            subtitle = TextReference.Res(
                id = R.string.warning_send_blocked_funds_for_fee_message,
                formatArgs = wrappedList(
                    feeInfo.currency.name,
                    feeInfo.blockchainFullName,
                    feeInfo.currency.name,
                    feeInfo.blockchainFullName,
                    feeInfo.blockchainSymbol,
                ),
            ),
            iconResId = feeInfo.currency.networkIconResId,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(
                    id = R.string.common_buy_currency,
                    formatArgs = wrappedList(feeInfo.blockchainSymbol),
                ),
                onClick = onBuyClick,
            ),
        )
    }

    object NetworksUnreachable : Warning() {
        override val config = NotificationConfig(
            title = resourceReference(R.string.warning_network_unreachable_title),
            subtitle = resourceReference(R.string.warning_network_unreachable_message),
            iconResId = R.drawable.img_attention_20,
        )
    }

    class NetworksNoAccount(val network: String, val symbol: String, val amount: String) : Informational() {
        override val config = NotificationConfig(
            title = resourceReference(R.string.warning_no_account_title),
            subtitle = resourceReference(
                R.string.no_account_generic,
                wrappedList(network, amount, symbol),
            ),
            iconResId = R.drawable.ic_alert_circle_24,
        )
    }
}