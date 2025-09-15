package com.tangem.features.tangempay.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.decompose.context.AppComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultTangemPayDetailsComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsComponent.Params,
) : AppComponentContext by appComponentContext, TangemPayDetailsComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier.fillMaxSize().background(Color.Red))
        // TODO("[REDACTED_JIRA]")
    }

    @AssistedFactory
    interface Factory : TangemPayDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayDetailsComponent.Params,
        ): DefaultTangemPayDetailsComponent
    }
}