package com.tangem.features.addressbook.editcontact.state

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class EditContactStateController @Inject constructor() {

    val uiState: StateFlow<EditContactUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<EditContactUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): EditContactUM {
        val colors = CryptoPortfolioIcon.Color.entries.toImmutableList()
        val selectedColor = colors.first()
        return EditContactUM(
            title = TextReference.EMPTY,
            name = "",
            namePlaceholder = resourceReference(R.string.address_book_new_contact),
            portfolioIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = selectedColor,
            ),
            colors = EditContactUM.Colors(
                selected = selectedColor,
                list = colors,
                onColorSelect = {},
            ),
            addresses = persistentListOf(),
            onNameChange = {},
            onCloseClick = {},
            onAddAddressClick = {},
        )
    }
}