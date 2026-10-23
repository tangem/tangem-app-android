package com.tangem.features.tangempay.success

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.tangempay.components.TangemPayOnboardingSuccessComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayOnboardingSuccessComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Unit,
) : TangemPayOnboardingSuccessComponent, AppComponentContext by context {

    private val model: TangemPayOnboardingSuccessModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        TangemThemeRedesign {
            TangemPayOnboardingSuccessScreen(
                onCloseClick = model::onCloseClick,
                animated = true,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayOnboardingSuccessComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultTangemPayOnboardingSuccessComponent
    }
}