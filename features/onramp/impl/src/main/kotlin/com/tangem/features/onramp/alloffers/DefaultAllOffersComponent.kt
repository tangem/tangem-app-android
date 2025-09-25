package com.tangem.features.onramp.alloffers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.alloffers.model.AllOffersModel
import com.tangem.features.onramp.alloffers.ui.AllOffersContentSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAllOffersComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: AllOffersComponent.Params,
) : AllOffersComponent, AppComponentContext by context {

    private val model: AllOffersModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsState()
        AllOffersContentSheet(
            state = state,
            onCloseClick = { dismiss() },
        )
    }

    @AssistedFactory
    interface Factory : AllOffersComponent.Factory {
        override fun create(context: AppComponentContext, params: AllOffersComponent.Params): DefaultAllOffersComponent
    }
}