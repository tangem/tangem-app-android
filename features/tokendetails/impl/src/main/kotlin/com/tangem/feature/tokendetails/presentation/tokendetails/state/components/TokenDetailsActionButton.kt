package com.tangem.feature.tokendetails.presentation.tokendetails.state.components

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.tokendetails.impl.R

@Immutable
internal sealed class TokenDetailsActionButton(val config: ActionButtonConfig) {

    /** Lambda be invoked when manage button is clicked */
    abstract val onClick: () -> Unit

    /**
     * Buy
     *
     * @property onClick lambda be invoked when Buy button is clicked
     */
    data class Buy(override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_buy),
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick,
        ),
    )

    /**
     * Send
     *
     * @property onClick lambda be invoked when Send button is clicked
     */
    data class Send(override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_send),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick,
        ),
    )

    /**
     * Receive
     *
     * @property onClick lambda be invoked when Receive button is clicked
     */
    data class Receive(override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_receive),
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = onClick,
        ),
    )

    /**
     * Sell
     *
     * @property onClick lambda be invoked when Sell button is clicked
     */
    data class Sell(override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_sell),
            iconResId = R.drawable.ic_currency_24,
            onClick = onClick,
        ),
    )

    /**
     * Swap
     *
     * @property onClick lambda be invoked when Swap button is clicked
     */
    data class Swap(override val onClick: () -> Unit) : TokenDetailsActionButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.swapping_swap_action),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick,
        ),
    )
}
