package com.tangem.sdk.api.di

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.local.moshi.SdkMoshiConfigurer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Contributes the Tangem card SDK adapters to the shared SDK Moshi, so the central provider does not
 * depend on the card SDK ([MoshiJsonConverter]) directly.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TangemSdkMoshiModule {

    @Provides
    @IntoSet
    fun provideTangemSdkMoshiConfigurer(): SdkMoshiConfigurer = SdkMoshiConfigurer { builder ->
        builder.apply {
            MoshiJsonConverter.getTangemSdkAdapters().forEach { add(it) }
            MoshiJsonConverter.getTangemSdkTypedAdapters().forEach { (type, adapter) -> add(type, adapter) }
        }
    }
}