package com.tangem.data.visa.di

import com.tangem.data.pay.datasource.DefaultTangemPayAuthDataSource
import com.tangem.data.visa.DefaultTangemPayRemoteDataSource
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VisaDataModule {

    @Binds
    @Singleton
    fun bindTangemPayRemoteDataSource(impl: DefaultTangemPayRemoteDataSource): TangemPayRemoteDataSource

    @Binds
    @Singleton
    fun bindTangemPayAuthDataSource(repository: DefaultTangemPayAuthDataSource): TangemPayAuthDataSource
}