package com.tangem.feature.referral.domain.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.ReferralInteractorImpl
import com.tangem.feature.referral.domain.ReferralRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn

@Module
@InstallIn(ModelComponent::class)
class ReferralDomainModule {

    @Provides
    @ModelScoped
    fun provideReferralInteractor(
        referralRepository: ReferralRepository,
        manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
        singleAccountSupplier: SingleAccountSupplier,
        walletManagersFacade: WalletManagersFacade,
    ): ReferralInteractor {
        return ReferralInteractorImpl(
            repository = referralRepository,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            singleAccountSupplier = singleAccountSupplier,
            walletManagersFacade = walletManagersFacade,
        )
    }
}