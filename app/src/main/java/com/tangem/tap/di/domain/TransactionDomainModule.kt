package com.tangem.tap.di.domain

import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.GetDynamicReceiveAddressUseCase
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.usecase.gasless.ResolveGaslessFeePlanUseCase
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.*
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.transaction.usecase.gasless.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions", "LargeClass")
@Module
@InstallIn(SingletonComponent::class)
internal object TransactionDomainModule {

    @Provides
    @Singleton
    fun provideGetFeeUseCase(walletManagersFacade: WalletManagersFacade): GetFeeUseCase {
        return GetFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun provideGetEthSpecificFeeUseCase(walletManagersFacade: WalletManagersFacade): GetEthSpecificFeeUseCase {
        return GetEthSpecificFeeUseCase(walletManagersFacade = walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideSendTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        transactionRepository: TransactionRepository,
        walletManagersFacade: WalletManagersFacade,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
        pushNotificationsRepository: PushNotificationsRepository,
        appScope: AppCoroutineScope,
    ): SendTransactionUseCase {
        return SendTransactionUseCase(
            demoConfig = DemoConfig,
            cardSdkConfigRepository = cardSdkConfigRepository,
            transactionRepository = transactionRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            parallelUpdatingScope = appScope,
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
            pushNotificationsRepository = pushNotificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun provideSignAndBroadcastPsbtUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): SignAndBroadcastPsbtUseCase {
        return SignAndBroadcastPsbtUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            getHotTransactionSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideAssociateAssetUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): AssociateAssetUseCase {
        return AssociateAssetUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            getHotTransactionSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideRetryTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): RetryIncompleteTransactionUseCase {
        return RetryIncompleteTransactionUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            getHotTransactionSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideOpenTrustlineUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): OpenTrustlineUseCase {
        return OpenTrustlineUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            getHotTransactionSigner = tangemHotWalletSignerFactory::create,
        )
    }

    @Provides
    @Singleton
    fun provideDismissIncompleteTransactionUseCase(
        walletManagersFacade: WalletManagersFacade,
    ): DismissIncompleteTransactionUseCase {
        return DismissIncompleteTransactionUseCase(
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun provideCreateTransactionUseCase(transactionRepository: TransactionRepository): CreateTransactionUseCase {
        return CreateTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideCreateTransactionExtrasUseCase(
        transactionRepository: TransactionRepository,
    ): CreateTransactionDataExtrasUseCase {
        return CreateTransactionDataExtrasUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideEstimateFeeUseCase(walletManagersFacade: WalletManagersFacade): EstimateFeeUseCase {
        return EstimateFeeUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun provideIsFeeApproximateUseCase(feeRepository: FeeRepository): IsFeeApproximateUseCase {
        return IsFeeApproximateUseCase(feeRepository)
    }

    @Provides
    @Singleton
    fun provideValidateTransactionUseCase(transactionRepository: TransactionRepository): ValidateTransactionUseCase {
        return ValidateTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideIsSelfSendAvailableUseCase(walletManagersFacade: WalletManagersFacade): IsSelfSendAvailableUseCase {
        return IsSelfSendAvailableUseCase(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideCreateApproveTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateApprovalTransactionUseCase {
        return CreateApprovalTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetAllowanceUseCase(allowanceRepository: AllowanceRepository): GetAllowanceUseCase {
        return GetAllowanceUseCase(allowanceRepository)
    }

    @Provides
    @Singleton
    fun provideGetAllowanceInfoUseCase(allowanceRepository: AllowanceRepository): GetAllowanceInfoUseCase {
        return GetAllowanceInfoUseCase(allowanceRepository)
    }

    @Provides
    @Singleton
    fun provideCreateTransferTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateTransferTransactionUseCase {
        return CreateTransferTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun providePrepareForSendUseCase(
        transactionRepository: TransactionRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): PrepareForSendUseCase {
        return PrepareForSendUseCase(
            transactionRepository = transactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun providePrepareAndSignUseCase(
        transactionRepository: TransactionRepository,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): PrepareAndSignUseCase {
        return PrepareAndSignUseCase(
            transactionRepository = transactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun provideSignUseCase(
        walletManagersFacade: WalletManagersFacade,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): SignUseCase {
        return SignUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            walletManagersFacade = walletManagersFacade,
            getHotTransactionSigner = { tangemHotWalletSignerFactory.create(it) },
        )
    }

    @Provides
    @Singleton
    fun provideVerifySecp256k1MessagesUseCase(): VerifySecp256k1MessagesUseCase {
        return VerifySecp256k1MessagesUseCase()
    }

    @Provides
    @Singleton
    fun provideCreateNFTTransferTransactionUseCase(
        transactionRepository: TransactionRepository,
    ): CreateNFTTransferTransactionUseCase {
        return CreateNFTTransferTransactionUseCase(transactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetEnsNameUseCase(
        walletManagersFacade: WalletManagersFacade,
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): GetEnsNameUseCase {
        return GetEnsNameUseCase(
            walletManagersFacade = walletManagersFacade,
            walletAddressServiceRepository = walletAddressServiceRepository,
        )
    }

    @Provides
    @Singleton
    fun provideReceiveAddressesFactory(
        getEnsNameUseCase: GetEnsNameUseCase,
        getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
        getDynamicReceiveAddressUseCase: GetDynamicReceiveAddressUseCase,
        dynamicAddressesRepository: DynamicAddressesRepository,
        dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles,
        userWalletsListRepository: UserWalletsListRepository,
    ): ReceiveAddressesFactory {
        return ReceiveAddressesFactory(
            getEnsNameUseCase = getEnsNameUseCase,
            getViewedTokenReceiveWarningUseCase = getViewedTokenReceiveWarningUseCase,
            getDynamicReceiveAddressUseCase = getDynamicReceiveAddressUseCase,
            dynamicAddressesRepository = dynamicAddressesRepository,
            dynamicAddressesFeatureToggles = dynamicAddressesFeatureToggles,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetReverseResolvedEnsAddressUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): GetReverseResolvedEnsAddressUseCase {
        return GetReverseResolvedEnsAddressUseCase(walletAddressServiceRepository)
    }

    @Provides
    @Singleton
    fun sendLargeSolanaTransactionUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
        walletManagersFacade: WalletManagersFacade,
    ): SendLargeSolanaTransactionUseCase {
        return SendLargeSolanaTransactionUseCase(cardSdkConfigRepository, walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideGetAvailableFeeTokensUseCase(
        gaslessTransactionRepository: GaslessTransactionRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        currencyChecksRepository: CurrencyChecksRepository,
        featureTogglesManager: FeatureTogglesManager,
    ): GetAvailableFeeTokensUseCase {
        return GetAvailableFeeTokensUseCase(
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            gaslessTransactionRepository = gaslessTransactionRepository,
            currencyChecksRepository = currencyChecksRepository,
            isYieldWithdrawEnabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideResolveGaslessFeePlanUseCase(
        gaslessYieldRepository: GaslessYieldRepository,
    ): ResolveGaslessFeePlanUseCase {
        return ResolveGaslessFeePlanUseCase(gaslessYieldRepository = gaslessYieldRepository)
    }

    @Provides
    @Singleton
    fun provideGetFeeForGaslessUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        gaslessYieldRepository: GaslessYieldRepository,
        getFeeUseCase: GetFeeUseCase,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        currencyChecksRepository: CurrencyChecksRepository,
        resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
        featureTogglesManager: FeatureTogglesManager,
    ): GetFeeForGaslessUseCase {
        return GetFeeForGaslessUseCase(
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            gaslessTransactionRepository = gaslessTransactionRepository,
            gaslessYieldRepository = gaslessYieldRepository,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            getFeeUseCase = getFeeUseCase,
            currencyChecksRepository = currencyChecksRepository,
            resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
            isYieldWithdrawEnabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideIsTronGaslessSupportedUseCase(
        tronGaslessTransactionRepository: TronGaslessTransactionRepository,
    ): IsTronGaslessSupportedUseCase {
        return IsTronGaslessSupportedUseCase(
            repository = tronGaslessTransactionRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetTronGaslessFeeUseCase(
        tronGaslessTransactionRepository: TronGaslessTransactionRepository,
    ): GetTronGaslessFeeUseCase {
        return GetTronGaslessFeeUseCase(
            tronGaslessTransactionRepository = tronGaslessTransactionRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetFeeForTokenUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        gaslessYieldRepository: GaslessYieldRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        currencyChecksRepository: CurrencyChecksRepository,
        resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase,
        featureTogglesManager: FeatureTogglesManager,
    ): GetFeeForTokenUseCase {
        return GetFeeForTokenUseCase(
            gaslessTransactionRepository = gaslessTransactionRepository,
            gaslessYieldRepository = gaslessYieldRepository,
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            currencyChecksRepository = currencyChecksRepository,
            resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
            isYieldWithdrawEnabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideIsGaslessFeeSupportedForNetwork(
        currencyChecksRepository: CurrencyChecksRepository,
    ): IsGaslessFeeSupportedForNetwork {
        return IsGaslessFeeSupportedForNetwork(currencyChecksRepository)
    }

    @Provides
    @Singleton
    fun provideCreateAndSendGaslessTransactionUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        singleAccountListSupplier: SingleAccountListSupplier,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
        featureTogglesManager: FeatureTogglesManager,
    ): CreateAndSendGaslessTransactionUseCase {
        return CreateAndSendGaslessTransactionUseCase(
            walletManagersFacade = walletManagersFacade,
            singleAccountListSupplier = singleAccountListSupplier,
            gaslessTransactionRepository = gaslessTransactionRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
            isGaslessV2Enabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideEstimateFeeForTokenUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        gaslessYieldRepository: GaslessYieldRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        currencyChecksRepository: CurrencyChecksRepository,
        featureTogglesManager: FeatureTogglesManager,
    ): EstimateFeeForTokenUseCase {
        return EstimateFeeForTokenUseCase(
            gaslessTransactionRepository = gaslessTransactionRepository,
            gaslessYieldRepository = gaslessYieldRepository,
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            currencyChecksRepository = currencyChecksRepository,
            isYieldWithdrawEnabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideEstimateFeeForGaslessTxUseCase(
        walletManagersFacade: WalletManagersFacade,
        gaslessTransactionRepository: GaslessTransactionRepository,
        gaslessYieldRepository: GaslessYieldRepository,
        singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
        estimateFeeUseCase: EstimateFeeUseCase,
        currencyChecksRepository: CurrencyChecksRepository,
    ): EstimateFeeForGaslessTxUseCase {
        return EstimateFeeForGaslessTxUseCase(
            gaslessTransactionRepository = gaslessTransactionRepository,
            gaslessYieldRepository = gaslessYieldRepository,
            walletManagersFacade = walletManagersFacade,
            demoConfig = DemoConfig,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            estimateFeeUseCase = estimateFeeUseCase,
            currencyChecksRepository = currencyChecksRepository,
        )
    }

    @Provides
    @Singleton
    fun provideSignCloreMessageUseCase(
        walletManagersFacade: WalletManagersFacade,
        cardSdkConfigRepository: CardSdkConfigRepository,
        tangemHotWalletSignerFactory: TangemHotWalletSigner.Factory,
    ): SignCloreMessageUseCase {
        return SignCloreMessageUseCase(
            walletManagersFacade = walletManagersFacade,
            cardSdkConfigRepository = cardSdkConfigRepository,
            getHotWalletSigner = tangemHotWalletSignerFactory::create,
        )
    }
}