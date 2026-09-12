package com.tangem.features.tangempay.eligibility

import com.tangem.features.tangempay.eligibility.country.DefaultTangemPayCountrySelectorComponent
import com.tangem.features.tangempay.eligibility.waitlist.DefaultTangemPayWaitlistComponent
import com.tangem.features.tangempay.eligibility.wallet.DefaultTangemPayWalletSelectorComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayEligibilityFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultTangemPayEligibilityComponent.Factory): TangemPayEligibilityComponent.Factory

    @Binds
    fun bindCountrySelectorFactory(
        impl: DefaultTangemPayCountrySelectorComponent.Factory,
    ): TangemPayCountrySelectorComponent.Factory

    @Binds
    fun bindWaitlistFactory(impl: DefaultTangemPayWaitlistComponent.Factory): TangemPayWaitlistComponent.Factory

    @Binds
    fun bindWalletSelectorFactory(
        impl: DefaultTangemPayWalletSelectorComponent.Factory,
    ): TangemPayWalletSelectorComponent.Factory
}