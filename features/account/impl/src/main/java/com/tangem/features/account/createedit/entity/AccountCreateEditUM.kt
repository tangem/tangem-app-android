package com.tangem.features.account.createedit.entity

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AccountCreateEditUM(
    val title: TextReference = TextReference.EMPTY,
    val account: Account = Account(),
    val colorsState: Colors,
    val iconsState: Icons,
    val buttonState: Button = Button(),
    val onCloseClick: () -> Unit = {},
) {

    data class Account(
        val name: TextReference = TextReference.EMPTY,
        val portfolioIcon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        val derivationInfo: TextReference = TextReference.EMPTY,
        val inputPlaceholder: TextReference = TextReference.EMPTY,
        val onNameChange: (String) -> Unit = {},
    )

    data class Colors(
        val selected: CryptoPortfolioIcon.Color,
        val list: ImmutableList<CryptoPortfolioIcon.Color> = persistentListOf(),
        val onColorSelect: (CryptoPortfolioIcon.Color) -> Unit = {},
    )

    data class Icons(
        val selected: CryptoPortfolioIcon.Icon,
        val list: ImmutableList<CryptoPortfolioIcon.Icon> = persistentListOf(),
        val onIconSelect: (CryptoPortfolioIcon.Icon) -> Unit = {},
    )

    data class Button(
        val isButtonEnabled: Boolean = false,
        val onConfirmClick: () -> Unit = {},
        val text: TextReference = TextReference.EMPTY,
    )
}