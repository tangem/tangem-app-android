package com.tangem.common.ui.notifications

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.R
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.shorted
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import java.math.BigDecimal

sealed class NotificationUM(val config: NotificationConfig) {

    open class Error(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_24,
        buttonState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM(
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

        data class MinimumSendAmountError(val amount: String) : Error(
            title = resourceReference(R.string.send_notification_invalid_amount_title),
            subtitle = resourceReference(
                R.string.transfer_notification_invalid_minimum_transaction_amount_text,
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

        data object TonStakingExtraFeeError : Error(
            title = resourceReference(R.string.staking_notification_ton_extra_reserve_title),
            subtitle = resourceReference(R.string.staking_notification_ton_extra_reserve_is_required),
        )

        data class TokenExceedsBalance(
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
                id = R.string.warning_blocked_funds_for_fee_title,
                wrappedList(feeName),
            ),
            subtitle = resourceReference(
                id = R.string.warning_blocked_funds_for_fee_message,
                formatArgs = wrappedList(currencyName),
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

        data class ReserveAmount(val amount: String) : Error(
            title = resourceReference(
                id = R.string.send_notification_invalid_reserve_amount_title,
                wrappedList(amount),
            ),
            subtitle = resourceReference(id = R.string.send_notification_invalid_reserve_amount_text),
        )

        data object DestinationTagRequired : Error(
            title = resourceReference(id = R.string.send_validation_destination_tag_required_title),
            subtitle = resourceReference(id = R.string.send_validation_destination_tag_required_description),
        )
    }

    open class Warning(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.img_attention_20,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
    ) : NotificationUM(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
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

        data class OnrampErrorNotification(val errorCode: String?, val onRefresh: () -> Unit) : Warning(
            title = resourceReference(R.string.common_error),
            subtitle = if (errorCode != null) {
                resourceReference(R.string.express_error_code, wrappedList(errorCode))
            } else {
                resourceReference(R.string.common_unknown_error)
            },
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.warning_button_refresh),
                onClick = onRefresh,
            ),
        )

        data object SwapNoAvailablePair : Warning(
            title = resourceReference(id = R.string.action_buttons_swap_no_available_pair_notification_title),
            subtitle = resourceReference(id = R.string.action_buttons_swap_no_available_pair_notification_message),
        )

        data object SellingRegionalRestriction : Warning(
            title = resourceReference(id = R.string.selling_regional_restriction_alert_title),
            subtitle = resourceReference(id = R.string.selling_regional_restriction_alert_message),
        )

        data object InsufficientBalanceForSelling : Warning(
            title = resourceReference(id = R.string.selling_insufficient_balance_alert_title),
            subtitle = resourceReference(id = R.string.selling_insufficient_balance_alert_message),
        )
    }

    open class Info(
        title: TextReference,
        subtitle: TextReference,
        iconResId: Int = R.drawable.ic_alert_circle_24,
        buttonsState: NotificationConfig.ButtonsState? = null,
        onCloseClick: (() -> Unit)? = null,
        iconTint: NotificationConfig.IconTint = NotificationConfig.IconTint.Unspecified,
    ) : NotificationUM(
        config = NotificationConfig(
            title = title,
            subtitle = subtitle,
            iconResId = iconResId,
            buttonsState = buttonsState,
            onCloseClick = onCloseClick,
            iconTint = iconTint,
        ),
    )

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

    sealed interface Sui {

        data object NotEnoughCoinForTokenTransaction : Error(
            title = resourceReference(id = R.string.sui_not_enough_coin_for_fee_title),
            subtitle = resourceReference(
                id = R.string.sui_not_enough_coin_for_fee_description,
                formatArgs = wrappedList(
                    BigDecimal.ONE.format {
                        crypto(Blockchain.Sui.currency, Blockchain.Sui.decimals())
                    },
                ),
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
                    mana.format { crypto("", Blockchain.Koinos.decimals()).shorted() },
                    maxMana.format { crypto("", Blockchain.Koinos.decimals()).shorted() },
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
                    availableKoinForTransfer.format {
                        crypto(Blockchain.Koinos.currency, Blockchain.Koinos.decimals())
                    },
                ),
            ),
            buttonState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.send_notification_reduce_to, wrappedList(availableKoinForTransfer)),
                onClick = onReduceClick,
            ),
        )
    }

    sealed interface Solana {

        data class RentInfo(
            private val rentInfo: CryptoCurrencyWarning.Rent,
        ) : Error(
            title = TextReference.Res(R.string.send_notification_invalid_amount_title),
            subtitle = TextReference.Res(
                id = R.string.send_notification_invalid_amount_rent_fee,
                formatArgs = wrappedList(rentInfo.exemptionAmount),
            ),
        )

        data class RentExemptionDestination(
            private val rentExemptionAmount: BigDecimal,
        ) : Error(
            title = TextReference.Res(R.string.send_notification_invalid_amount_title),
            subtitle = TextReference.Res(
                id = R.string.send_notification_invalid_amount_rent_destination,
                formatArgs = wrappedList(rentExemptionAmount),
            ),
        )
    }
}