package com.tangem.features.send.subcomponents.destination.model.transformers

import com.tangem.common.ui.account.AccountIconUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationContactTransformer(
    private val contactName: String?,
    private val contactIcon: AccountIconUM.CryptoPortfolio?,
) : Transformer<DestinationUM> {

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