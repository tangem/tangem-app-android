package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.signer.AndroidBase64UrlCodec
import com.tangem.data.polymarket.signer.Base64UrlCodec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketSignerModule {

    @Provides
    @Singleton
    fun provideBase64UrlCodec(): Base64UrlCodec = AndroidBase64UrlCodec()
}