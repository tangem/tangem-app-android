package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.signer.AndroidBase64UrlCodec
import com.tangem.data.polymarket.signer.Base64UrlCodec
import com.tangem.data.polymarket.signing.DefaultPolymarketTypedDataSigner
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketSignerModule {

    @Binds
    @Singleton
    fun bindPolymarketTypedDataSigner(impl: DefaultPolymarketTypedDataSigner): PolymarketTypedDataSigner

    companion object {

        @Provides
        @Singleton
        fun provideBase64UrlCodec(): Base64UrlCodec = AndroidBase64UrlCodec()
    }
}