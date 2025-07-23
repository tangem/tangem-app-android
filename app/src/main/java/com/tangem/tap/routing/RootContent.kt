package com.tangem.tap.routing

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.res.LocalRootBackgroundColor
import com.tangem.core.ui.res.LocalSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.transitions.RoutingTransitionAnimationFactory

@Suppress("LongParameterList")
@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun RootContent(
    stack: Value<ChildStack<AppRoute, RoutingComponent.Child>>,
    backHandler: BackHandler,
    uiDependencies: UiDependencies,
    wcContent: @Composable (modifier: Modifier) -> Unit,
    hotAccessCodeContent: @Composable (modifier: Modifier) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TangemTheme(
        activity = context as Activity,
        uiDependencies = uiDependencies,
    ) {
        val snackbarHostState = LocalSnackbarHostState.current

        Box(Modifier.background(LocalRootBackgroundColor.current.value)) {
            Children(
                modifier = modifier,
                animation = childrenAnimation(backHandler = backHandler, onBack = onBack),
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
                }
            }

            wcContent(Modifier.fillMaxSize())

            hotAccessCodeContent(Modifier.fillMaxSize())

            TangemSnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(all = 16.dp),
                hostState = snackbarHostState,
            )
        }
        EventMessageEffect()
    }
}

@OptIn(ExperimentalDecomposeApi::class)
private fun childrenAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
): StackAnimation<AppRoute, RoutingComponent.Child> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        predictiveBackAnimation(
            backHandler = backHandler,
            onBack = onBack,
            selector = { backEvent, _, _ ->
                androidPredictiveBackAnimatable(backEvent)
            },
            fallbackAnimation = stackAnimation {
                RoutingTransitionAnimationFactory.create(it.configuration)
            },
        )
    } else {
        stackAnimation {
            RoutingTransitionAnimationFactory.create(it.configuration)
        }
    }
}