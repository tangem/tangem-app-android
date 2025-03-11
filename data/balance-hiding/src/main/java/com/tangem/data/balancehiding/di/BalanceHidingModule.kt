package com.tangem.data.balancehiding.di

import android.content.Context
import com.tangem.data.balancehiding.DefaultBalanceHidingRepository
import com.tangem.data.balancehiding.DefaultDeviceFlipDetector
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.balancehiding.DeviceFlipDetector
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BalanceHidingModule {

    @Provides
    @Singleton
    fun provideBalanceHidingRepository(appPreferencesStore: AppPreferencesStore): BalanceHidingRepository {
        return DefaultBalanceHidingRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun provideFlipDetector(@ApplicationContext context: Context): DeviceFlipDetector {
        return DefaultDeviceFlipDetector(context = context)
    }
}