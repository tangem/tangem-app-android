package com.tangem.features.addressbook.di

import com.tangem.features.addressbook.AddressBookComponent
import com.tangem.features.addressbook.common.DefaultAddressBookComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AddressBookComponentModule {

    @Binds
    @Singleton
    fun bindAddressBookComponentFactory(factory: DefaultAddressBookComponent.Factory): AddressBookComponent.Factory
}