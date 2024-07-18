package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.datasource.api.common.adapter.*
import com.tangem.datasource.api.common.adapter.BigDecimalAdapter
import com.tangem.datasource.api.common.adapter.DateTimeAdapter
import com.tangem.datasource.api.common.adapter.LocalDateAdapter
import com.tangem.datasource.config.models.ProviderModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MoshiModule {

    @Provides
    @Singleton
    @NetworkMoshi
    fun provideNetworkMoshi(): Moshi {
        return Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(ProviderModel::class.java, "type")
                    .withSubtype(ProviderModel.Public::class.java, "public")
                    .withSubtype(ProviderModel.Private::class.java, "private")
                    .withDefaultValue(ProviderModel.UnsupportedType),
            )
            .add(BigDecimalAdapter())
            .add(LocalDateAdapter())
            .add(DateTimeAdapter())
            .add(KotlinJsonAdapterFactory())
            .addStakeKitEnumFallbackAdapters()
            .build()
    }

    @Provides
    @Singleton
    @SdkMoshi
    fun provideSdkMoshi(sdkMoshiJsonConverter: MoshiJsonConverter): Moshi {
        return sdkMoshiJsonConverter.moshi
    }

    @Provides
    @Singleton
    fun provideSdkMoshiConverter(): MoshiJsonConverter {
        return MoshiJsonConverter(
            adapters = listOf(BigDecimalAdapter()) + MoshiJsonConverter.getTangemSdkAdapters(),
            typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
        )
    }
}