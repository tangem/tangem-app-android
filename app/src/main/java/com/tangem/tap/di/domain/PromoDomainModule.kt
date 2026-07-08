package com.tangem.tap.di.domain

import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.usecase.EnrollPromoCampaignUseCase
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PromoDomainModule {

    @Provides
    @Singleton
    fun provideGetPromoCampaignStateUseCase(repository: PromoRepository): GetPromoCampaignStateUseCase {
        return GetPromoCampaignStateUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideEnrollPromoCampaignUseCase(repository: PromoRepository): EnrollPromoCampaignUseCase {
        return EnrollPromoCampaignUseCase(repository)
    }
}