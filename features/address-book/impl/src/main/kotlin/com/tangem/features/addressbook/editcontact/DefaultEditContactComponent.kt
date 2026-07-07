package com.tangem.features.addressbook.editcontact

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.addressbook.editcontact.model.EditContactModel
import com.tangem.features.addressbook.editcontact.ui.EditContactContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultEditContactComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: EditContactComponent.Params,
) : EditContactComponent, AppComponentContext by context {

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

    @AssistedFactory
    interface Factory : EditContactComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: EditContactComponent.Params,
        ): DefaultEditContactComponent
    }
}