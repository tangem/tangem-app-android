package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorUM
import com.tangem.features.managetokens.ui.CustomTokenSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

internal class DefaultCustomTokenSelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: CustomTokenSelectorComponent.Params,
) : CustomTokenSelectorComponent, AppComponentContext by context {

    private val state: MutableStateFlow<CustomTokenSelectorUM> = MutableStateFlow(
        value = PreviewCustomTokenSelectorComponent(params = params).previewState,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        CustomTokenSelectorContent(
            modifier = modifier,
            model = state,
        )
    }

    @AssistedFactory
    interface Factory : CustomTokenSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CustomTokenSelectorComponent.Params,
        ): DefaultCustomTokenSelectorComponent
    }
}