package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

/**
 * Wires the title (derived from whether an existing contact is being edited) and the callbacks owned by
 * [com.tangem.features.addressbook.editcontact.model.EditContactModel] into the initial state.
 */
internal class UpdateEditContactInitialStateTransformer(
    private val isExistingContact: Boolean,
    private val onNameChange: (String) -> Unit,
    private val onColorSelect: (CryptoPortfolioIcon.Color) -> Unit,
    private val onCloseClick: () -> Unit,
    private val onAddAddressClick: () -> Unit,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        val titleResId = if (isExistingContact) {
            R.string.address_book_contact
        } else {
            R.string.address_book_new_contact
        }
        return prevState.copy(
            title = resourceReference(titleResId),
            colors = prevState.colors.copy(onColorSelect = onColorSelect),
            onNameChange = onNameChange,
            onCloseClick = onCloseClick,
            onAddAddressClick = onAddAddressClick,
        )
    }
}