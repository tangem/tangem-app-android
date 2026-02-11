package com.tangem.tap.di.core.ui

import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.HoldToConfirmButtonFeatureToggles
import com.tangem.tap.core.ui.DefaultDesignFeatureToggles
import com.tangem.tap.core.ui.DefaultHoldToConfirmButtonFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CoreUiBindsModule {

    @Binds
    fun bindDesignFeatureToggles(impl: DefaultDesignFeatureToggles): DesignFeatureToggles

    @Binds
    fun bindHoldToConfirmButtonFeatureToggles(
        impl: DefaultHoldToConfirmButtonFeatureToggles,
    ): HoldToConfirmButtonFeatureToggles
}