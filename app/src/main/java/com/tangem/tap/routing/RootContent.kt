package com.tangem.tap.routing

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
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
        val snackbarHostState = LocalSnackbarHostState.current

        Scaffold(
            modifier = modifier,
            containerColor = TangemTheme.colors.background.primary,
            snackbarHost = {
                TangemSnackbarHost(
                    modifier = Modifier.padding(all = 16.dp),
                    hostState = snackbarHostState,
                )
            },
            contentWindowInsets = WindowInsetsZero,
            content = { paddingValues ->
                Children(
                    modifier = Modifier.padding(paddingValues),
                    stack = stack,
                ) { child ->
                    when (val instance = child.instance) {
                        is RoutingComponent.Child.Initial -> Unit
                        is RoutingComponent.Child.ComposableComponent -> {
                            instance.component.Content(Modifier.fillMaxSize())
                        }
                        is RoutingComponent.Child.LegacyIntent -> {
                            // TODO: Remove and use it's own router: [REDACTED_JIRA]
                            startActivity(context, instance.intent, Bundle.EMPTY)
                        }
                        is RoutingComponent.Child.LegacyFragment,
                        -> error("Unsupported child: $instance")
                    }
                }
            },
        )

        EventMessageEffect()
    }
}