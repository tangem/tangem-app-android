package com.tangem.tap.di.domain

import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.status.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.account.usecase.*
import com.tangem.feature.referral.data.ExternalReferralRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDomainModule {

    @Provides
    @Singleton
    fun provideAddCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
        singleAccountListFetcher: SingleAccountListFetcher,
        mainAccountTokensMigration: MainAccountTokensMigration,
    ): AddCryptoPortfolioUseCase {
        return AddCryptoPortfolioUseCase(
            crudRepository = accountsCRUDRepository,
            singleAccountListFetcher = singleAccountListFetcher,
            mainAccountTokensMigration = mainAccountTokensMigration,
        )
    }

    @Provides
    @Singleton
    fun provideUpdateCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): UpdateCryptoPortfolioUseCase {
        return UpdateCryptoPortfolioUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideArchiveCryptoPortfolioUseCase(
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        accountsCRUDRepository: AccountsCRUDRepository,
        referralRepository: ExternalReferralRepository,
    ): ArchiveCryptoPortfolioUseCase {
        return ArchiveCryptoPortfolioUseCase(
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            crudRepository = accountsCRUDRepository,
            referralRepository = referralRepository,
        )
    }

    @Provides
    @Singleton
    fun provideRecoverCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
        mainAccountTokensMigration: MainAccountTokensMigration,
        cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    ): RecoverCryptoPortfolioUseCase {
        return RecoverCryptoPortfolioUseCase(
            crudRepository = accountsCRUDRepository,
            mainAccountTokensMigration = mainAccountTokensMigration,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        )
    }

    @Provides
    @Singleton
    fun provideGetArchivedAccountsUseCase(accountsCRUDRepository: AccountsCRUDRepository): GetArchivedAccountsUseCase {
        return GetArchivedAccountsUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideGetUnoccupiedAccountIndexUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): GetUnoccupiedAccountIndexUseCase {
        return GetUnoccupiedAccountIndexUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideIsAccountsModeEnabledUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
        accountsFeatureToggles: AccountsFeatureToggles,
    ): IsAccountsModeEnabledUseCase {
        return IsAccountsModeEnabledUseCase(
            crudRepository = accountsCRUDRepository,
            accountsFeatureToggles = accountsFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideApplyAccountListSortingUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyAccountListSortingUseCase {
        return ApplyAccountListSortingUseCase(
            accountsCRUDRepository = accountsCRUDRepository,
            dispatchers = dispatchers,
        )
    }
}