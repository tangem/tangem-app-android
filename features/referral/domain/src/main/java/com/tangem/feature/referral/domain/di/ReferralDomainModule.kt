package com.tangem.feature.referral.domain.di

import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.ReferralInteractorImpl
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.feature.referral.domain.converter.TokensConverter
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.UserWalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ReferralDomainModule {

    @Provides
    @Singleton
    fun provideReferralInteractor(
        referralRepository: ReferralRepository,
        derivationManager: DerivationManager,
        userWalletManager: UserWalletManager,
        tokensConverter: TokensConverter,
    ): ReferralInteractor {
        return ReferralInteractorImpl(
            repository = referralRepository,
            derivationManager = derivationManager,
            userWalletManager = userWalletManager,
            tokensConverter = tokensConverter,
        )
    }
}
