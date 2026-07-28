package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class RemoveValidatedAddressTransformer(
    private val address: String,
    private val maxAddresses: Int,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        val addresses = prevState.addresses.filterNot { it.address == address }.toImmutableList()
        return prevState.copy(
            addresses = addresses,
            isAddAddressEnabled = addresses.size < maxAddresses,
        )
    }
}