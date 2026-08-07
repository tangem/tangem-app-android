package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.impl.R
import com.tangem.utils.transformer.Transformer

internal class UpdateSaveButtonTransformer(
    private val isEnabled: Boolean,
    private val isLoading: Boolean,
    private val isColdWallet: Boolean,
    private val isNewContact: Boolean,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            saveButton = prevState.saveButton.copy(
                isEnabled = isEnabled,
                isLoading = isLoading,
                tangemIconUM = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24).takeIf { isColdWallet },
                text = if (isNewContact) {
                    resourceReference(R.string.address_book_add_contact)
                } else {
                    resourceReference(R.string.address_book_save_contact)
                },
            ),
        )
    }
}