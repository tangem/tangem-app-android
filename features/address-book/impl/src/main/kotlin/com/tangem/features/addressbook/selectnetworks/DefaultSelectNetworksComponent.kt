package com.tangem.features.addressbook.selectnetworks

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.addressbook.selectnetworks.model.SelectNetworksModel
import com.tangem.features.addressbook.selectnetworks.ui.SelectNetworksContent

internal class DefaultSelectNetworksComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SelectNetworksModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        SelectNetworksContent(
            state = state,
            modifier = modifier,
        )
        BackHandler(onBack = state.onBackClick)
    }

    data class Params(
        val address: String,
        val selectedNetworkIds: List<String>,
        val onBackClick: () -> Unit,
        val onDone: (selectedNetworkIds: Set<String>) -> Unit,
    )
}