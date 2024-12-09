package com.tangem.features.onramp.selectcurrency

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
import com.tangem.features.onramp.selectcurrency.model.OnrampSelectCurrencyModel
import com.tangem.features.onramp.selectcurrency.ui.OnrampCurrencyList
import com.tangem.features.onramp.selectcurrency.ui.SelectCurrencyBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSelectCurrencyComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: SelectCurrencyComponent.Params,
) : SelectCurrencyComponent, AppComponentContext by context {

    private val model: OnrampSelectCurrencyModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        SelectCurrencyBottomSheet(
            config = bottomSheetConfig,
            content = {
                OnrampCurrencyList(modifier = Modifier.fillMaxSize(), state = state)
            },
        )
    }

    @AssistedFactory
    interface Factory : SelectCurrencyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SelectCurrencyComponent.Params,
        ): DefaultSelectCurrencyComponent
    }
}