package com.tangem.tap.di.domain

import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.usecase.EnrollPromoCampaignUseCase
import com.tangem.domain.promo.usecase.GetPromoCampaignStateUseCase
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.promo.usecase.RegisterPromoCampaignUseCase
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

    @Provides
    @Singleton
    fun provideGetPromoCampaignsUseCase(repository: PromoRepository): GetPromoCampaignsUseCase {
        return GetPromoCampaignsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideRegisterPromoCampaignUseCase(repository: PromoRepository): RegisterPromoCampaignUseCase {
        return RegisterPromoCampaignUseCase(repository)
    }
}