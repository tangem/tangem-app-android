package com.tangem.tap.routing.component

import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.fragment.app.Fragment
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.utils.Provider

internal interface RoutingComponent : ComposableContentComponent {
// [REDACTED_TODO_COMMENT]
    val router: Router
// [REDACTED_TODO_COMMENT]
    val stack: Value<ChildStack<AppRoute, Child>>

    @Immutable
    sealed class Child {
// [REDACTED_TODO_COMMENT]
        data object Initial : Child()

        data class LegacyFragment(
            val name: String,
            val fragmentProvider: Provider<Fragment>,
        ) : Child()

        data class LegacyIntent(val intent: Intent) : Child()

        data class ComposableComponent(
            val component: ComposableContentComponent,
        ) : Child()
    }

    interface Factory {
        fun create(context: AppComponentContext, initialStack: List<AppRoute>?): RoutingComponent
    }
}
