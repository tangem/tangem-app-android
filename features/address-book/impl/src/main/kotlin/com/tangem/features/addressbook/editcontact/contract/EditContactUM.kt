package com.tangem.features.addressbook.editcontact.contract

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EditContactUM(
    val title: TextReference,
    val name: String,
    val namePlaceholder: TextReference,
    val portfolioIcon: AccountIconUM.CryptoPortfolio,
    val colors: Colors,
    val onNameChange: (String) -> Unit,
    val onCloseClick: () -> Unit,
) {

    @Immutable
    data class Colors(
        val selected: CryptoPortfolioIcon.Color,
        val list: ImmutableList<CryptoPortfolioIcon.Color>,
        val onColorSelect: (CryptoPortfolioIcon.Color) -> Unit,
    )
}