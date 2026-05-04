package com.tangem.core.analytics.di

import com.tangem.core.analytics.paramsinterceptor.SendTransactionSignerInfoInterceptor
import com.tangem.core.analytics.store.LastSignedWalletFormStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface LastSignedWalletFormStoreModule {

    @Binds
    fun bindLastSignedWalletFormStore(impl: SendTransactionSignerInfoInterceptor): LastSignedWalletFormStore
}