package com.tangem.data.balancehiding.di

import com.tangem.data.balancehiding.DefaultBalanceHidingRepository
import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BalanceHidingModule {

    @Provides
    @Singleton
    fun provideBalanceHidingRepository(
        balanceHidingSettingsStore: BalanceHidingSettingsStore,
    ): BalanceHidingRepository {
        return DefaultBalanceHidingRepository(
            balanceHidingSettingsStore = balanceHidingSettingsStore,
        )
    }
}
