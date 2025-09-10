package com.tangem.features.account.createedit.entity

import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.CryptoPortfolioIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

internal data class AccountCreateEditUM(
    val title: TextReference,
    val account: Account,
    val colorsState: Colors,
    val iconsState: Icons,
    val buttonState: Button,
    val onCloseClick: () -> Unit,
) {

    data class Account(
        val name: AccountNameUM,
        val portfolioIcon: CryptoPortfolioIconUM,
        val derivationInfo: DerivationInfo,
        val inputPlaceholder: TextReference,
        val onNameChange: (AccountNameUM) -> Unit,
    )

    sealed interface DerivationInfo {
        val text: TextReference
        val index: Int?

        data class Content(override val text: TextReference, override val index: Int) : DerivationInfo

        data object Empty : DerivationInfo {
            override val text: TextReference = TextReference.EMPTY
            override val index: Int? = null
        }
    }

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