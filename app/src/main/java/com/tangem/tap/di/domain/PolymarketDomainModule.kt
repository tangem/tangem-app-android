package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.DefaultPredictionPositionsUpdater
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateEmitter
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateListener
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.interactor.SubmitPredictionClaimInteractor
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.BuildPredictionOrderDraftUseCase
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.domain.polymarket.usecase.DeployDepositWalletUseCase
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawDestinationsUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawalStateUseCase
import com.tangem.domain.polymarket.usecase.SearchPolymarketEventsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.SignOnboardingDigestsUseCase
import com.tangem.domain.polymarket.usecase.SubmitApprovalsUseCase
import com.tangem.domain.polymarket.usecase.SyncBalanceAllowanceUseCase
import com.tangem.domain.polymarket.usecase.ValidatePredictionOrderUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
internal object PolymarketDomainModule {

    @Provides
    @Singleton
    fun provideCreatePolymarketPositionsSessionUseCase(
        repository: PolymarketPositionsRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        updateListener: PredictionPositionsUpdateListener,
        dispatchers: CoroutineDispatcherProvider,
    ): CreatePolymarketPositionsSessionUseCase = CreatePolymarketPositionsSessionUseCase(
        repository = repository,
        deriveAddresses = deriveAddresses,
        updateListener = updateListener,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun providePredictionPositionsUpdater(): DefaultPredictionPositionsUpdater = DefaultPredictionPositionsUpdater()

    @Provides
    @Singleton
    fun providePredictionPositionsUpdateEmitter(
        impl: DefaultPredictionPositionsUpdater,
    ): PredictionPositionsUpdateEmitter = impl

    @Provides
    @Singleton
    fun providePredictionPositionsUpdateListener(
        impl: DefaultPredictionPositionsUpdater,
    ): PredictionPositionsUpdateListener = impl

    @Provides
    @Singleton
    fun provideGetPolymarketEventsBatchFlowUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPolymarketEventsBatchFlowUseCase {
        return GetPolymarketEventsBatchFlowUseCase(polymarketRepository = polymarketRepository)
    }

    @Provides
    @Singleton
    fun provideGetPolymarketCollateralCurrencyUseCase(
        collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    ): GetPolymarketCollateralCurrencyUseCase {
        return GetPolymarketCollateralCurrencyUseCase(collateralCurrencyFactory = collateralCurrencyFactory)
    }

    @Provides
    @Singleton
    fun provideGetPolymarketEventUseCase(polymarketRepository: PolymarketRepository): GetPolymarketEventUseCase {
        return GetPolymarketEventUseCase(polymarketRepository = polymarketRepository)
    }

    @Provides
    @Singleton
    fun provideSearchPolymarketEventsUseCase(
        polymarketRepository: PolymarketRepository,
    ): SearchPolymarketEventsUseCase {
        return SearchPolymarketEventsUseCase(polymarketRepository = polymarketRepository)
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
        DeployDepositWalletUseCase(
            polymarketRepository = polymarketRepository,
        )

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
    fun provideBuildPredictionOrderDraftUseCase(): BuildPredictionOrderDraftUseCase = BuildPredictionOrderDraftUseCase()

    @Provides
    @Singleton
    fun provideValidatePredictionOrderUseCase(): ValidatePredictionOrderUseCase = ValidatePredictionOrderUseCase()

    @Provides
    @Singleton
    fun provideSignPredictionOrderInteractor(
        buildDraft: BuildPredictionOrderDraftUseCase,
        validateOrder: ValidatePredictionOrderUseCase,
        signer: PolymarketTypedDataSigner,
    ): SignPredictionOrderInteractor = SignPredictionOrderInteractor(
        buildDraft = buildDraft,
        validateOrder = validateOrder,
        signer = signer,
    )

    @Provides
    @Singleton
    fun providePlacePredictionOrderUseCase(
        polymarketRepository: PolymarketRepository,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
    ): PlacePredictionOrderUseCase = PlacePredictionOrderUseCase(
        polymarketRepository = polymarketRepository,
        getApiCredentials = getApiCredentials,
    )

    @Provides
    @Singleton
    fun provideAwaitDelayedPredictionOrderUseCase(
        polymarketRepository: PolymarketRepository,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
    ): AwaitDelayedPredictionOrderUseCase = AwaitDelayedPredictionOrderUseCase(
        polymarketRepository = polymarketRepository,
        getApiCredentials = getApiCredentials,
    )

    @Provides
    @Singleton
    fun provideGetPolymarketBalanceInteractor(
        polymarketRepository: PolymarketRepository,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
    ): GetPolymarketBalanceInteractor = GetPolymarketBalanceInteractor(
        polymarketRepository = polymarketRepository,
        getApiCredentials = getApiCredentials,
    )

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun provideRunPolymarketOnboardingInteractor(
        deriveAddresses: DerivePolymarketAddressesUseCase,
        getWalletStatus: GetPolymarketWalletStatusUseCase,
        getRelayerNonce: GetPolymarketRelayerNonceUseCase,
        signOnboardingDigests: SignOnboardingDigestsUseCase,
        deployDepositWallet: DeployDepositWalletUseCase,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
        deriveApiCredentials: DeriveApiCredentialsUseCase,
        submitApprovals: SubmitApprovalsUseCase,
        syncBalanceAllowance: SyncBalanceAllowanceUseCase,
        polymarketOnboardedStore: PolymarketOnboardedStore,
        predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
    ): RunPolymarketOnboardingInteractor = RunPolymarketOnboardingInteractor(
        deriveAddresses = deriveAddresses,
        getWalletStatus = getWalletStatus,
        getRelayerNonce = getRelayerNonce,
        signOnboardingDigests = signOnboardingDigests,
        deployDepositWallet = deployDepositWallet,
        getApiCredentials = getApiCredentials,
        deriveApiCredentials = deriveApiCredentials,
        submitApprovals = submitApprovals,
        syncBalanceAllowance = syncBalanceAllowance,
        polymarketOnboardedStore = polymarketOnboardedStore,
        predictionAccountStatusFetcher = predictionAccountStatusFetcher,
    )

    @Provides
    @Singleton
    fun provideRefreshPredictionDepositRequisitesInteractor(
        bridgeRepository: PolymarketBridgeRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        requisitesStore: PredictionDepositRequisitesStore,
        depositAddressWriter: PredictionDepositAddressWriter,
    ): RefreshPredictionDepositRequisitesInteractor = RefreshPredictionDepositRequisitesInteractor(
        bridgeRepository = bridgeRepository,
        deriveAddresses = deriveAddresses,
        requisitesStore = requisitesStore,
        depositAddressWriter = depositAddressWriter,
    )

    @Provides
    @Singleton
    fun provideGetPredictionWithdrawDestinationsUseCase(
        bridgeRepository: PolymarketBridgeRepository,
        collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    ): GetPredictionWithdrawDestinationsUseCase = GetPredictionWithdrawDestinationsUseCase(
        bridgeRepository = bridgeRepository,
        collateralCurrencyFactory = collateralCurrencyFactory,
    )

    @Provides
    @Singleton
    fun provideGetPredictionWithdrawalStateUseCase(
        withdrawRepository: PolymarketWithdrawRepository,
    ): GetPredictionWithdrawalStateUseCase = GetPredictionWithdrawalStateUseCase(
        repository = withdrawRepository,
    )

    @Provides
    @Singleton
    fun provideSubmitPredictionWithdrawalInteractor(
        bridgeRepository: PolymarketBridgeRepository,
        withdrawRepository: PolymarketWithdrawRepository,
        polymarketRepository: PolymarketRepository,
        signer: PolymarketTypedDataSigner,
        store: PredictionWithdrawalStore,
        codec: PredictionTransferCallCodec,
    ): SubmitPredictionWithdrawalInteractor = SubmitPredictionWithdrawalInteractor(
        bridgeRepository = bridgeRepository,
        withdrawRepository = withdrawRepository,
        polymarketRepository = polymarketRepository,
        signer = signer,
        store = store,
        codec = codec,
    )

    @Provides
    @Singleton
    fun provideSubmitPredictionClaimInteractor(
        redeemRepository: PolymarketRedeemRepository,
        polymarketRepository: PolymarketRepository,
        signer: PolymarketTypedDataSigner,
    ): SubmitPredictionClaimInteractor = SubmitPredictionClaimInteractor(
        redeemRepository = redeemRepository,
        polymarketRepository = polymarketRepository,
        signer = signer,
    )
}