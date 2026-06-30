package com.tangem.features.addressbook.addaddress

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.addressbook.addaddress.model.AddAddressModel
import com.tangem.features.addressbook.addaddress.ui.AddAddressContent
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress

internal class DefaultAddAddressComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: AddAddressModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        AddAddressContent(
            state = state,
            modifier = modifier,
        )
        BackHandler(onBack = state.onBackClick)
    }

    data class Params(
        val onBackClick: () -> Unit,
        val onSelectNetworksClick: (address: String, selectedNetworkIds: List<String>) -> Unit,
        val onConfirm: (ValidatedAddress) -> Unit,
    )
}