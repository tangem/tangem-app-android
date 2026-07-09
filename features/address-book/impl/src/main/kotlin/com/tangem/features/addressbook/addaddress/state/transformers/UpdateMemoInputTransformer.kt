package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.transformer.Transformer

internal class UpdateMemoInputTransformer(
    private val value: String,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        return prevState.copy(
            memoField = prevState.memoField.copy(value = value),
        )
    }
}