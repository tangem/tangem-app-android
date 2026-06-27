package com.tangem.features.send.subcomponents.destination.model.transformers

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.domain.addressbook.model.Contact
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.subcomponents.destination.model.converter.ContactIconConverter
import com.tangem.utils.transformer.Transformer

internal class SendDestinationContactTransformer(
    private val contactName: String?,
    private val contactIcon: AccountIconUM.CryptoPortfolio?,
) : Transformer<DestinationUM> {

    constructor(contact: Contact?) : this(
        contactName = contact?.name?.value,
        contactIcon = contact?.let(ContactIconConverter::convert),
    )

    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            addressTextField = state.addressTextField.copy(
                contactName = contactName,
                contactIcon = contactIcon,
            ),
        )
    }
}