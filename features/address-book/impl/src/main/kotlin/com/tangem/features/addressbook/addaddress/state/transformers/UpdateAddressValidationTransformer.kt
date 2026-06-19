package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.transformer.Transformer

/**
 * Validates [address] against the wallet's [coins] and reflects the result in the UI.
 *
 * The network is not chosen on this screen (it is selected on the next screen), so the address is valid when it matches
 * at least one of the available networks — the same blockchain check the Send flow uses. An invalid (non-empty,
 * matching nothing) address surfaces the error in the field label and disables the confirm button.
 */
internal class UpdateAddressValidationTransformer(
    private val address: String,
    private val coins: List<CryptoCurrency.Coin>,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        val hasMatchedAnyNetwork = address.isNotBlank() &&
            coins.any { it.network.toBlockchain().validateAddress(address) }
        val isError = address.isNotBlank() && !hasMatchedAnyNetwork
        val label = if (isError) {
            resourceReference(R.string.address_book_invalid_address_error)
        } else {
            resourceReference(R.string.common_address)
        }
        return prevState.copy(
            addressField = prevState.addressField.copy(isError = isError, label = label),
            buttonUM = prevState.buttonUM.copy(isEnabled = hasMatchedAnyNetwork),
        )
    }
}