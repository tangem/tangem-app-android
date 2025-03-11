package com.tangem.features.onramp.selectcountry.di

import com.tangem.features.onramp.selectcountry.DefaultSelectCountryComponent
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSelectCountryComponentModule {

    @Binds
    @Singleton
    fun bindOnrampSelectCountryComponentFactory(
        factory: DefaultSelectCountryComponent.Factory,
    ): SelectCountryComponent.Factory
}