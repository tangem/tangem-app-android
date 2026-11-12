package com.tangem.features.hotwallet.hardwareupgrade.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import com.tangem.features.hotwallet.hardwareupgrade.DefaultHardwareUpgradeLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn

/** Lives in the model graph, so the launcher opens the upgrade on the router of the model that launches it */
@Module
@InstallIn(ModelComponent::class)
internal interface HardwareUpgradeLauncherModule {

    @Binds
    fun bindHardwareUpgradeLauncher(impl: DefaultHardwareUpgradeLauncher): HardwareUpgradeLauncher
}