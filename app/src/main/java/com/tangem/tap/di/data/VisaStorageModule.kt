package com.tangem.tap.di.data

import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.tap.data.DefaultVisaAuthTokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VisaStorageModule {

    @Binds
    @Singleton
    fun bindVisaStorage(impl: DefaultVisaAuthTokenStorage): VisaAuthTokenStorage
}