package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
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
            .add(BigDecimalAdapter())
            .add(BigIntegerAdapter())
            .add(LocalDateAdapter())
            .add(DateTimeAdapter())
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
    fun provideSdkMoshi(): Moshi {
        val adapters = MoshiJsonConverter.getTangemSdkAdapters() +
            listOf(
                BigDecimalAdapter(),
                BigIntegerAdapter(),
                WalletDerivedKeysMapAdapter(),
                ScanResponseDerivedKeysMapAdapter(),
                ByteArrayKeyAdapter(),
                ExtendedPublicKeysMapAdapter(),
                CardBackupStatusAdapter(),
                DerivationPathAdapterWithMigration(),
                DateTimeAdapter(),
            )

        val typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters()

        return Moshi.Builder().apply {
            // add(VisaActivationRemoteState.jsonAdapter)
            // add(VisaCardActivationStatus.jsonAdapter)
            adapters.forEach { this.add(it) }
            typedAdapters.forEach { add(it.key, it.value) }
            addLast(KotlinJsonAdapterFactory())
        }.build()
    }

    @Provides
    @Singleton
    fun provideSdkMoshiConverter(): MoshiJsonConverter {
        return MoshiJsonConverter(
            adapters = MoshiJsonConverter.getTangemSdkAdapters() +
                listOf(
                    BigDecimalAdapter(),
                    BigIntegerAdapter(),
                    WalletDerivedKeysMapAdapter(),
                    ScanResponseDerivedKeysMapAdapter(),
                    ByteArrayKeyAdapter(),
                    ExtendedPublicKeysMapAdapter(),
                    CardBackupStatusAdapter(),
                    DerivationPathAdapterWithMigration(),
                ),
            typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters(),
        )
    }
}