package com.tangem.features.details.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.model.DetailsModel
import com.tangem.features.details.ui.DetailsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: DetailsComponent.Params,
    userWalletListComponentFactory: UserWalletListComponent.Factory,
) : DetailsComponent, AppComponentContext by context {

    private val model: DetailsModel = getOrCreateModel(params)

    private val userWalletListComponent = userWalletListComponentFactory.create(
        context = child(key = "user_wallet_list_component"),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        DetailsScreen(
            modifier = modifier,
            state = state,
            userWalletListBlockContent = userWalletListComponent,
        )
    }

    @AssistedFactory
    interface Factory : DetailsComponent.Factory {
        override fun create(context: AppComponentContext, params: DetailsComponent.Params): DefaultDetailsComponent
    }
}
