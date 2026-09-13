package com.tangem.feature.tester.presentation.storybook.page.storiesv2

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
import com.tangem.features.storiesv2.StoriesV2StorybookComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Runs a real Decompose component inside the storybook.
 *
 * The storybook is not part of the app's component tree, so the context a component expects has to be assembled by
 * hand from the singleton graph. The lifecycle is mirrored from the hosting composition, which is what makes the
 * demo behave like the real screen: leaving the app pauses playback and coming back resumes it, exactly as it does
 * in the flow this player will ship in.
 */
@Composable
internal fun StoriesV2DemoHost(modifier: Modifier = Modifier) {
    val appContext = LocalContext.current.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, StoriesV2StorybookEntryPoint::class.java)
    }

    val componentLifecycle = remember { LifecycleRegistry() }
    val hostLifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(hostLifecycleOwner) {
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

    val component = remember {
        val context = StorybookAppComponentContext(
            componentContext = DefaultComponentContext(lifecycle = componentLifecycle),
            dispatchers = entryPoint.dispatchers(),
            hiltComponentBuilder = entryPoint.modelComponentBuilder(),
            messageSender = entryPoint.uiMessageSender(),
        )
        entryPoint.storybookComponentFactory().create(context = context, params = Unit)
    }

    component.Content(modifier)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface StoriesV2StorybookEntryPoint {

    fun storybookComponentFactory(): StoriesV2StorybookComponent.Factory

    fun modelComponentBuilder(): ModelComponent.Builder

    fun dispatchers(): CoroutineDispatcherProvider

    @GlobalUiMessageSender
    fun uiMessageSender(): UiMessageSender
}

/**
 * The pieces of an app component context that exist outside the app's navigation tree.
 *
 * Routing is a no-op — the storybook has nowhere to route to — and there is no [AppCompatActivity], because the
 * tester runs on a plain ComponentActivity. Rather than fabricating one, the property fails loudly: a component
 * that genuinely needs an activity cannot be demoed here, and that should be obvious the first time it is tried.
 */
private class StorybookAppComponentContext(
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
        get() = error("The storybook host has no AppCompatActivity")
}