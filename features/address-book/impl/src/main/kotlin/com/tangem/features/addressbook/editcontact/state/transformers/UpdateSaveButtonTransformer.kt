package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class UpdateSaveButtonTransformer(
    private val isEnabled: Boolean,
    private val isLoading: Boolean,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            saveButton = prevState.saveButton.copy(isEnabled = isEnabled, isLoading = isLoading),
        )
    }
}