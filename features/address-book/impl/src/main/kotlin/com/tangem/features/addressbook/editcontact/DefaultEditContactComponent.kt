package com.tangem.features.addressbook.editcontact

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.features.addressbook.editcontact.model.EditContactModel
import com.tangem.features.addressbook.editcontact.ui.EditContactContent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress

internal class DefaultEditContactComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: EditContactModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        EditContactContent(
            state = state,
            modifier = modifier,
        )
        BackHandler(onBack = state.onCloseClick)
    }

    data class Params(
        val contactId: ContactId?,
        val predefinedAddress: ValidatedAddress? = null,
        val onBackClick: () -> Unit,
        val onAddAddressClick: () -> Unit,
    )
}