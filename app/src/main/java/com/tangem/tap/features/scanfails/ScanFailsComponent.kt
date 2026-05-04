package com.tangem.tap.features.scanfails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.tap.features.scanfails.ui.ScanFailsDialogContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ScanFailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AppComponentContext by appComponentContext, ComposableContentComponent, ScanFailsRequester {

    private val model: ScanFailsModel = getOrCreateModel(params)

    override suspend fun show(source: AnalyticsParam.ScreensSources): ScanFailsRequester.Result {
        model.show(source)
        return model.waitResult()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        if (state.isShown) {
            ScanFailsDialogContent(state = state)
        }
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Unit, ScanFailsComponent> {
        override fun create(context: AppComponentContext, params: Unit): ScanFailsComponent
    }
}