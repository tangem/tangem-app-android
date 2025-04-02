package com.tangem.tap.routing

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.res.LocalIsNavigationRefactoringEnabled
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.routing.component.RoutingComponent

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun RootContent(
    stack: Value<ChildStack<AppRoute, RoutingComponent.Child>>,
    uiDependencies: UiDependencies,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TangemTheme(
        activity = context as Activity,
        uiDependencies = uiDependencies,
    ) {
        CompositionLocalProvider(
            LocalIsNavigationRefactoringEnabled provides true,
        ) {
            val snackbarHostState = LocalSnackbarHostState.current

            Box(Modifier.background(TangemTheme.colors.background.primary)) {
                Children(
                    modifier = modifier,
                    animation = stackAnimation(fade()),
                    stack = stack,
                ) { child ->
                    when (val instance = child.instance) {
                        is RoutingComponent.Child.Initial -> Unit
                        is RoutingComponent.Child.ComposableComponent -> {
                            instance.component.Content(Modifier.fillMaxSize())
                        }
                        is RoutingComponent.Child.LegacyIntent -> {
                            // TODO: Remove and use it's own router: [REDACTED_JIRA]
                            LaunchedEffect(instance) {
                                startActivity(context, instance.intent, Bundle.EMPTY)
                            }
                        }
                        is RoutingComponent.Child.LegacyFragment,
                        -> error("Unsupported child: $instance")
                    }
                }

                TangemSnackbarHost(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(all = 16.dp),
                    hostState = snackbarHostState,
                )
            }
        }

        EventMessageEffect()
    }
}