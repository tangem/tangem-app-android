package com.tangem.features.onramp.redirect

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.redirect.model.OnrampRedirectModel
import com.tangem.features.onramp.redirect.ui.OnrampRedirectContent
import com.tangem.features.onramp.success.OnrampSuccessScreenTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultOnrampRedirectComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampRedirectComponent.Params,
    private val onrampSuccessScreenTrigger: OnrampSuccessScreenTrigger,
) : OnrampRedirectComponent, AppComponentContext by appComponentContext {

    private val model: OnrampRedirectModel = getOrCreateModel(params)

    init {
        lifecycle.subscribe(
            onResume = {
                // On reopen check if redirect happened. If so trigger close action
                if (model.latestOnrampTransaction != null) {
                    componentScope.launch {
                        onrampSuccessScreenTrigger.triggerOnrampSuccess(false)
                    }
                }
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = params.onBack)
        OnrampRedirectContent(modifier = modifier, state = model.state)

        val isDarkTheme = isSystemInDarkTheme()
        LaunchedEffect(model.state) {
            model.getRedirectUrl(isDarkTheme = isDarkTheme)
        }
    }

    @AssistedFactory
    interface Factory : OnrampRedirectComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampRedirectComponent.Params,
        ): DefaultOnrampRedirectComponent
    }
}