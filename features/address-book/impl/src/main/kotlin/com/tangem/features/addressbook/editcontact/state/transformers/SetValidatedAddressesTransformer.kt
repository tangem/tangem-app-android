package com.tangem.features.addressbook.editcontact.state.transformers

import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class SetValidatedAddressesTransformer(
    private val addresses: List<ValidatedAddress>,
    private val maxAddresses: Int,
) : Transformer<EditContactUM> {

    override fun transform(prevState: EditContactUM): EditContactUM {
        return prevState.copy(
            addresses = addresses.toImmutableList(),
            isAddAddressEnabled = addresses.size < maxAddresses,
        )
    }
}