package com.tangem.tap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.ComposableContentComponent
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.utils.Provider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class DecomposeFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val component by lazy(mode = LazyThreadSafetyMode.NONE) {
        requireNotNull(componentBuilder?.build()) {
            "Component builder is not set, call newInstance() for DecomposeFragment creation first."
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        component.Content(modifier)

        EventMessageEffect(
            messageHandler = uiDependencies.eventMessageHandler,
            snackbarHostState = uiDependencies.globalSnackbarHostState,
        )
    }

    private class ComponentBuilder<C : ComposableContentComponent, P : Any, F : ComponentFactory<C, P>>(
        private val contextProvider: Provider<AppComponentContext>,
        private val params: P,
        private val componentFactory: F,
    ) {

        fun build(): C = componentFactory.create(contextProvider(), params)
    }

    companion object {

        private var componentBuilder: ComponentBuilder<*, *, *>? = null

        fun <C : ComposableContentComponent, P : Any, F : ComponentFactory<C, P>> newInstance(
            contextProvider: Provider<AppComponentContext>,
            params: P,
            componentFactory: F,
        ): Fragment {
            this@Companion.componentBuilder = ComponentBuilder(contextProvider, params, componentFactory)

            return DecomposeFragment()
        }
    }
}