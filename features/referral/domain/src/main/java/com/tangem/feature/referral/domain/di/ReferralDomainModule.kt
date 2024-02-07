package com.tangem.feature.referral.domain.di

import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.ReferralInteractorImpl
import com.tangem.feature.referral.domain.ReferralRepository
import com.tangem.lib.crypto.UserWalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ReferralDomainModule {

    @Provides
    @ViewModelScoped
    fun provideReferralInteractor(
        referralRepository: ReferralRepository,
        userWalletManager: UserWalletManager,
        derivePublicKeysUseCase: DerivePublicKeysUseCase,
        getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    ): ReferralInteractor {
        return ReferralInteractorImpl(
            repository = referralRepository,
            userWalletManager = userWalletManager,
            derivePublicKeysUseCase = derivePublicKeysUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            addCryptoCurrenciesUseCase = addCryptoCurrenciesUseCase,
        )
    }
}