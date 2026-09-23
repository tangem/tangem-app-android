package com.tangem.datasource.common.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.core.local.moshi.SdkMoshi
import com.tangem.core.local.moshi.SdkMoshiConfigurer
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.datasource.common.*
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
        val builder = Moshi.Builder().apply {
            commonAdapters().forEach(::add)
            addLast(KotlinJsonAdapterFactory())
        }

        // Both the card SDK adapters and the card model adapters (e.g. ScanResponse) are contributed via
        // @IntoSet by the modules that own them, so this builder depends on neither the card SDK nor
        // domain models.
        return configurers.fold(builder) { acc, configurer -> configurer.configure(acc) }.build()
    }

    /** Adapters shared by both the network and the SDK [Moshi] instances. */
    private fun commonAdapters(): List<Any> = listOf(
        BigDecimalAdapter(),
        BigIntegerAdapter(),
        DateTimeAdapter(),
    )
}