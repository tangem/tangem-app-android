package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.local.moshi.SdkMoshi
import com.tangem.core.local.moshi.SdkMoshiConfigurer
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.datasource.api.common.adapter.*
import com.tangem.datasource.utils.SerializeNullsFactory
import com.tangem.domain.models.scan.serialization.*
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
    fun provideNetworkMoshi(configurers: Set<@JvmSuppressWildcards NetworkMoshiConfigurer>): Moshi {
        val builder = Moshi.Builder()
            .add(SerializeNullsFactory)
            .apply { commonAdapters().forEach(::add) }
            .add(LocalDateAdapter())
            // .add(VisaActivationRemoteState.jsonAdapter)
            // .add(VisaCardActivationStatus.jsonAdapter)
            .addLast(KotlinJsonAdapterFactory())

        // Enum-fallback (and future) adapters are contributed via @IntoSet so stream modules can register
        // their own without this central builder depending on them.
        return configurers.fold(builder) { acc, configurer -> configurer.configure(acc) }.build()
    }

    @Provides
    @Singleton
    @SdkMoshi
    fun provideSdkMoshi(configurers: Set<@JvmSuppressWildcards SdkMoshiConfigurer>): Moshi {
        val adapters = MoshiJsonConverter.getTangemSdkAdapters() + commonAdapters()
        val typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters()

        val builder = Moshi.Builder().apply {
            adapters.forEach { this.add(it) }
            typedAdapters.forEach { add(it.key, it.value) }
            addLast(KotlinJsonAdapterFactory())
        }

        // Card model adapters (e.g. ScanResponse) are contributed via @IntoSet by the module that
        // (de)serializes them through this Moshi, so this builder doesn't depend on domain models.
        return configurers.fold(builder) { acc, configurer -> configurer.configure(acc) }.build()
    }

    @Provides
    @Singleton
    fun provideSdkMoshiConverter(): MoshiJsonConverter {
        return MoshiJsonConverter(
            adapters = MoshiJsonConverter.getTangemSdkAdapters() +
                listOf(BigDecimalAdapter(), BigIntegerAdapter()) +
                scanResponseMoshiAdapters(),
            typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
        )
    }

    /** Adapters shared by both the network and the SDK [Moshi] instances. */
    private fun commonAdapters(): List<Any> = listOf(
        BigDecimalAdapter(),
        BigIntegerAdapter(),
        DateTimeAdapter(),
    )
}