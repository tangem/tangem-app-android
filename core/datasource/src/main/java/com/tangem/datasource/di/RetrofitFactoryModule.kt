package com.tangem.datasource.di

import com.tangem.core.remote.RetrofitFactory
import com.tangem.datasource.di.utils.RetrofitApiBuilder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RetrofitFactoryModule {

    @Binds
    fun bindRetrofitFactory(impl: RetrofitApiBuilder): RetrofitFactory
}