package com.tangem.tap.di.hot

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.android.create
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object TangemHotSdkModule {

    @Provides
    @ActivityScoped
    fun provideRootAppComponentContext(@ActivityContext context: Context): TangemHotSdk {
        return TangemHotSdk.create(context as AppCompatActivity)
    }
}