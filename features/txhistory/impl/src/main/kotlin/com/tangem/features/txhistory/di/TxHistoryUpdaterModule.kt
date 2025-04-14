package com.tangem.features.txhistory.di

import com.tangem.features.txhistory.entity.DefaultTxHistoryUpdater
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.features.txhistory.entity.TxHistoryUpdateListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TxHistoryUpdaterModule {

    @Provides
    @Singleton
    fun provideTxHistoryContentContentUpdateEmitter(impl: DefaultTxHistoryUpdater): TxHistoryContentUpdateEmitter = impl

    @Provides
    @Singleton
    fun provideTxHistoryUpdaterListener(impl: DefaultTxHistoryUpdater): TxHistoryUpdateListener = impl
}