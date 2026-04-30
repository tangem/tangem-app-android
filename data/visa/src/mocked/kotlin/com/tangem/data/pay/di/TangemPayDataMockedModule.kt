package com.tangem.data.pay.di

import com.tangem.data.pay.repository.MockAwareOnboardingRepository
import com.tangem.data.pay.repository.MockAwareTangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataMockedModule {

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: MockAwareOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: MockAwareTangemPayCardDetailsRepository): TangemPayCardDetailsRepository
}