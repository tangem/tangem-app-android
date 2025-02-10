package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tokendetails.impl.R

@Immutable
internal sealed class TokenDetailsActionButton(val config: ActionButtonConfig) {

    /** Lambda be invoked when manage button is clicked */
    abstract val onClick: () -> Unit

    /** Lambda be invoked when manage button is long clicked */
    open val onLongClick: (() -> TextReference?)? = null

    /**
     * Buy
     *
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Buy button is clicked
     */
    data class Buy(val dimContent: Boolean, override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_buy),
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick,
            dimContent = dimContent,
        ),
    )

    /**
     * Send
     *
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Send button is clicked
     */
    data class Send(val dimContent: Boolean, override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_send),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick,
            dimContent = dimContent,
        ),
    )

    /**
     * Receive
     * @property onClick lambda be invoked when Receive button is clicked
     * @property onLongClick lambda be invoked when Receive button is long clicked
     */
    data class Receive(
        override val onClick: () -> Unit,
        override val onLongClick: (() -> TextReference?)?,
    ) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_receive),
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = true,
        ),
    )

    /**
     * Sell
     *
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Sell button is clicked
     */
    data class Sell(val dimContent: Boolean, override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_sell),
            iconResId = R.drawable.ic_currency_24,
            onClick = onClick,
            dimContent = dimContent,
        ),
    )

    /**
     * Swap
     *
     * @property dimContent determines whether the button content will be dimmed
     * @property onClick lambda be invoked when Swap button is clicked
     */
    data class Swap(
        val dimContent: Boolean,
        val showBadge: Boolean,
        override val onClick: () -> Unit,
    ) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.swapping_swap_action),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick,
            dimContent = dimContent,
            showBadge = showBadge,
        ),
    )
}