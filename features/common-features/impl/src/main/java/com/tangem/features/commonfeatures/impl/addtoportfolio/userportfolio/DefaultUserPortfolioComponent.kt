package com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.markets.tokenselector.StickyFooter
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEmbeddedContent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.model.UserPortfolioModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultUserPortfolioComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: UserPortfolioComponent.Params,
) : UserPortfolioComponent, AppComponentContext by context {

    private val model: UserPortfolioModel = getOrCreateModel(params)
    private val onContinueClick: () -> Unit = params.callbacks::onContinueFromUserPortfolio

    @Composable
    override fun Content(modifier: Modifier) {
        val stateFlow = model.state.collectAsStateWithLifecycle()
        val state = stateFlow.value ?: return
        TokenSelectorEmbeddedContent(
            content = state.content,
            modifier = modifier,
            stickyFooter = StickyFooter(
                buttonText = resourceReference(R.string.common_add),
                isEnabled = state.isAddEnabled,
                onClick = onContinueClick,
            ),
        )
    }

    @AssistedFactory
    interface Factory : UserPortfolioComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: UserPortfolioComponent.Params,
        ): DefaultUserPortfolioComponent
    }
}