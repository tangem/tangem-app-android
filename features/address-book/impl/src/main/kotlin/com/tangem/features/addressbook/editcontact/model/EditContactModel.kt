package com.tangem.features.addressbook.editcontact.model

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.EditContactComponent
import com.tangem.features.addressbook.editcontact.contract.EditContactUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class EditContactModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: EditContactComponent.Params = paramsContainer.require()

    val state: StateFlow<EditContactUM>
        field = MutableStateFlow(getInitialState())

    private fun onNameChange(name: String) {
        state.update { it.copy(name = name) }
    }

    private fun onColorSelect(color: CryptoPortfolioIcon.Color) {
        state.update { oldState ->
            oldState.copy(
                colors = oldState.colors.copy(selected = color),
                portfolioIcon = oldState.portfolioIcon.copy(color = color),
            )
        }
    }

    private fun getInitialState(): EditContactUM {
        val colors = CryptoPortfolioIcon.Color.entries.toImmutableList()
        val selectedColor = colors.first()
        val titleResId = if (params.contactId == null) {
            R.string.address_book_new_contact
        } else {
            R.string.address_book_contact
        }
        return EditContactUM(
            title = resourceReference(titleResId),
            name = "",
            namePlaceholder = resourceReference(R.string.address_book_new_contact),
            portfolioIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = selectedColor,
            ),
            colors = EditContactUM.Colors(
                selected = selectedColor,
                list = colors,
                onColorSelect = ::onColorSelect,
            ),
            onNameChange = ::onNameChange,
            onCloseClick = params.onBackClick,
        )
    }
}