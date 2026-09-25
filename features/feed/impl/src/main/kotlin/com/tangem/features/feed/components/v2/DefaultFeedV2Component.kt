package com.tangem.features.feed.components.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.animation.WithoutMaterialWhileAnimating
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.extensions.dropWhenNoRoom
import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.feed.nav.FeedScreenFactory
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.features.feed.ui.v2.FeedSearchBarHeader
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedV2Component
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Provider

@Suppress("LongParameterList")
@Stable
internal class DefaultFeedV2Component @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: Unit,
    private val screenFactories: Map<Class<*>, @JvmSuppressWildcards Provider<FeedScreenFactory>>,
    private val searchBarController: DefaultFeedSearchBarController,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    private val addAccountTypeComponentFactory: AddAccountTypeComponent.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val feedEntryController: FeedEntryController,
    private val tabSet: FeedTabSet,
) : FeedV2Component, AppComponentContext by context {

    override val isSheetBlurEnabled: StateFlow<Boolean>
        field = MutableStateFlow(true)

    private val stackNavigation = StackNavigation<FeedRoute>()

    private val shtorkaExpansion = MutableStateFlow(ShtorkaExpansion.PILL)

    private val blocksAnalytics = FeedBlocksAnalytics(
        analyticsEventHandler = analyticsEventHandler,
        shtorkaExpansion = shtorkaExpansion,
    )

    /**
     * Declared above [stack]: the child collecting it is built while [stack] initialises, so a later
     * declaration would not exist yet there.
     *
     * Replaying, because the request normally precedes that child; a flow rather than a state, because
     * the same section may be asked for twice with the user having switched tab in between, and a
     * conflated state would not re-emit it.
     */
    private val requestedTab = MutableSharedFlow<FeedTabId>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Drained by [Content], the only place the host's `onExpandSheet` lambda is reachable from.
     *
     * A conflated channel rather than a flow: the sheet must rise once per request, and [Content]
     * leaves composition whenever a screen is pushed over the wallet, so a replaying flow would
     * re-open the sheet on every return to it.
     */
    private val expandSheetRequests = Channel<Unit>(capacity = Channel.CONFLATED)

    private val feedRouter = FeedRouter(
        stackNavigation = stackNavigation,
        isRouteRegistered = { route -> route is FeedHomeRoute || screenFactories.containsKey(route.javaClass) },
        popCallback = { onComplete -> onChildBack(onComplete) },
    )

    private val stack: Value<ChildStack<FeedRoute, FeedScreenComponent>> = childStack(
        key = "feedV2Stack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = FeedHomeRoute,
        handleBackButton = false,
        childFactory = ::createChild,
    )

    override val isPartialExpansionSupported: Value<Boolean> =
        stack.map { it.active.instance.isPartialExpansionSupported }

    init {
        componentScope.launch {
            searchBarController.activationRequests.collect(::onSearchActivationRequest)
        }
        componentScope.launch {
            feedEntryController.pendingEntry.filterNotNull().collect(::onEntryRequested)
        }
        componentScope.launch {
            shtorkaExpansion.collect(::onShtorkaExpansionChange)
        }
        stack.subscribe(lifecycle) { stackState ->
            // presence on the stack, not "is active": tapping a search result pushes a screen on top of
            // search, and deactivation drops the query — the user would come back to an empty search
            searchBarController.setActive(
                isActive = stackState.items.any { it.configuration is FeedRoute.Search },
            )
            val active = stackState.active.configuration
            isSheetBlurEnabled.value = active is FeedHomeRoute || active is FeedRoute.Search
        }
        lifecycle.doOnDestroy { searchBarController.setActive(isActive = false) }
    }

    @Composable
    override fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier) {
        val stackState by stack.subscribeAsState()

        LaunchedEffect(Unit) {
            snapshotFlow { sheetExpansion.value }.collect { shtorkaExpansion.value = it }
        }
        val active = stackState.active

        // Home and Search share the search bar as their navbar: they map to the same target (equal
        // by key), so AnimatedContent keeps one composition and the text field survives the
        // Home → Search push, keeping focus while typing.
        val isSearchBarUsed = active.configuration is FeedHomeRoute || active.configuration is FeedRoute.Search
        val target = FeedHeaderTarget(
            key = if (isSearchBarUsed) SEARCH_BAR_HEADER_KEY else active.configuration,
            depth = stackState.items.size,
            screen = if (isSearchBarUsed) null else active.instance,
        )

        AnimatedContent(
            targetState = target,
            transitionSpec = { sharedAxisY(forward = targetState.depth >= initialState.depth) },
            label = "feedHeader",
            modifier = modifier,
        ) { headerTarget ->
            WithoutMaterialWhileAnimating {
                val screen = headerTarget.screen
                if (screen == null) {
                    FeedSearchBarHeader(
                        searchBarController = searchBarController,
                        sheetExpansion = sheetExpansion,
                        onExpandSheet = onExpandSheet,
                        modifier = Modifier,
                    )
                } else {
                    screen.Header(
                        sheetExpansion = sheetExpansion,
                        onExpandSheet = onExpandSheet,
                        modifier = Modifier,
                    )
                }
            }
        }
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val stackState by stack.subscribeAsState()
        val expandSheet by rememberUpdatedState(onExpandSheet)

        LaunchedEffect(Unit) {
            expandSheetRequests.receiveAsFlow().collect { expandSheet() }
        }

        BackHandler(
            enabled = bottomSheetState.value == BottomSheetState.EXPANDED &&
                stackState.active.configuration !is FeedHomeRoute,
        ) {
            onChildBack()
        }

        Children(
            stack = stackState,
            modifier = modifier.dropWhenNoRoom(minRoom = contentPadding.calculateTopPadding() + 8.dp),
            animation = stackAnimation(slide()),
        ) { child ->
            child.instance.Content(
                bottomSheetState = bottomSheetState,
                contentPadding = contentPadding,
                onExpandSheet = onExpandSheet,
                modifier = Modifier,
            )
        }
    }

    private fun createChild(route: FeedRoute, factoryContext: ComponentContext): FeedScreenComponent {
        val childContext = childByContext(componentContext = factoryContext, router = feedRouter)

        return when (route) {
            is FeedHomeRoute -> FeedHomeComponent(
                context = childContext,
                portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
                addAccountTypeComponentFactory = addAccountTypeComponentFactory,
                analyticsEventHandler = analyticsEventHandler,
                blocksAnalytics = blocksAnalytics,
                requestedTab = requestedTab,
            )
            else -> requireNotNull(screenFactories[route.javaClass]) { "No feed screen registered for $route" }
                .get()
                .create(context = childContext, route = route)
        }
    }

    private fun onEntryRequested(request: FeedEntryRequest) {
        feedRouter.popTo(FeedHomeRoute)

        val tab = request.resolveTab(tabs = tabSet.tabs)

        // the request fell back to another section, and the filter addressed to the asked-for one would
        // otherwise stay parked with no tab ever coming to clear it
        if (tab != request.section?.tabId) feedEntryController.consumeFilter()

        tab?.let(requestedTab::tryEmit)
        expandSheetRequests.trySend(element = Unit)
        feedEntryController.consumeEntry()
    }

    private fun onSearchActivationRequest(isActive: Boolean) {
        val isSearchActive = stack.value.active.configuration is FeedRoute.Search
        when {
            isActive && !isSearchActive -> feedRouter.push(
                route = FeedRoute.Search(source = AnalyticsParam.ScreensSources.Markets.value),
            )
            !isActive && isSearchActive -> stackNavigation.pop()
        }
    }

    private fun onShtorkaExpansionChange(expansion: ShtorkaExpansion) {
        if (expansion == ShtorkaExpansion.PILL && stack.value.active.configuration is FeedRoute.Search) {
            stackNavigation.pop()
        }
    }

    private fun onChildBack(onComplete: (Boolean) -> Unit = {}) {
        when {
            stack.value.active.configuration is FeedHomeRoute -> onComplete(false)
            stack.value.backStack.isEmpty() -> router.pop(onComplete)
            else -> stackNavigation.pop(onComplete)
        }
    }

    @AssistedFactory
    interface Factory : FeedV2Component.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultFeedV2Component
    }
}

private const val SEARCH_BAR_HEADER_KEY = "searchBarHeader"

/**
 * Target of the sheet-header [AnimatedContent].
 */
@Stable
private class FeedHeaderTarget(
    val key: Any,
    val depth: Int,
    val screen: FeedScreenComponent?,
) {

    override fun equals(other: Any?): Boolean {
        return other is FeedHeaderTarget && other.key == key && other.screen === screen
    }

    override fun hashCode(): Int = 31 * key.hashCode() + System.identityHashCode(screen)
}

/**
 * Material shared-axis Y between sheet headers: the new header rises from below while the old one
 * floats up (push); pop mirrors the motion downwards. Reads as a depth change without fighting the
 * content's horizontal slide.
 */
private fun sharedAxisY(forward: Boolean): ContentTransform {
    val offsetSpec = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )
    val alphaSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

    val enter = fadeIn(animationSpec = alphaSpec) +
        slideInVertically(animationSpec = offsetSpec) { height -> if (forward) height / 2 else -height / 2 }
    val exit = fadeOut(animationSpec = alphaSpec) +
        slideOutVertically(animationSpec = offsetSpec) { height -> if (forward) -height / 2 else height / 2 }

    return enter togetherWith exit
}