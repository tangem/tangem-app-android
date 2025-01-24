package com.tangem.features.onramp.hottokens.portfolio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.onramp.hottokens.portfolio.model.OnrampAddToPortfolioModel
import com.tangem.features.onramp.hottokens.portfolio.ui.OnrampAddToPortfolioBottomSheet
import com.tangem.features.onramp.hottokens.portfolio.ui.OnrampAddToPortfolioContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampAddToPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: OnrampAddToPortfolioComponent.Params,
) : OnrampAddToPortfolioComponent, AppComponentContext by context {

    private val model: OnrampAddToPortfolioModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        OnrampAddToPortfolioBottomSheet(
            config = bottomSheetConfig,
            content = { OnrampAddToPortfolioContent(state = state) },
        )
    }

    @AssistedFactory
    interface Factory : OnrampAddToPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampAddToPortfolioComponent.Params,
        ): DefaultOnrampAddToPortfolioComponent
    }
}