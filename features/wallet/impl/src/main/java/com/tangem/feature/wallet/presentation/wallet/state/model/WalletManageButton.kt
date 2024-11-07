package com.tangem.feature.wallet.presentation.wallet.state.model

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

    /** Whether to dim content */
    abstract val dimContent: Boolean

    /** Lambda be invoked when manage button is clicked */
    abstract val onClick: () -> Unit

    /** Lambda be invoked when manage button is long clicked */
    open val onLongClick: (() -> TextReference?)? = null

    /**
     * Buy
     *
     * @property enabled button click availability
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Buy button is clicked
     */
    data class Buy(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
        val isInProgress: Boolean = false,
    ) :
        WalletManageButton(
            config = ActionButtonConfig(
                text = TextReference.Res(id = R.string.common_buy),
                iconResId = R.drawable.ic_plus_24,
                onClick = onClick,
                enabled = enabled,
                dimContent = dimContent,
                isInProgress = isInProgress,
            ),
        )

    /**
     * Send
     *
     * @property enabled button click availability
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Send button is clicked
     */
    data class Send(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
    ) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_send),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick,
            enabled = enabled,
            dimContent = dimContent,
        ),
    )

    /**
     * Receive
     *
     * @property onClick lambda be invoked when Receive button is clicked
     * @property onLongClick lambda be invoked when Receive button is long clicked
     */
    data class Receive(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
        override val onLongClick: (() -> TextReference?)?,
    ) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_receive),
            iconResId = R.drawable.ic_arrow_down_24,
            enabled = enabled,
            dimContent = dimContent,
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    )

    data class Stake(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
    ) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_stake),
            iconResId = R.drawable.ic_staking_24,
            onClick = onClick,
            enabled = enabled,
            dimContent = dimContent,
        ),
    )

    /**
     * Sell
     *
     * @property enabled button click availability
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Sell button is clicked
     */
    data class Sell(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
        val isInProgress: Boolean = false,
    ) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_sell),
            iconResId = R.drawable.ic_currency_24,
            onClick = onClick,
            enabled = enabled,
            dimContent = dimContent,
            isInProgress = isInProgress,
        ),
    )

    /**
     * Swap
     *
     * @property enabled button click availability
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Swap button is clicked
     */
    data class Swap(
        override val enabled: Boolean,
        override val dimContent: Boolean,
        override val onClick: () -> Unit,
        val isInProgress: Boolean = false,
    ) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.swapping_swap_action),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick,
            enabled = enabled,
            dimContent = dimContent,
            isInProgress = isInProgress,
        ),
    )
}