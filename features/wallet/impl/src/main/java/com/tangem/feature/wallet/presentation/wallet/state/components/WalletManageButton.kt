package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R

/**
 * Wallet manage button state
 *
 * @property config action config
 *
[REDACTED_AUTHOR]
 */
@Immutable
internal sealed class WalletManageButton(val config: ActionButtonConfig) {

    /** Is click enabled */
    abstract val enabled: Boolean

    /** Lambda be invoked when manage button is clicked */
    abstract val onClick: () -> Unit

    /**
     * Buy
     *
     * @property enabled button click availability
     * @property onClick lambda be invoked when Buy button is clicked
     */
    data class Buy(override val enabled: Boolean, override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_buy),
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick,
            enabled = enabled,
        ),
    )

    /**
     * Send
     *
     * @property enabled button click availability
     * @property onClick lambda be invoked when Send button is clicked
     */
    data class Send(override val enabled: Boolean, override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_send),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick,
            enabled = enabled,
        ),
    )

    /**
     * Receive
     *
     * @property onClick lambda be invoked when Receive button is clicked
     */
    data class Receive(override val enabled: Boolean, override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_receive),
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = onClick,
            enabled = enabled,
        ),
    )

    /**
     * Sell
     *
     * @property enabled button click availability
     * @property onClick lambda be invoked when Sell button is clicked
     */
    data class Sell(override val enabled: Boolean, override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_sell),
            iconResId = R.drawable.ic_currency_24,
            onClick = onClick,
            enabled = enabled,
        ),
    )

    /**
     * Swap
     *
     * @property enabled button click availability
     * @property onClick lambda be invoked when Swap button is clicked
     */
    data class Swap(override val enabled: Boolean, override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_swap),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick,
            enabled = enabled,
        ),
    )
}