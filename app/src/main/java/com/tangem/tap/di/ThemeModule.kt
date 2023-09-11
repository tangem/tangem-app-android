package com.tangem.tap.di

import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
internal object ThemeModule {

    @Provides
    fun provideAppThemeModeHolder(): AppThemeModeHolder {
        return MutableAppThemeModeHolder
    }
}