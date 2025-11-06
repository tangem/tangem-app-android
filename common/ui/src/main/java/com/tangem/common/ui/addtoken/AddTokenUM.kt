package com.tangem.common.ui.addtoken

import androidx.annotation.DrawableRes
import com.tangem.common.ui.account.PortfolioSelectUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

data class AddTokenUM(
    val tokenToAdd: TokenItemState,
    val network: Network,
    val portfolio: PortfolioSelectUM,
    val button: Button,
) {

    data class Network(
        @DrawableRes val icon: Int,
        val name: TextReference,
        val editable: Boolean,
        val onClick: () -> Unit,
    )

    data class Button(
        val isEnabled: Boolean,
        val showProgress: Boolean,
        val isTangemIconVisible: Boolean,
        val onConfirmClick: () -> Unit,
        val text: TextReference,
    )
}