package com.tangem.features.addressbook.di

import com.tangem.features.addressbook.AddressBookComponent
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.AddressSelectorComponent
import com.tangem.features.addressbook.ContactSelectionListener
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.addressselector.DefaultAddressSelectorComponent
import com.tangem.features.addressbook.block.DefaultAddressBookContactsBlockComponent
import com.tangem.features.addressbook.common.DefaultAddressBookComponent
import com.tangem.features.addressbook.common.DefaultContactSelectionTrigger
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

    @Binds
    @Singleton
    fun bindContactsBlockComponentFactory(
        factory: DefaultAddressBookContactsBlockComponent.Factory,
    ): AddressBookContactsBlockComponent.Factory

    @Binds
    @Singleton
    fun bindAddressSelectorComponentFactory(
        factory: DefaultAddressSelectorComponent.Factory,
    ): AddressSelectorComponent.Factory

    @Binds
    @Singleton
    fun bindContactSelectionTrigger(impl: DefaultContactSelectionTrigger): ContactSelectionTrigger

    @Binds
    @Singleton
    fun bindContactSelectionListener(impl: DefaultContactSelectionTrigger): ContactSelectionListener
}