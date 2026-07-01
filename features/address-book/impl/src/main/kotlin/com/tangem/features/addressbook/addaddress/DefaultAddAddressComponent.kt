package com.tangem.features.addressbook.addaddress

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.addressbook.addaddress.model.AddAddressModel
import com.tangem.features.addressbook.addaddress.ui.AddAddressContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddAddressComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: AddAddressComponent.Params,
) : AddAddressComponent, AppComponentContext by context {

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

    @AssistedFactory
    interface Factory : AddAddressComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddAddressComponent.Params,
        ): DefaultAddAddressComponent
    }
}