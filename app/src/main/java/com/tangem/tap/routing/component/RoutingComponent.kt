package com.tangem.tap.routing.component

import android.content.Intent
import androidx.compose.runtime.Immutable
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface RoutingComponent : ComposableContentComponent {

    @Immutable
    sealed class Child {

        // TODO: Remove and find initial intent in RoutingComponent: [REDACTED_JIRA]
        data object Initial : Child()

        data class LegacyIntent(val intent: Intent) : Child()

        data class ComposableComponent(
            val component: ComposableContentComponent,
        ) : Child()
    }

    interface Factory {
        fun create(
            context: AppComponentContext,
            initialStack: List<AppRoute>?,
            launchMode: InitScreenLaunchMode,
        ): RoutingComponent
    }
}