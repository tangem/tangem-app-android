package com.tangem.features.addressbook.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.addressbook.list.model.AddressBookListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AddressBookModelModule {

    @Binds
    @IntoMap
    @ClassKey(AddressBookListModel::class)
    fun bindAddressBookModel(model: AddressBookListModel): Model
}