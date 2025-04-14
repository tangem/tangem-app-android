package com.tangem.data.balancehiding.di

import com.tangem.data.balancehiding.DefaultBalanceHidingRepository
import com.tangem.data.balancehiding.DefaultDeviceFlipDetector
import com.tangem.domain.balancehiding.DeviceFlipDetector
import com.tangem.domain.balancehiding.repositories.BalanceHidingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BalanceHidingModule {

    @Binds
    @Singleton
    fun provideBalanceHidingRepository(impl: DefaultBalanceHidingRepository): BalanceHidingRepository

    @Binds
    @Singleton
    fun provideFlipDetector(impl: DefaultDeviceFlipDetector): DeviceFlipDetector
}