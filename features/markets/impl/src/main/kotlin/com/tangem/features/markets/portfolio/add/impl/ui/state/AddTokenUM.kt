package com.tangem.features.markets.portfolio.add.impl.ui.state

import androidx.annotation.DrawableRes
import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

data class AddTokenUM(
    val tokenToAdd: TokenItemState,
    val network: Network,
    val portfolio: Portfolio,
    val button: Button,
) {
    data class Portfolio(
        val accountIconUM: CryptoPortfolioIconUM?,
        val name: TextReference,
        val editable: Boolean,
        val onClick: () -> Unit,
    ) {
        val isAccountMode get() = accountIconUM != null
    }

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