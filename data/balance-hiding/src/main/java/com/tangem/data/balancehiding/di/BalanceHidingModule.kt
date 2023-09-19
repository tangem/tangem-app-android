package com.tangem.data.balancehiding.di

import android.content.Context
import com.tangem.data.balancehiding.DefaultBalanceHidingRepository
import com.tangem.data.balancehiding.DefaultDeviceFlipDetector
import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.domain.balancehiding.DeviceFlipDetector
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

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

    @Provides
    @Singleton
    fun provideFlipDetector(
        @ApplicationContext context: Context,
    ): DeviceFlipDetector {
        return DefaultDeviceFlipDetector(context = context)
    }
}
