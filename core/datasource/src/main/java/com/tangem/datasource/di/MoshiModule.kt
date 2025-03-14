package com.tangem.datasource.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.datasource.api.common.adapter.BigDecimalAdapter
import com.tangem.datasource.api.common.adapter.DateTimeAdapter
import com.tangem.datasource.api.common.adapter.LocalDateAdapter
import com.tangem.datasource.api.common.adapter.addStakeKitEnumFallbackAdapters
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.domain.models.scan.serialization.*
import com.tangem.domain.visa.model.VisaCardActivationStatus
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
            .add(VisaCardActivationStatus.jsonAdapter)
            .add(KotlinJsonAdapterFactory())
            .addStakeKitEnumFallbackAdapters()
            .build()
    }

    @Provides
    @Singleton
    @SdkMoshi
    fun provideSdkMoshi(): Moshi {
        val adapters = MoshiJsonConverter.getTangemSdkAdapters() +
            listOf(
                BigDecimalAdapter(),
                WalletDerivedKeysMapAdapter(),
                ScanResponseDerivedKeysMapAdapter(),
                ByteArrayKeyAdapter(),
                ExtendedPublicKeysMapAdapter(),
                CardBackupStatusAdapter(),
                DerivationPathAdapterWithMigration(),
            )

        val typedAdapters = MoshiJsonConverter.getTangemSdkTypedAdapters()

        return Moshi.Builder().apply {
            add(VisaCardActivationStatus.jsonAdapter)
            adapters.forEach { this.add(it) }
            typedAdapters.forEach { add(it.key, it.value) }
            add(KotlinJsonAdapterFactory())
        }.build()
    }

    @Provides
    @Singleton
    fun provideSdkMoshiConverter(): MoshiJsonConverter {
        return MoshiJsonConverter(
            adapters = MoshiJsonConverter.getTangemSdkAdapters() +
                listOf(
                    BigDecimalAdapter(),
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
