package com.tangem.features.addressbook.editcontact

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId

internal interface EditContactComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, EditContactComponent>

    data class Params(
        val contactId: ContactId?,
        val onBackClick: () -> Unit,
    )
}