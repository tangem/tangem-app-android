package com.tangem.feature.tester.di

import android.content.Context
import com.tangem.feature.tester.presentation.navigation.DefaultTesterMenuLauncher
import com.tangem.features.tester.api.TesterMenuLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@Module
@InstallIn(ActivityComponent::class)
internal object TesterMenuLauncherModule {

    @Provides
    fun provideTesterMenuLauncher(@ActivityContext context: Context): TesterMenuLauncher {
        return DefaultTesterMenuLauncher(context)
    }
}