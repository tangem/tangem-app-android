package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketCredentialsLock
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketDepositWalletLock
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
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
import com.tangem.domain.polymarket.interactor.ManagePolymarketSessionKeyInteractor
import com.tangem.domain.polymarket.interactor.ReconcilePolymarketSessionKeysInteractor
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.interactor.RunPolymarketOnboardingInteractor
import com.tangem.domain.polymarket.interactor.SignPolymarketSessionKeyBatchesInteractor
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.interactor.SubmitPredictionClaimInteractor
import com.tangem.domain.polymarket.interactor.SubmitPredictionOrderInteractor
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import com.tangem.domain.polymarket.signing.PolymarketSigningSession
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.BuildPredictionOrderDraftUseCase
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.domain.polymarket.usecase.CreatePolymarketActivitySessionUseCase
import com.tangem.domain.polymarket.usecase.DeployDepositWalletUseCase
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetAllPolymarketActivityUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawDestinationsUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawalStateUseCase
import com.tangem.domain.polymarket.usecase.InvalidatePolymarketCredentialsUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketCredentialsPresenceUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.SearchPolymarketEventsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.usecase.SubmitApprovalsUseCase
import com.tangem.domain.polymarket.usecase.SyncBalanceAllowanceUseCase
import com.tangem.domain.polymarket.usecase.ValidatePredictionOrderUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions", "LargeClass")
internal object PolymarketDomainModule {

    @Provides
    fun provideObservePolymarketLiveQuotesUseCase(
        repository: PolymarketLiveQuotesRepository,
    ): ObservePolymarketLiveQuotesUseCase = ObservePolymarketLiveQuotesUseCase(repository)

    @Provides
    @Singleton
    fun provideCreatePolymarketActivitySessionUseCase(
        repository: PolymarketActivityRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        dispatchers: CoroutineDispatcherProvider,
    ): CreatePolymarketActivitySessionUseCase = CreatePolymarketActivitySessionUseCase(
        repository = repository,
        deriveAddresses = deriveAddresses,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun provideGetAllPolymarketActivityUseCase(
        repository: PolymarketActivityRepository,
        deriveAddresses: DerivePolymarketAddressesUseCase,
    ): GetAllPolymarketActivityUseCase = GetAllPolymarketActivityUseCase(
        repository = repository,
        deriveAddresses = deriveAddresses,
    )

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
    fun provideInvalidatePolymarketCredentialsUseCase(
        credentialsStore: PolymarketCredentialsStore,
    ): InvalidatePolymarketCredentialsUseCase = InvalidatePolymarketCredentialsUseCase(
        credentialsStore = credentialsStore,
    )

    @Provides
    @Singleton
    fun provideObservePolymarketCredentialsPresenceUseCase(
        credentialsStore: PolymarketCredentialsStore,
    ): ObservePolymarketCredentialsPresenceUseCase = ObservePolymarketCredentialsPresenceUseCase(
        credentialsStore = credentialsStore,
    )

    @Provides
    @Singleton
    fun provideDeriveApiCredentialsUseCase(
        polymarketRepository: PolymarketRepository,
        credentialsStore: PolymarketCredentialsStore,
        credentialsLock: PolymarketCredentialsLock,
    ): DeriveApiCredentialsUseCase = DeriveApiCredentialsUseCase(
        polymarketRepository = polymarketRepository,
        credentialsStore = credentialsStore,
        credentialsLock = credentialsLock,
    )

    @Provides
    @Singleton
    fun providePolymarketCredentialsLock(): PolymarketCredentialsLock = PolymarketCredentialsLock()

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
        invalidateCredentials: InvalidatePolymarketCredentialsUseCase,
        credentialsLock: PolymarketCredentialsLock,
        sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    ): PlacePredictionOrderUseCase = PlacePredictionOrderUseCase(
        polymarketRepository = polymarketRepository,
        getApiCredentials = getApiCredentials,
        invalidateCredentials = invalidateCredentials,
        credentialsLock = credentialsLock,
        sessionCredentialsProvider = sessionCredentialsProvider,
    )

    @Provides
    @Singleton
    fun providePolymarketOrderSigningRouter(
        polymarketRepository: PolymarketRepository,
        sessionKeyStore: PolymarketSessionKeyStore,
        tradingStore: PolymarketSessionKeyTradingStore,
        sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    ): PolymarketOrderSigningRouter = PolymarketOrderSigningRouter(
        polymarketRepository = polymarketRepository,
        sessionKeyStore = sessionKeyStore,
        tradingStore = tradingStore,
        sessionCredentialsProvider = sessionCredentialsProvider,
    )

