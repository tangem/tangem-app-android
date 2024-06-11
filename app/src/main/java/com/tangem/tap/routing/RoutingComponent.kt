package com.tangem.tap.routing

import android.content.Intent
import androidx.fragment.app.Fragment
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Router
import com.tangem.utils.Provider

internal interface RoutingComponent {

    val router: Router

    val stack: Value<ChildStack<AppRoute, Child>>

    sealed class Child {

        data object Initial : Child()

        data class LegacyFragment(
            val name: String,
            val fragmentProvider: Provider<Fragment>,
        ) : Child()

        data class LegacyIntent(val intent: Intent) : Child()
    }

    interface Factory {
        fun create(context: AppComponentContext): RoutingComponent
    }
}