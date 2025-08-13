package com.tangem.feature.tester.di

import android.content.Context
import com.tangem.feature.tester.presentation.navigation.DefaultTesterMenuLauncher
import com.tangem.features.tester.api.TesterMenuLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object TesterMenuLauncherModule {

    @Provides
    fun provideTesterMenuLauncher(@ApplicationContext context: Context): TesterMenuLauncher {
        return DefaultTesterMenuLauncher(context = context)
    }
}