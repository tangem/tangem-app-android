package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class AddValidatedAddressTransformer(
    private val address: ValidatedAddress,
    private val maxAddresses: Int,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        // Skip duplicates: an address is identified by its string value (it already carries all its networks).
        if (prevState.addresses.any { it.address == address.address }) return prevState
        val addresses = (prevState.addresses + address).toImmutableList()
        return prevState.copy(
            addresses = addresses,
            isAddAddressEnabled = addresses.size < maxAddresses,
        )
    }
}