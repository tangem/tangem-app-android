package com.tangem.features.markets.portfolio.add.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.markets.portfolio.add.impl.model.AddToPortfolioModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

internal class AddTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        TODO("Not yet implemented")
    }

    data class Params(
        val selectedPortfolio: Flow<AddToPortfolioModel.SelectedPortfolio>,
    )

    @AssistedFactory
    interface Factory : ComponentFactory<Params, AddTokenComponent> {
        override fun create(context: AppComponentContext, params: Params): AddTokenComponent
    }
}