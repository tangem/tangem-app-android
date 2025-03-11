package com.tangem.tap.di

import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ThemeModule {

    @Provides
    @Singleton
    fun provideAppThemeModeHolder(): AppThemeModeHolder {
        return MutableAppThemeModeHolder
    }
}