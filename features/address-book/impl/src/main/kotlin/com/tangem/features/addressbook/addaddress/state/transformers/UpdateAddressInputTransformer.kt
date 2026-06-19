package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.transformer.Transformer

/**
 * Updates the address field with a freshly entered/pasted [value] and clears any previous error, restoring the default
 * label. The actual (re)validation runs after a debounce — see [UpdateAddressValidationTransformer].
 */
internal class UpdateAddressInputTransformer(
    private val value: String,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        return prevState.copy(
            addressField = prevState.addressField.copy(
                value = value,
                isError = false,
                label = resourceReference(R.string.common_address),
            ),
        )
    }
}