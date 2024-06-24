package com.tangem.tap.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.arkivanov.decompose.defaultComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.di.RootAppComponentContext
import com.tangem.core.ui.UiDependencies
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object RootAppComponentContextModule {

    @Provides
    @ActivityScoped
    @RootAppComponentContext
    fun provideRootAppComponentContext(
        @ActivityContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
        componentBuilder: DecomposeComponent.Builder,
        uiDependencies: UiDependencies,
    ): AppComponentContext {
        return DefaultAppComponentContext(
            componentContext = (context as AppCompatActivity).defaultComponentContext(),
            messageHandler = uiDependencies.eventMessageHandler,
            dispatchers = dispatchers,
            hiltComponentBuilder = componentBuilder,
        )
    }
}
