package com.tangem.features.onramp.selectcountry

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.onramp.selectcountry.model.OnrampSelectCountryModel
import com.tangem.features.onramp.selectcountry.ui.OnrampCountryList
import com.tangem.features.onramp.selectcountry.ui.SelectCountryBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSelectCountryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SelectCountryComponent.Params,
) : SelectCountryComponent, AppComponentContext by appComponentContext {

    private val model: OnrampSelectCountryModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
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
        SelectCountryBottomSheet(
            config = bottomSheetConfig,
            content = { OnrampCountryList(state = state, modifier = Modifier.fillMaxSize()) },
        )
    }

    @AssistedFactory
    interface Factory : SelectCountryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SelectCountryComponent.Params,
        ): DefaultSelectCountryComponent
    }
}