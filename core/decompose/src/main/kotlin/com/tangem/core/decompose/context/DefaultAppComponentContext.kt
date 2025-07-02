package com.tangem.core.decompose.context

import androidx.appcompat.app.AppCompatActivity
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.navigation.AppNavigationProvider
import com.tangem.core.decompose.navigation.DefaultAppNavigationProvider
import com.tangem.core.decompose.navigation.DefaultRouter
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.decompose.utils.ComponentCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope

class DefaultAppComponentContext(
    componentContext: ComponentContext,
    override val dispatchers: CoroutineDispatcherProvider,
    override val hiltComponentBuilder: ModelComponent.Builder,
    override val messageSender: UiMessageSender,
    override val activity: AppCompatActivity,
    private val replaceRouter: Router? = null,
) : AppComponentContext, ComponentContext by componentContext {

    override val tags: HashMap<String, Any> = HashMap()

    override val componentScope: CoroutineScope = ComponentCoroutineScope(lifecycle, dispatchers)

    override val navigationProvider: AppNavigationProvider
        get() = instanceKeeper.getOrCreate { DefaultAppNavigationProvider() }

    override val router: Router
        get() = replaceRouter ?: instanceKeeper.getOrCreate { DefaultRouter(navigationProvider) }
}