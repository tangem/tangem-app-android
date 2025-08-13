package com.tangem.features.account.createedit.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

data class AccountCreateEditUM(
    val title: TextReference,
    val account: Account,
    val colorsState: Colors,
    val iconsState: Icons,
    val buttonState: Button,
    val onCloseClick: () -> Unit,
) {

    data class Account(
        val name: String,
        val portfolioIcon: CryptoPortfolioIcon,
        val derivationInfo: TextReference,
        val inputPlaceholder: TextReference,
        val onNameChange: (String) -> Unit,
    )

    data class Colors(
        val selected: CryptoPortfolioIcon.Color,
        val list: ImmutableList<CryptoPortfolioIcon.Color>,
        val onColorSelect: (CryptoPortfolioIcon.Color) -> Unit,
    )

    data class Icons(
        val selected: CryptoPortfolioIcon.Icon,
        val list: ImmutableList<CryptoPortfolioIcon.Icon>,
        val onIconSelect: (CryptoPortfolioIcon.Icon) -> Unit,
    )

    data class Button(
        val isButtonEnabled: Boolean,
        val onConfirmClick: () -> Unit,
        val text: TextReference,
    )
}