package com.tangem.features.markets.portfolio.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.markets.portfolio.impl.model.MarketsPortfolioModel
import com.tangem.features.markets.portfolio.impl.ui.MyPortfolio
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: MarketsPortfolioComponent.Params,
) : AppComponentContext by context, MarketsPortfolioComponent {

    private val model: MarketsPortfolioModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        MyPortfolio(
            modifier = modifier,
            state = MyPortfolioUM.Loading,
        )
    }

    @AssistedFactory
    interface Factory : MarketsPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsPortfolioComponent.Params,
        ): DefaultMarketsPortfolioComponent
    }
}
