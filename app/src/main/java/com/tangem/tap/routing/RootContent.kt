package com.tangem.tap.routing

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV2
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.ZoomTransitions
import androidx.compose.runtime.CompositionLocalProvider
import com.tangem.core.decompose.ui.stack.ChildStack
import com.tangem.core.decompose.ui.stack.animation.PredictiveBackParams
import com.tangem.core.decompose.ui.stack.animation.StackAnimation
import com.tangem.core.decompose.ui.stack.animation.stackAnimation
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.snackbar.TangemTopSnackbarHost
import com.tangem.core.ui.ds2.modal.LocalTangemModalHost
import com.tangem.core.ui.ds2.modal.TangemModalHost
import com.tangem.core.ui.ds2.modal.rememberTangemModalHostState
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.res.*
import com.tangem.core.ui.security.ProvideSecureFlagController
import com.tangem.core.ui.utils.ZoomTransitionChild
import com.tangem.core.ui.utils.zoomTransitionDestination
import com.tangem.tap.routing.component.RoutingComponent
import com.tangem.tap.routing.transitions.RoutingTransitionAnimationFactory
import dev.chrisbanes.haze.rememberHazeState

@Suppress("LongParameterList", "ReusedModifierInstance")
@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun RootContent(
    stack: Value<ChildStack<AppRoute, RoutingComponent.Child>>,
    backHandler: BackHandler,
    uiDependencies: UiDependencies,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    wcContent: @Composable (modifier: Modifier) -> Unit,
    promoContent: @Composable (modifier: Modifier) -> Unit,
    walletLinkContent: @Composable (modifier: Modifier) -> Unit,
    hotAccessCodeContent: @Composable (modifier: Modifier) -> Unit,
    startupGateContent: @Composable (modifier: Modifier) -> Unit,
    scanFailsContent: @Composable (modifier: Modifier) -> Unit,
) {
    val context = LocalContext.current

    TangemTheme(
        activity = context as Activity,
        uiDependencies = uiDependencies,
    ) {
        ProvideSecureFlagController {
            val modalHostState = rememberTangemModalHostState()
            val modalHazeState = rememberHazeState()

            val zoomSources = remember { RootZoomSources() }
            val currentStack by stack.subscribeAsState()
            SideEffect { zoomSources.retainAround(currentStack) }

            CompositionLocalProvider(LocalTangemModalHost provides modalHostState) {
                Box(Modifier.background(LocalRootBackgroundColor.current.value)) {
                    Box(Modifier.fillMaxSize().hazeSourceTangem(state = modalHazeState)) {
                        SharedTransitionLayout(modifier = modifier) {
                            ChildStack(
                                modifier = Modifier.fillMaxSize(),
                                animation = childrenAnimation(
                                    backHandler = backHandler,
                                    onBack = onBack,
                                    zoomSources = zoomSources,
                                ),
                                stack = stack,
                            ) { child ->
                                ZoomTransitionChild(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this,
                                ) {
                                    RootChildContent(child = child, zoomSource = zoomSources.of(child))
                                }
                            }
                        }

                        wcContent(Modifier.fillMaxSize())

                        promoContent(Modifier.fillMaxSize())

                        walletLinkContent(Modifier.fillMaxSize())

                        hotAccessCodeContent(Modifier.fillMaxSize())

                        startupGateContent(Modifier.fillMaxSize())

                        scanFailsContent(Modifier.fillMaxSize())
                    }

                    TangemModalHost(
                        state = modalHostState,
                        hazeState = modalHazeState,
                        modifier = Modifier.matchParentSize(),
                    )

                    TangemTopSnackbarHost(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(all = 16.dp),
                        hostState = LocalTopSnackbarHostState.current,
                    )
                }
            }
        }
        EventMessageEffect()
    }
}

@Composable
private fun RootChildContent(child: Child.Created<AppRoute, RoutingComponent.Child>, zoomSource: String?) {
    val context = LocalContext.current

    when (val instance = child.instance) {
        is RoutingComponent.Child.Initial -> Unit
        is RoutingComponent.Child.ComposableComponent -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zoomTransitionDestination(key = zoomSource, shape = RoundedCornerShape(size = 20.dp)),
            ) {
                ProvideHaze {
                    instance.component.Content(Modifier.fillMaxSize())
                }
            }
        }
        is RoutingComponent.Child.LegacyIntent -> {
            // TODO: Remove and use it's own router: [REDACTED_JIRA]
            LaunchedEffect(instance) {
                startActivity(context, instance.intent, Bundle.EMPTY)
            }
        }
        RoutingComponent.Child.DummyComponent -> Unit
    }
}

/**
 * Zoom source of each root child, claimed from [ZoomTransitions] the first time the stack meets the child.
 * A popped child leaves the stack before its zoom back has played, so its source is kept until the stack
 * changes again.
 */
private class RootZoomSources {
    private val byChildKey = mutableMapOf<String, String>()
    private var previousChildKeys = emptySet<String>()
    private var lastStack: ChildStack<AppRoute, *>? = null

    fun of(child: Child.Created<AppRoute, *>): String? = byChildKey[child.key]
        ?: ZoomTransitions.claim(child.configuration)?.also { sourceKey -> byChildKey[child.key] = sourceKey }

    fun retainAround(stack: ChildStack<AppRoute, *>) {
        if (stack == lastStack) return
        lastStack = stack

        val childKeys = stack.items.mapTo(mutableSetOf()) { it.key }
        byChildKey.keys.retainAll(childKeys + previousChildKeys)
        previousChildKeys = childKeys
    }
}

@OptIn(ExperimentalDecomposeApi::class)
private fun childrenAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
    zoomSources: RootZoomSources,
): StackAnimation<AppRoute, RoutingComponent.Child> = stackAnimation(
    predictiveBackParams = { stack ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // a zoomed screen shrinks back into its source under the finger instead of the system scale-down
            val isZoomTransition = zoomSources.of(stack.active) != null
            PredictiveBackParams(
                backHandler = backHandler,
                onBack = onBack,
                animatable = { backEvent ->
                    if (isZoomTransition) null else androidPredictiveBackAnimatableV2(backEvent)
                },
            )
        } else {
            null
        }
    },
    // Initial renders nothing: the first real screen replacing it appears at once, as does going back to it.
    // A zoomed screen moves through its shared bounds only: neither it nor the screen behind slides.
    selector = { child, otherChild, direction, _ ->
        val front = if (direction.isFront) child else otherChild
        val isInitialInvolved = child.configuration is AppRoute.Initial || otherChild.configuration is AppRoute.Initial
        if (isInitialInvolved || zoomSources.of(front) != null) {
            null
        } else {
            RoutingTransitionAnimationFactory.create(child.configuration)
        }
    },
)