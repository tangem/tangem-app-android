package com.tangem.tap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.utils.Provider
import dagger.hilt.android.AndroidEntryPoint
import java.util.WeakHashMap
import javax.inject.Inject

@AndroidEntryPoint
internal class DecomposeFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val component by lazy(mode = LazyThreadSafetyMode.NONE) {
        val tag = requireArguments().getString(TAG_KEY)
        val builder = componentsBuilders[tag]

        requireNotNull(builder?.build()) {
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

    private class ComponentBuilder<C : ComposableContentComponent, P : Any, F : ComponentFactory<P, C>>(
        private val contextProvider: Provider<AppComponentContext>,
        private val params: P,
        private val componentFactory: F,
    ) {

        fun build(): C = componentFactory.create(contextProvider(), params)
    }

    companion object {

        private const val TAG_KEY = "tag"

        private val componentsBuilders = WeakHashMap<String, ComponentBuilder<*, *, *>>()

        fun <C : ComposableContentComponent, P : Any, F : ComponentFactory<P, C>> newInstance(
            tag: String,
            contextProvider: Provider<AppComponentContext>,
            params: P,
            componentFactory: F,
        ): Fragment {
            this@Companion.componentsBuilders[tag] = ComponentBuilder(contextProvider, params, componentFactory)

            return DecomposeFragment().apply {
                this.arguments = bundleOf(TAG_KEY to tag)
            }
        }
    }
}
