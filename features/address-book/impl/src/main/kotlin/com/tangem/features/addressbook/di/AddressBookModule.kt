package com.tangem.features.addressbook.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.addressbook.AddressBookFeatureToggles
import com.tangem.features.addressbook.DefaultAddressBookFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AddressBookModule {

    @Provides
    @Singleton
    fun provideAddressBookFeatureToggles(featureTogglesManager: FeatureTogglesManager): AddressBookFeatureToggles {
        return DefaultAddressBookFeatureToggles(featureTogglesManager)
    }
}