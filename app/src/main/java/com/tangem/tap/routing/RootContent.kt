package com.tangem.tap.routing

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.routing.component.RoutingComponent

@Composable
internal fun RootContent(
    stack: ChildStack<AppRoute, RoutingComponent.Child>,
    uiDependencies: UiDependencies,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TangemTheme(
        activity = context as Activity,
        uiDependencies = uiDependencies,
    ) {
        Children(
            modifier = modifier,
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is RoutingComponent.Child.Initial -> Unit
                is RoutingComponent.Child.ComposableComponent -> {
                    instance.component.Content(Modifier.fillMaxSize())
                }
                is RoutingComponent.Child.LegacyIntent -> {
// [REDACTED_TODO_COMMENT]
                    startActivity(context, instance.intent, Bundle.EMPTY)
                }
                is RoutingComponent.Child.LegacyFragment,
                -> error("Unsupported child: $instance")
            }
        }
    }
}
