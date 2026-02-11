package com.tangem.feature.referral.domain.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
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
        derivePublicKeysUseCase: DerivePublicKeysUseCase,
        getUserWalletUseCase: GetUserWalletUseCase,
        addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
        manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
        singleAccountSupplier: SingleAccountSupplier,
        walletManagersFacade: WalletManagersFacade,
    ): ReferralInteractor {
        return ReferralInteractorImpl(
            repository = referralRepository,
            derivePublicKeysUseCase = derivePublicKeysUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            addCryptoCurrenciesUseCase = addCryptoCurrenciesUseCase,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            singleAccountSupplier = singleAccountSupplier,
            walletManagersFacade = walletManagersFacade,
        )
    }
}