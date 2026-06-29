package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM
import com.tangem.utils.transformer.Transformer

/**
 * Updates the address field with a freshly entered/pasted [value] and clears any previous error, restoring the default
 * label. The confirm button is disabled while validation is pending; the actual (re)validation runs after a debounce —
 * see [UpdateAddressValidationTransformer].
 *
 * The network selector reflects the pending validation: a non-blank address shows [ChosenNetworkStateUM.Loading], but
 * an already-resolved selector keeps its networks on screen instead of flashing back to the spinner on every keystroke.
 */
internal class UpdateAddressInputTransformer(
    private val value: String,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        val chosenNetworkState = when {
            value.isBlank() -> ChosenNetworkStateUM.Hidden
            prevState.chosenNetworkStateUM is ChosenNetworkStateUM.Result -> prevState.chosenNetworkStateUM
            else -> ChosenNetworkStateUM.Loading
        }
        val memoField = if (value.isBlank()) {
            prevState.memoField.copy(isVisible = false, value = "", isError = false)
        } else {
            prevState.memoField
        }
        return prevState.copy(
            addressField = prevState.addressField.copy(
                value = value,
                isError = false,
                label = resourceReference(R.string.common_address),
            ),
            chosenNetworkStateUM = chosenNetworkState,
            buttonUM = prevState.buttonUM.copy(isEnabled = false),
            memoField = memoField,
        )
    }
}