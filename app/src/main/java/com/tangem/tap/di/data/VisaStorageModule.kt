package com.tangem.tap.di.data

import com.tangem.spend.datasource.visa.storage.VisaAuthTokenStorage
import com.tangem.spend.datasource.visa.storage.VisaOTPStorage
import com.tangem.tap.data.DefaultVisaAuthTokenStorage
import com.tangem.tap.data.DefaultVisaOTPStorage
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

    @Binds
    @Singleton
    fun bindVisaOTPStorage(impl: DefaultVisaOTPStorage): VisaOTPStorage
}