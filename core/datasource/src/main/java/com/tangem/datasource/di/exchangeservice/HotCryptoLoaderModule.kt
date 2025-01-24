package com.tangem.datasource.di.exchangeservice

import com.tangem.datasource.exchangeservice.hotcrypto.DefaultHotCryptoLoader
import com.tangem.datasource.exchangeservice.hotcrypto.HotCryptoLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HotCryptoLoaderModule {

    @Binds
    @Singleton
    fun bindExpressServiceLoader(defaultHotCryptoLoader: DefaultHotCryptoLoader): HotCryptoLoader
}