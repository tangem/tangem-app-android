package com.tangem.features.txhistory.di

import com.tangem.features.txhistory.component.DefaultTxHistoryComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TxHistoryFeatureModule {

    @Binds
    @Singleton
    fun bindComponentFactory(factory: DefaultTxHistoryComponent.Factory): TxHistoryComponent.Factory
}