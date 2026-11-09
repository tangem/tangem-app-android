package com.tangem.feature.tester.presentation.earnflow.ui

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.navigation.AppNavigationProvider
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Runs the real Earn flow component inside the tester, outside the app's component tree.
 *
 * The flow's dialogs go to the global message handler, which only the main activity listens to, so the host shows
 * them itself. System back goes to the flow first; when the flow does not handle it, the host closes.
 */
@Composable
internal fun TesterEarnFlowHost(params: EarnFlowComponent.Params, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, TesterEarnFlowEntryPoint::class.java)
    }

    val componentLifecycle = remember(params) { LifecycleRegistry() }
    val backDispatcher = remember(params) { BackDispatcher() }
    val hostLifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(hostLifecycleOwner, params) {
        componentLifecycle.create()
        componentLifecycle.resume()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> componentLifecycle.resume()
                Lifecycle.Event.ON_PAUSE -> componentLifecycle.pause()
                else -> Unit
            }
        }
        hostLifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            hostLifecycleOwner.lifecycle.removeObserver(observer)
            componentLifecycle.destroy()
        }
    }

    val component = remember(params) {
        val context = TesterAppComponentContext(
            componentContext = DefaultComponentContext(lifecycle = componentLifecycle, backHandler = backDispatcher),
            dispatchers = entryPoint.dispatchers(),
            hiltComponentBuilder = entryPoint.modelComponentBuilder(),
            messageSender = entryPoint.uiMessageSender(),
        )
        entryPoint.earnFlowComponentFactory().create(context = context, params = params)
    }

    BackHandler {
        if (!backDispatcher.back()) onClose()
    }

    TangemThemeRedesign {
        component.Content(modifier)
        EventMessageEffect()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TesterEarnFlowEntryPoint {

    fun earnFlowComponentFactory(): EarnFlowComponent.Factory

    fun modelComponentBuilder(): ModelComponent.Builder

    fun dispatchers(): CoroutineDispatcherProvider

    @GlobalUiMessageSender
    fun uiMessageSender(): UiMessageSender
}

/** Routing is a no-op and there is no [AppCompatActivity]: the tester runs on a plain ComponentActivity. */
private class TesterAppComponentContext(
    componentContext: ComponentContext,
    override val dispatchers: CoroutineDispatcherProvider,
    override val hiltComponentBuilder: ModelComponent.Builder,
    override val messageSender: UiMessageSender,
) : AppComponentContext, ComponentContext by componentContext {

    override val tags: HashMap<String, Any> = HashMap()

    override val componentScope: CoroutineScope =
        CoroutineScope(context = dispatchers.mainImmediate + SupervisorJob())
            .also { scope -> componentContext.lifecycle.doOnDestroy(scope::cancel) }

    override val navigationProvider: AppNavigationProvider = object : AppNavigationProvider {
        private val navigation = StackNavigation<Route>()
        override fun getOrCreate(): StackNavigation<Route> = navigation
    }

    override val router: Router = DummyRouter()

    override val activity: AppCompatActivity
        get() = error("The tester host has no AppCompatActivity")
}