package com.tangem.tap.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.arkivanov.decompose.defaultComponentContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.di.RootAppComponentContext
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageHandler
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import timber.log.Timber

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
    ): AppComponentContext {
        // TODO: Implement message handler
        val dummyMessageHandler = object : UiMessageHandler {
            override fun handleMessage(message: UiMessage) {
                Timber.w("Unable to handle message: $message")
            }
        }

        return DefaultAppComponentContext(
            componentContext = (context as AppCompatActivity).defaultComponentContext(),
            messageHandler = dummyMessageHandler,
            dispatchers = dispatchers,
            hiltComponentBuilder = componentBuilder,
        )
    }
}