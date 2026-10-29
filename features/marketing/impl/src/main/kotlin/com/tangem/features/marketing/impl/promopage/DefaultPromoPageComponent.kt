package com.tangem.features.marketing.impl.promopage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.marketing.api.promopage.PromoPageComponent
import com.tangem.features.marketing.impl.promopage.ui.PromoPageScreen
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultPromoPageComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: PromoPageComponent.Params,
) : PromoPageComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        PromoPageScreen(
            url = params.url,
            onCloseClick = { router.pop() },
            onLoadError = ::closeOnLoadError,
            modifier = modifier,
        )
    }

    private fun closeOnLoadError(reason: String) {
        // Off the top (closed, covered, in background), a pop would close another screen or drop a loading page
        if (lifecycle.state != Lifecycle.State.RESUMED) return

        TangemLogger.e("Promo page ${params.url} closed: $reason")
        router.pop()
    }

    @AssistedFactory
    interface Factory : PromoPageComponent.Factory {
        override fun create(context: AppComponentContext, params: PromoPageComponent.Params): DefaultPromoPageComponent
    }
}