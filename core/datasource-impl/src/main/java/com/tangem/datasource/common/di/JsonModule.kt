package com.tangem.datasource.common.di

import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.utils.serialization.SerializersModuleProvider
import com.tangem.utils.serialization.aggregate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface JsonModule {

    @Multibinds
    fun serializersModuleProviders(): Set<SerializersModuleProvider>

    companion object {

        @Provides
        @Singleton
        @NetworkJson
        fun provideNetworkJson(providers: Set<@JvmSuppressWildcards SerializersModuleProvider>): Json {
            return NetworkJsonFactory.create(serializersModule = providers.aggregate())
        }
    }
}