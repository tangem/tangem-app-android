package com.tangem.tap.di

import android.content.Context
import com.tangem.tap.DefaultAppRestarter
import com.tangem.utils.AppRestarter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object RestarterModule {

    @Provides
    @ActivityScoped
    fun provideAppRestarter(@ActivityContext context: Context): AppRestarter {
        return DefaultAppRestarter(context)
    }
}
