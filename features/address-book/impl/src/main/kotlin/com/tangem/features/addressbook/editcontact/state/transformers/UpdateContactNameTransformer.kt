package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class UpdateContactNameTransformer(
    private val name: String,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(name = name)
    }
}