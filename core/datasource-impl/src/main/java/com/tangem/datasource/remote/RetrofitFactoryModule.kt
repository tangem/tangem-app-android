package com.tangem.datasource.remote

import com.tangem.core.remote.RetrofitFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RetrofitFactoryModule {

    @Binds
    fun bindRetrofitFactory(impl: DefaultRetrofitFactory): RetrofitFactory
}