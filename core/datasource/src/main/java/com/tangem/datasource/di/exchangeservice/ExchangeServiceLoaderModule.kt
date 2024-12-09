package com.tangem.datasource.di.exchangeservice

import com.tangem.datasource.exchangeservice.swap.DefaultSwapServiceLoader
import com.tangem.datasource.exchangeservice.swap.SwapServiceLoader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ExchangeServiceLoaderModule {

    @Binds
    @Singleton
    fun bindSwapServiceLoader(defaultSwapServiceLoader: DefaultSwapServiceLoader): SwapServiceLoader
}