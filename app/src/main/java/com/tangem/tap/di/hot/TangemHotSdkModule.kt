package com.tangem.tap.di.hot

import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.tap.features.hot.TangemHotSDKProxy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemHotSdkModule {

    @Binds
    @Singleton
    fun bindTangemHotSdk(proxy: TangemHotSDKProxy): TangemHotSdk
}