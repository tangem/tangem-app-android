package com.tangem.data.pay.di

import com.tangem.data.pay.repository.DefaultOnboardingRepository
import com.tangem.data.pay.repository.DefaultTangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataProductionModule {

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: DefaultOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: DefaultTangemPayCardDetailsRepository): TangemPayCardDetailsRepository
}