package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DeployDepositWalletUseCase
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.RunPolymarketOnboardingUseCase
import com.tangem.domain.polymarket.usecase.SignOnboardingDigestsUseCase
import com.tangem.domain.polymarket.usecase.SubmitApprovalsUseCase
import com.tangem.domain.polymarket.usecase.SyncBalanceAllowanceUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketDomainModule {

    @Provides
    @Singleton
    fun provideGetPolymarketEventsUseCase(polymarketRepository: PolymarketRepository): GetPolymarketEventsUseCase {
        return GetPolymarketEventsUseCase(polymarketRepository = polymarketRepository)
    }

    @Provides
    @Singleton
    fun provideGetPolymarketCategoriesUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketCategoriesUseCase {
        return GetPolymarketCategoriesUseCase(polymarketRepository = polymarketRepository)
    }

    @Provides
    @Singleton
    fun provideCheckPolymarketGeoblockUseCase(
        polymarketRepository: PolymarketRepository,
    ): CheckPolymarketGeoblockUseCase = CheckPolymarketGeoblockUseCase(polymarketRepository = polymarketRepository)

    @Provides
    @Singleton
    fun provideGetPolymarketRelayerNonceUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketRelayerNonceUseCase = GetPolymarketRelayerNonceUseCase(
        polymarketRepository = polymarketRepository,
    )

    @Provides
    @Singleton
    fun provideDerivePolymarketAddressesUseCase(
        eoaDeriver: PolymarketEoaDeriver,
        depositWalletDeriver: PolymarketDepositWalletDeriver,
    ): DerivePolymarketAddressesUseCase = DerivePolymarketAddressesUseCase(
        eoaDeriver = eoaDeriver,
        depositWalletDeriver = depositWalletDeriver,
    )

    @Provides
    @Singleton
    fun provideGetPolymarketWalletStatusUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketWalletStatusUseCase = GetPolymarketWalletStatusUseCase(
        polymarketRepository = polymarketRepository,
    )

    @Provides
    @Singleton
    fun provideSignOnboardingDigestsUseCase(signer: PolymarketTypedDataSigner): SignOnboardingDigestsUseCase =
        SignOnboardingDigestsUseCase(signer = signer)

    @Provides
    @Singleton
    fun provideDeployDepositWalletUseCase(polymarketRepository: PolymarketRepository): DeployDepositWalletUseCase =
        DeployDepositWalletUseCase(polymarketRepository = polymarketRepository)

    @Provides
    @Singleton
    fun provideGetPolymarketApiCredentialsUseCase(
        credentialsStore: PolymarketCredentialsStore,
    ): GetPolymarketApiCredentialsUseCase = GetPolymarketApiCredentialsUseCase(
        credentialsStore = credentialsStore,
    )

    @Provides
    @Singleton
    fun provideDeriveApiCredentialsUseCase(
        polymarketRepository: PolymarketRepository,
        credentialsStore: PolymarketCredentialsStore,
    ): DeriveApiCredentialsUseCase = DeriveApiCredentialsUseCase(
        polymarketRepository = polymarketRepository,
        credentialsStore = credentialsStore,
    )

    @Provides
    @Singleton
    fun provideSubmitApprovalsUseCase(polymarketRepository: PolymarketRepository): SubmitApprovalsUseCase =
        SubmitApprovalsUseCase(polymarketRepository = polymarketRepository)

    @Provides
    @Singleton
    fun provideSyncBalanceAllowanceUseCase(polymarketRepository: PolymarketRepository): SyncBalanceAllowanceUseCase =
        SyncBalanceAllowanceUseCase(polymarketRepository = polymarketRepository)

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun provideRunPolymarketOnboardingUseCase(
        deriveAddresses: DerivePolymarketAddressesUseCase,
        getWalletStatus: GetPolymarketWalletStatusUseCase,
        getRelayerNonce: GetPolymarketRelayerNonceUseCase,
        signOnboardingDigests: SignOnboardingDigestsUseCase,
        deployDepositWallet: DeployDepositWalletUseCase,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
        deriveApiCredentials: DeriveApiCredentialsUseCase,
        submitApprovals: SubmitApprovalsUseCase,
        syncBalanceAllowance: SyncBalanceAllowanceUseCase,
    ): RunPolymarketOnboardingUseCase = RunPolymarketOnboardingUseCase(
        deriveAddresses = deriveAddresses,
        getWalletStatus = getWalletStatus,
        getRelayerNonce = getRelayerNonce,
        signOnboardingDigests = signOnboardingDigests,
        deployDepositWallet = deployDepositWallet,
        getApiCredentials = getApiCredentials,
        deriveApiCredentials = deriveApiCredentials,
        submitApprovals = submitApprovals,
        syncBalanceAllowance = syncBalanceAllowance,
    )
}