package com.tangem.feature.tester.di

import android.content.Context
import com.tangem.feature.tester.ActivityClassWrapper
import com.tangem.feature.tester.apprestarter.DefaultAppRestarter
import com.tangem.features.tester.api.AppRestarter
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
    fun provideAppRestarter(
        @ActivityContext context: Context,
        activityClassWrapper: ActivityClassWrapper,
    ): AppRestarter {
        return DefaultAppRestarter(context, activityClassWrapper)
    }
}