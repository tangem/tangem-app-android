package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import java.math.BigDecimal

internal sealed class SendNotification(val config: NotificationConfig) {

    sealed class Error(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_24,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonState,
            onCloseClick = onCloseClick,
        ),
    ) {

        data object TotalExceedsBalance : Error(
            title = resourceReference(R.string.send_notification_exceed_balance_title),
            subtitle = resourceReference(R.string.send_notification_exceed_balance_text),
        )

        data object InvalidAmount : Error(
            title = resourceReference(R.string.send_notification_invalid_amount_title),
            subtitle = resourceReference(R.string.send_notification_invalid_amount_text),
        )

        data class MinimumAmountError(val amount: String) : Error(
            title = resourceReference(R.string.send_notification_invalid_amount_title),
            subtitle = resourceReference(
                R.string.send_notification_invalid_minimum_amount_text,
                wrappedList(amount, amount),
            ),
        )

        data class TransactionLimitError(
            val cryptoCurrency: String,
            val utxoLimit: String,
            val amountLimit: String,
            val onConfirmClick: () -> Unit,
        ) : Error(
            title = resourceReference(R.string.send_notification_transaction_limit_title),
            subtitle = resourceReference(
                R.string.send_notification_transaction_limit_text,
                wrappedList(cryptoCurrency, utxoLimit, amountLimit),
            ),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_leave_button, wrappedList(amountLimit)),
                onClick = onConfirmClick,
            ),
        )

        data class ExceedsBalance(
            val networkIconId: Int,
            val currencyName: String,
            val feeName: String,
            val feeSymbol: String,
            val networkName: String,
            val mergeFeeNetworkName: Boolean = false,
            val onClick: (() -> Unit)? = null,
        ) : Error(
            title = resourceReference(
                id = R.string.warning_send_blocked_funds_for_fee_title,
                wrappedList(feeName),
            ),
            subtitle = resourceReference(
                id = R.string.warning_send_blocked_funds_for_fee_message,
                formatArgs = wrappedList(currencyName, networkName, currencyName, feeName, feeSymbol),
            ),
            iconResId = networkIconId,
            buttonState = onClick?.let {
                NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(
                        R.string.common_buy_currency,
                        wrappedList(
                            if (mergeFeeNetworkName) {
                                "$currencyName ($feeSymbol)"
                            } else {
                                feeName
                            },
                        ),
                    ),
                    onClick = onClick,
                )
            },
        )

        data class ExistentialDeposit(val deposit: String, val onConfirmClick: () -> Unit) : Error(
            title = resourceReference(R.string.send_notification_existential_deposit_title),
            subtitle = resourceReference(R.string.send_notification_existential_deposit_text, wrappedList(deposit)),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_leave_button, wrappedList(deposit)),
                onClick = onConfirmClick,
            ),
        )
    }

    sealed class Warning(
        title: TextReference,
        subtitle: TextReference,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : SendNotification(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = R.drawable.img_attention_20,
            buttonsState = buttonsState,
            onCloseClick = onCloseClick,
        ),
    ) {
        data class HighFeeError(
            val currencyName: String,
            val amount: String,
            val onConfirmClick: () -> Unit,
            val onCloseClick: () -> Unit,
        ) : Warning(
            title = resourceReference(R.string.send_notification_high_fee_title),
            subtitle = resourceReference(R.string.send_notification_high_fee_text, wrappedList(currencyName, amount)),
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_reduce_by, wrappedList(amount)),
                onClick = onConfirmClick,
            ),
            onCloseClick = onCloseClick,
        )

        data object FeeTooLow : Warning(
            title = resourceReference(id = R.string.send_notification_transaction_delay_title),
            subtitle = resourceReference(id = R.string.send_notification_transaction_delay_text),
        )

        data class TooHigh(
            val value: String,
        ) : Warning(
            title = resourceReference(id = R.string.send_notification_fee_too_high_title),
            subtitle = resourceReference(id = R.string.send_notification_fee_too_high_text, wrappedList(value)),
        )

        data class NetworkFeeUnreachable(val onRefresh: () -> Unit) : Warning(
            title = resourceReference(R.string.send_fee_unreachable_error_title),
            subtitle = resourceReference(R.string.send_fee_unreachable_error_text),
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onRefresh,
            ),
        )

        data class TronAccountNotActivated(val tokenName: String) : Warning(
            title = resourceReference(R.string.send_fee_unreachable_error_title),
            subtitle = resourceReference(
                R.string.send_tron_account_activation_error,
                wrappedList(tokenName),
            ),
        )

        data class FeeCoverageNotification(val cryptoAmount: String, val fiatAmount: String) : Warning(
            title = resourceReference(R.string.send_network_fee_warning_title),
            subtitle = resourceReference(
                R.string.common_network_fee_warning_content,
                wrappedList(cryptoAmount, fiatAmount),
            ),
        )
    }

    sealed interface Cardano {

        data class MinAdaValueCharged(val tokenName: String, val minAdaValue: String) : Warning(
            title = resourceReference(id = R.string.cardano_coin_will_be_send_with_token_title),
            subtitle = resourceReference(
                id = R.string.cardano_coin_will_be_send_with_token_description,
                formatArgs = wrappedList(minAdaValue, tokenName),
            ),
        )

        data object InsufficientBalanceToTransferCoin : Error(
            title = resourceReference(id = R.string.cardano_max_amount_has_token_title),
            subtitle = resourceReference(id = R.string.cardano_max_amount_has_token_description),
        )

        data class InsufficientBalanceToTransferToken(val tokenName: String) : Error(
            title = resourceReference(id = R.string.cardano_insufficient_balance_to_send_token_title),
            subtitle = resourceReference(
                id = R.string.cardano_insufficient_balance_to_send_token_description,
                formatArgs = wrappedList(tokenName),
            ),
        )
    }

    sealed interface Koinos {
        data class InsufficientRecoverableMana(
            val mana: BigDecimal,
            val maxMana: BigDecimal,
        ) : Error(
            title = resourceReference(R.string.koinos_insufficient_mana_to_send_koin_title),
            subtitle = resourceReference(
                R.string.koinos_insufficient_mana_to_send_koin_description,
                formatArgs = wrappedList(
                    BigDecimalFormatter.formatCryptoAmountShorted(mana, "", Blockchain.Koinos.decimals()),
                    BigDecimalFormatter.formatCryptoAmountShorted(maxMana, "", Blockchain.Koinos.decimals()),
                ),
            ),
        )

        data object InsufficientBalance : Error(
            title = resourceReference(R.string.koinos_insufficient_balance_to_send_koin_title),
            subtitle = resourceReference(R.string.koinos_insufficient_balance_to_send_koin_description),
        )

        data class ManaExceedsBalance(
            val availableKoinForTransfer: BigDecimal,
            val onReduceClick: () -> Unit,
        ) : Error(
            title = resourceReference(R.string.koinos_mana_exceeds_koin_balance_title),
            subtitle = resourceReference(
                R.string.koinos_mana_exceeds_koin_balance_description,
                formatArgs = wrappedList(
                    BigDecimalFormatter.formatCryptoAmount(
                        availableKoinForTransfer,
                        Blockchain.Koinos.currency,
                        Blockchain.Koinos.decimals(),
                    ),
                ),
            ),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_reduce_to, wrappedList(availableKoinForTransfer)),
                onClick = onReduceClick,
            ),
        )
    }
}
