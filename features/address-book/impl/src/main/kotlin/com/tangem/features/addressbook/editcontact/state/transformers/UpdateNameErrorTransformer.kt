package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer

internal class UpdateNameErrorTransformer(private val error: TextReference?) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(nameError = error)
    }
}