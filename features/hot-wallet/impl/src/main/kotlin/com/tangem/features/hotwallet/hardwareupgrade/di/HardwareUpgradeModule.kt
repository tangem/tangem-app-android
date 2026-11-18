package com.tangem.features.hotwallet.hardwareupgrade.di

import com.tangem.features.hotwallet.HardwareUpgradeStoriesComponent
import com.tangem.features.hotwallet.hardwareupgrade.component.DefaultHardwareUpgradeStoriesComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HardwareUpgradeModule {

    @Binds
    @Singleton
    fun bindHardwareUpgradeStoriesComponentFactory(
        impl: DefaultHardwareUpgradeStoriesComponent.Factory,
    ): HardwareUpgradeStoriesComponent.Factory
}