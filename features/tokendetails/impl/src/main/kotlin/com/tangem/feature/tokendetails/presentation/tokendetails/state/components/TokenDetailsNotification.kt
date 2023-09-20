package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.tokens.models.warnings.CryptoCurrencyWarning
import com.tangem.features.tokendetails.impl.R

// TODO: Finalize notification strings [REDACTED_JIRA]
@Immutable
sealed class TokenDetailsNotification(open val config: NotificationConfig) {

    data class RentInfo(
        private val rentInfo: CryptoCurrencyWarning.Rent,
        private val onCloseClick: () -> Unit,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = TextReference.Res(R.string.send_network_fee_title),
            subtitle = TextReference.Res(
                id = R.string.solana_rent_warning,
                formatArgs = wrappedList(rentInfo.rent, rentInfo.exemptionAmount),
            ),
            iconResId = R.drawable.img_attention_20,
            onCloseClick = onCloseClick,
        ),
    )

    data class ExistentialDeposit(
        private val existentialInfo: CryptoCurrencyWarning.ExistentialDeposit,
        private val onCloseClick: () -> Unit,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = TextReference.Str("Existential Deposit"),
            subtitle = TextReference.Res(
                id = R.string.warning_existential_deposit_message,
                formatArgs = wrappedList(existentialInfo.currencyName, existentialInfo.edStringValueWithSymbol),
            ),
            iconResId = R.drawable.img_attention_20,
            onCloseClick = onCloseClick,
        ),
    )

    data class NetworkFee(
        private val feeInfo: CryptoCurrencyWarning.BalanceNotEnoughForFee,
        private val onBuyClick: () -> Unit,
    ) : TokenDetailsNotification(
        config = NotificationConfig(
            title = TextReference.Res(
                id = R.string.notification_title_not_enough_funds,
                formatArgs = wrappedList(feeInfo.blockchainFullName),
            ),
            subtitle = TextReference.Res(
                id = R.string.token_details_send_blocked_fee_format,
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
                text = TextReference.Res(R.string.common_buy),
                onClick = onBuyClick,
            ),
        ),
    )

    object NetworksUnreachable : TokenDetailsNotification(
        config = NotificationConfig(
            title = TextReference.Str("Some networks are unreachable"),
            subtitle = TextReference.Str("The problem is on the crypto-network side. It will be fixed soon."),
            iconResId = R.drawable.img_attention_20,
        ),
    )
}