    @Provides
    @Singleton
    fun provideSubmitPredictionOrderInteractor(
        signOrder: SignPredictionOrderInteractor,
        placeOrder: PlacePredictionOrderUseCase,
        router: PolymarketOrderSigningRouter,
        stateService: PolymarketSessionKeyStateService,
    ): SubmitPredictionOrderInteractor = SubmitPredictionOrderInteractor(
        signOrder = signOrder,
        placeOrder = placeOrder,
        router = router,
        stateService = stateService,
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
        invalidateCredentials: InvalidatePolymarketCredentialsUseCase,
        credentialsLock: PolymarketCredentialsLock,
    ): GetPolymarketBalanceInteractor = GetPolymarketBalanceInteractor(
        polymarketRepository = polymarketRepository,
        getApiCredentials = getApiCredentials,
        invalidateCredentials = invalidateCredentials,
        credentialsLock = credentialsLock,
    )

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun provideRunPolymarketOnboardingInteractor(
        signingSession: PolymarketSigningSession,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        getWalletStatus: GetPolymarketWalletStatusUseCase,
        getRelayerNonce: GetPolymarketRelayerNonceUseCase,
        deployDepositWallet: DeployDepositWalletUseCase,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
        deriveApiCredentials: DeriveApiCredentialsUseCase,
        submitApprovals: SubmitApprovalsUseCase,
        syncBalanceAllowance: SyncBalanceAllowanceUseCase,
        polymarketOnboardedStore: PolymarketOnboardedStore,
        predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
    ): RunPolymarketOnboardingInteractor = RunPolymarketOnboardingInteractor(
        signingSession = signingSession,
        deriveAddresses = deriveAddresses,
        getWalletStatus = getWalletStatus,
        getRelayerNonce = getRelayerNonce,
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
        depositWalletLock: PolymarketDepositWalletLock,
    ): SubmitPredictionWithdrawalInteractor = SubmitPredictionWithdrawalInteractor(
        bridgeRepository = bridgeRepository,
        withdrawRepository = withdrawRepository,
        polymarketRepository = polymarketRepository,
        signer = signer,
        store = store,
        codec = codec,
        depositWalletLock = depositWalletLock,
    )

    @Provides
    @Singleton
    fun provideSubmitPredictionClaimInteractor(
        redeemRepository: PolymarketRedeemRepository,
        polymarketRepository: PolymarketRepository,
        positionsRepository: PolymarketPositionsRepository,
        signer: PolymarketTypedDataSigner,
        depositWalletLock: PolymarketDepositWalletLock,
    ): SubmitPredictionClaimInteractor = SubmitPredictionClaimInteractor(
        redeemRepository = redeemRepository,
        polymarketRepository = polymarketRepository,
        positionsRepository = positionsRepository,
        signer = signer,
        depositWalletLock = depositWalletLock,
    )

    @Provides
    @Singleton
    fun providePolymarketDepositWalletLock(): PolymarketDepositWalletLock = PolymarketDepositWalletLock()

    @Provides
    @Singleton
    fun provideSignPolymarketSessionKeyBatchesInteractor(
        signingSession: PolymarketSigningSession,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        getRelayerNonce: GetPolymarketRelayerNonceUseCase,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
    ): SignPolymarketSessionKeyBatchesInteractor = SignPolymarketSessionKeyBatchesInteractor(
        signingSession = signingSession,
        deriveAddresses = deriveAddresses,
        getRelayerNonce = getRelayerNonce,
        getApiCredentials = getApiCredentials,
    )

    @Provides
    @Singleton
    fun provideManagePolymarketSessionKeyInteractor(
        signBatches: SignPolymarketSessionKeyBatchesInteractor,
        sessionKeyRepository: PolymarketSessionKeyRepository,
        polymarketRepository: PolymarketRepository,
        sessionKeyStore: PolymarketSessionKeyStore,
        deviceIdProvider: PolymarketDeviceIdProvider,
        sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
        tradingStore: PolymarketSessionKeyTradingStore,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
        deriveApiCredentials: DeriveApiCredentialsUseCase,
        depositWalletLock: PolymarketDepositWalletLock,
    ): ManagePolymarketSessionKeyInteractor = ManagePolymarketSessionKeyInteractor(
        signBatches = signBatches,
        sessionKeyRepository = sessionKeyRepository,
        polymarketRepository = polymarketRepository,
        sessionKeyStore = sessionKeyStore,
        deviceIdProvider = deviceIdProvider,
        sessionCredentialsProvider = sessionCredentialsProvider,
        tradingStore = tradingStore,
        getApiCredentials = getApiCredentials,
        deriveApiCredentials = deriveApiCredentials,
        depositWalletLock = depositWalletLock,
    )

    @Provides
    @Singleton
    fun provideReconcilePolymarketSessionKeysInteractor(
        polymarketRepository: PolymarketRepository,
        sessionKeyRepository: PolymarketSessionKeyRepository,
        sessionKeyStore: PolymarketSessionKeyStore,
        deviceIdProvider: PolymarketDeviceIdProvider,
        tradingStore: PolymarketSessionKeyTradingStore,
        deriveAddresses: DerivePolymarketAddressesUseCase,
        getApiCredentials: GetPolymarketApiCredentialsUseCase,
    ): ReconcilePolymarketSessionKeysInteractor = ReconcilePolymarketSessionKeysInteractor(
        polymarketRepository = polymarketRepository,
        sessionKeyRepository = sessionKeyRepository,
        sessionKeyStore = sessionKeyStore,
        deviceIdProvider = deviceIdProvider,
        tradingStore = tradingStore,
        deriveAddresses = deriveAddresses,
        getApiCredentials = getApiCredentials,
    )

    @Provides
    @Singleton
    fun providePolymarketSessionKeyStateService(
        reconcile: ReconcilePolymarketSessionKeysInteractor,
        appCoroutineScope: AppCoroutineScope,
    ): PolymarketSessionKeyStateService = PolymarketSessionKeyStateService(
        reconcile = reconcile,
        scope = appCoroutineScope,
    )
}