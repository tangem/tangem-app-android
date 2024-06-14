package com.tangem.tap.di

import androidx.compose.material3.SnackbarHostState
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.core.ui.theme.AppThemeModeHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UiDependenciesModule {

    @Provides
    @Singleton
    fun provideUiDependencies(hapticManager: HapticManager, appThemeModeHolder: AppThemeModeHolder): UiDependencies {
        return object : UiDependencies {
            override val hapticManager = hapticManager
            override val appThemeModeHolder = appThemeModeHolder
            override val globalSnackbarHostState: SnackbarHostState = SnackbarHostState()
            override val eventMessageHandler: EventMessageHandler = EventMessageHandler()
        }
    }
}
