package com.tangem.data.pay.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.pay.DefaultTangemPayEligibilityManager
import com.tangem.data.pay.TangemPayUserWalletDataCleaner
import com.tangem.data.pay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.data.pay.entity.DefaultTangemPayCurrencyFactory
import com.tangem.data.pay.flow.DefaultPaymentAccountStatusFetcher
import com.tangem.data.pay.flow.DefaultPaymentAccountStatusProducer
import com.tangem.data.pay.repository.*
import com.tangem.data.pay.store.CashbackSummaryStore
import com.tangem.data.pay.store.DefaultPaymentAccountRecentsStore
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.data.pay.usecase.DefaultGetTangemPayCurrencyStatusUseCase
import com.tangem.data.pay.usecase.DefaultGetTangemPayCustomerIdUseCase
import com.tangem.data.pay.usecase.DefaultTangemPayWithdrawUseCase
import com.tangem.data.pay.usecase.DefaultTangemPayWithdrawWithSwapUseCase
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.spend.datasource.pay.store.entity.CashbackSummaryDM
import com.tangem.spend.datasource.visa.entity.PaymentAccountStatusValueDM
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.*
import com.tangem.domain.pay.usecase.*
import com.tangem.domain.pay.usecase.bankofframp.AwaitBankOfframpSignatureUseCase
import com.tangem.domain.pay.usecase.bankofframp.CancelBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.FindActiveBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetRecentBankAccountsUseCase
import com.tangem.domain.pay.usecase.bankofframp.SignBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.recents.GetRecentTokenTransfersUseCase
import com.tangem.domain.pay.usecase.recents.RecordTokenTransferFromSendUseCase
import com.tangem.domain.pay.usecase.recents.RecordTokenTransferFromSwapUseCase
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawWithSwapUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataModule {

    @Binds
    @Singleton
    fun bindKycRepository(repository: DefaultKycRepository): KycRepository

    @Binds
    @Singleton
    fun bindTangemPaySwapRepository(repository: DefaultTangemPayWithdrawRepository): TangemPayWithdrawRepository

    @Binds
    @Singleton
    fun bindReissueCardRepository(repository: DefaultReissueCardRepository): TangemPayReissueCardRepository

    @Binds
    @Singleton
    fun bindCloseCardRepository(repository: DefaultCloseCardRepository): TangemPayCloseCardRepository

    @Binds
    @Singleton
    fun bindIssueCardRepository(repository: DefaultIssueCardRepository): TangemPayIssueCardRepository

    @Binds
    @Singleton
    fun bindTangemPayCryptoCurrencyFactory(factory: DefaultTangemPayCurrencyFactory): TangemPayCurrencyFactory

    @Binds
    @Singleton
    fun bindGetTangemPayCurrencyStatusUseCase(
        impl: DefaultGetTangemPayCurrencyStatusUseCase,
    ): GetTangemPayCurrencyStatusUseCase

    @Binds
    @Singleton
    fun bindTangemPayWithdrawWithSwapUseCase(
        impl: DefaultTangemPayWithdrawWithSwapUseCase,
    ): TangemPayWithdrawWithSwapUseCase

    @Binds
    @Singleton
    fun bindTangemPayWithdrawUseCase(impl: DefaultTangemPayWithdrawUseCase): TangemPayWithdrawUseCase

    @Binds
    @Singleton
    fun bindGetTangemPayCustomerIdUseCase(impl: DefaultGetTangemPayCustomerIdUseCase): GetTangemPayCustomerIdUseCase

    @Binds
    @Singleton
    fun bindTangemPayEligibilityManager(impl: DefaultTangemPayEligibilityManager): TangemPayEligibilityManager

    @Binds
    @Singleton
    fun bindPaymentAccountStatusProducerFactory(
        impl: DefaultPaymentAccountStatusProducer.Factory,
    ): PaymentAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindPaymentAccountStatusFetcher(impl: DefaultPaymentAccountStatusFetcher): PaymentAccountStatusFetcher

    @Binds
    @IntoSet
    fun bindTangemPayUserWalletDataCleaner(impl: TangemPayUserWalletDataCleaner): UserWalletDataCleaner

    @Binds
    @Singleton
    fun bindTariffPlanTransitionsRepository(
        repository: DefaultTariffPlanTransitionsRepository,
    ): TangemPayTariffPlanTransitionsRepository

    @Binds
    @Singleton
    fun bindWaitlistRepository(repository: DefaultTangemPayWaitlistRepository): TangemPayWaitlistRepository

    @Suppress("TooManyFunctions", "LargeClass")
    companion object {

        @Provides
        fun provideGetTangemPayTariffPlanTransitionsUseCase(
            repository: TangemPayTariffPlanTransitionsRepository,
        ): GetTangemPayTariffPlanTransitionsUseCase {
            return GetTangemPayTariffPlanTransitionsUseCase(repository)
        }

        @Provides
        fun provideSetTariffPlanPendingTransitionUseCase(
            repository: TangemPayTariffPlanTransitionsRepository,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): SetTariffPlanPendingTransitionUseCase {
            return SetTariffPlanPendingTransitionUseCase(
                repository = repository,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            )
        }

        @Provides
        fun provideCancelTariffPlanPendingTransitionUseCase(
            repository: TangemPayTariffPlanTransitionsRepository,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): CancelTariffPlanPendingTransitionUseCase {
            return CancelTariffPlanPendingTransitionUseCase(
                repository = repository,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            )
        }

        @Provides
        fun provideGetTangemPayTariffPlanStateUseCase(
            customerOrderRepository: CustomerOrderRepository,
            getTangemPayTariffPlanTransitionsUseCase: GetTangemPayTariffPlanTransitionsUseCase,
        ): GetTangemPayTariffPlanStateUseCase {
            return GetTangemPayTariffPlanStateUseCase(
                customerOrderRepository = customerOrderRepository,
                getTariffPlanTransitions = getTangemPayTariffPlanTransitionsUseCase,
            )
        }

        @Provides
        @Singleton
        fun providePaymentAccountStatusesStore(
            @NetworkMoshi moshi: Moshi,
            @ApplicationContext context: Context,
            dispatchers: CoroutineDispatcherProvider,
            scope: AppCoroutineScope,
            converter: PaymentAccountStatusValueDMConverter,
            dataStoreFactory: AppDataStoreFactory,
        ): PaymentAccountStatusesStore {
            return PaymentAccountStatusesStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = dataStoreFactory.create(
                    serializer = MoshiDataStoreSerializer(
                        moshi = moshi,
                        types = mapWithStringKeyTypes<PaymentAccountStatusValueDM>(),
                        defaultValue = emptyMap(),
                    ),
                    produceFile = { context.dataStoreFile(fileName = "payment_account_statuses") },
                    scope = scope,
                ),
                converter = converter,
                scope = scope,
            )
        }

        @Provides
        @Singleton
        fun provideCashbackSummaryStore(dataStoreFactory: AppDataStoreFactory): CashbackSummaryStore {
            return CashbackSummaryStore(
                dataStore = dataStoreFactory.create(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(String.serializer(), CashbackSummaryDM.serializer()),
                    fileName = "tangem_pay_cashback_summary",
                ),
            )
        }

        @Provides
        @Singleton
        fun providePaymentAccountStatusSupplier(
            factory: PaymentAccountStatusProducer.Factory,
        ): PaymentAccountStatusSupplier {
            return object : PaymentAccountStatusSupplier(
                factory = factory,
                keyCreator = { "payment_account_status_${it.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        fun provideSetTangemPayCardLimitUseCase(
            cardDetailsRepository: TangemPayCardDetailsRepository,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): SetTangemPayCardLimitUseCase {
            return SetTangemPayCardLimitUseCase(cardDetailsRepository, paymentAccountStatusFetcher)
        }

        @Provides
        fun provideUpdateTangemPayCardNameUseCase(
            cardDetailsRepository: TangemPayCardDetailsRepository,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): UpdateTangemPayCardNameUseCase {
            return UpdateTangemPayCardNameUseCase(cardDetailsRepository, paymentAccountStatusFetcher)
        }

        @Provides
        @Singleton
        fun provideGetTangemPayCryptoCurrencyStatusUseCase(
            paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
        ): GetPaymentAccountCryptoCurrencyStatusUseCase {
            return GetPaymentAccountCryptoCurrencyStatusUseCase(paymentAccountStatusSupplier)
        }

        @Provides
        @Singleton
        fun provideProduceTangemPayInitialDataUseCase(
            repository: OnboardingRepository,
        ): ProduceTangemPayInitialDataUseCase {
            return ProduceTangemPayInitialDataUseCase(repository = repository)
        }

        @Provides
        fun provideChangeCardFrozenStateUseCase(
            cardDetailsRepository: TangemPayCardDetailsRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): ChangeCardFrozenStateUseCase {
            return ChangeCardFrozenStateUseCase(
                cardDetailsRepository = cardDetailsRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        @Singleton
        fun provideStartTangemPayPollingUseCase(
            cardDetailsRepository: TangemPayCardDetailsRepository,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): StartTangemPayOrderPollingUseCase {
            return StartTangemPayOrderPollingUseCase(cardDetailsRepository, paymentAccountStatusFetcher)
        }

        @Provides
        fun provideReissueTangemPayCardUseCase(
            reissueCardRepository: TangemPayReissueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): ReissueTangemPayCardUseCase {
            return ReissueTangemPayCardUseCase(
                reissueCardRepository = reissueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            )
        }

        @Provides
        fun provideCloseTangemPayCardUseCase(
            closeCardRepository: TangemPayCloseCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): CloseTangemPayCardUseCase {
            return CloseTangemPayCardUseCase(
                closeCardRepository = closeCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
            )
        }

        @Provides
        fun provideGetTangemPayCountriesUseCase(
            residencyRepository: TangemPayResidencyRepository,
        ): GetTangemPayCountriesUseCase {
            return GetTangemPayCountriesUseCase(residencyRepository)
        }

        @Provides
        fun provideGetTangemPayRegionsUseCase(
            residencyRepository: TangemPayResidencyRepository,
        ): GetTangemPayRegionsUseCase {
            return GetTangemPayRegionsUseCase(residencyRepository)
        }

        @Provides
        fun provideCheckTangemPayEligibilityUseCase(
            residencyRepository: TangemPayResidencyRepository,
        ): CheckTangemPayEligibilityUseCase {
            return CheckTangemPayEligibilityUseCase(residencyRepository)
        }

        @Provides
        fun provideCheckTangemPayEligibilityByKycUseCase(
            residencyRepository: TangemPayResidencyRepository,
        ): CheckTangemPayEligibilityForCustomerUseCase {
            return CheckTangemPayEligibilityForCustomerUseCase(residencyRepository)
        }

        @Provides
        fun provideJoinTangemPayWaitlistUseCase(
            waitlistRepository: TangemPayWaitlistRepository,
        ): JoinTangemPayWaitlistUseCase {
            return JoinTangemPayWaitlistUseCase(waitlistRepository)
        }

        @Provides
        fun provideGetCustomerOffersUseCase(
            customerOffersRepository: CustomerOffersRepository,
        ): GetCustomerOffersUseCase {
            return GetCustomerOffersUseCase(customerOffersRepository)
        }

        @Provides
        fun provideResolveTangemPayCampaignUseCase(
            onboardingRepository: OnboardingRepository,
            campaignRepository: TangemPayCampaignRepository,
            eligibilityManager: TangemPayEligibilityManager,
            getCustomerOffersUseCase: GetCustomerOffersUseCase,
        ): ResolveTangemPayCampaignUseCase {
            return ResolveTangemPayCampaignUseCase(
                onboardingRepository = onboardingRepository,
                campaignRepository = campaignRepository,
                eligibilityManager = eligibilityManager,
                getCustomerOffersUseCase = getCustomerOffersUseCase,
            )
        }

        @Provides
        fun provideGetCashbackSummaryUseCase(cashbackRepository: CashbackRepository): GetCashbackSummaryUseCase {
            return GetCashbackSummaryUseCase(cashbackRepository)
        }

        @Provides
        fun provideObserveCashbackSummaryUseCase(
            cashbackRepository: CashbackRepository,
        ): ObserveCashbackSummaryUseCase {
            return ObserveCashbackSummaryUseCase(cashbackRepository)
        }

        @Provides
        fun provideGetCashbackDeactivationDismissedUseCase(
            cashbackRepository: CashbackRepository,
        ): GetCashbackDeactivationDismissedUseCase {
            return GetCashbackDeactivationDismissedUseCase(cashbackRepository)
        }

        @Provides
        fun provideSetCashbackDeactivationDismissedUseCase(
            cashbackRepository: CashbackRepository,
        ): SetCashbackDeactivationDismissedUseCase {
            return SetCashbackDeactivationDismissedUseCase(cashbackRepository)
        }

        @Provides
        fun provideRestoreActiveCardOrdersUseCase(
            customerOrderRepository: CustomerOrderRepository,
            issueCardRepository: TangemPayIssueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): RestoreActiveCardOrdersUseCase {
            return RestoreActiveCardOrdersUseCase(
                customerOrderRepository = customerOrderRepository,
                issueCardRepository = issueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideValidateLocalOrderHintUseCase(
            customerOrderRepository: CustomerOrderRepository,
            onboardingRepository: OnboardingRepository,
        ): ValidateLocalOrderHintUseCase {
            return ValidateLocalOrderHintUseCase(customerOrderRepository, onboardingRepository)
        }

        @Provides
        fun provideIssueAdditionalCardUseCase(
            customerOffersRepository: CustomerOffersRepository,
            customerOrderRepository: CustomerOrderRepository,
            issueCardRepository: TangemPayIssueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): IssueAdditionalCardUseCase {
            return IssueAdditionalCardUseCase(
                customerOffersRepository = customerOffersRepository,
                customerOrderRepository = customerOrderRepository,
                issueCardRepository = issueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideIssuePlasticCardUseCase(
            customerOffersRepository: CustomerOffersRepository,
            customerOrderRepository: CustomerOrderRepository,
            issueCardRepository: TangemPayIssueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): IssuePlasticCardUseCase {
            return IssuePlasticCardUseCase(
                customerOffersRepository = customerOffersRepository,
                customerOrderRepository = customerOrderRepository,
                issueCardRepository = issueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideReissuePlasticCardUseCase(
            customerOrderRepository: CustomerOrderRepository,
            reissueCardRepository: TangemPayReissueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): ReissuePlasticCardUseCase {
            return ReissuePlasticCardUseCase(
                customerOrderRepository = customerOrderRepository,
                reissueCardRepository = reissueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideActivatePlasticCardUseCase(
            customerOrderRepository: CustomerOrderRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
            appCoroutineScope: AppCoroutineScope,
        ): ActivatePlasticCardUseCase {
            return ActivatePlasticCardUseCase(
                customerOrderRepository = customerOrderRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideCreateVirtualAccountOrderUseCase(
            onboardingRepository: OnboardingRepository,
            pollingUseCase: StartTangemPayOrderPollingUseCase,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
            appCoroutineScope: AppCoroutineScope,
        ): CreateVirtualAccountOrderUseCase {
            return CreateVirtualAccountOrderUseCase(
                onboardingRepository = onboardingRepository,
                pollingUseCase = pollingUseCase,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideGetBankCredentialsUseCase(onboardingRepository: OnboardingRepository): GetBankCredentialsUseCase {
            return GetBankCredentialsUseCase(onboardingRepository = onboardingRepository)
        }

        @Provides
        fun provideGetOnrampFeesUseCase(onboardingRepository: OnboardingRepository): GetOnrampFeesUseCase {
            return GetOnrampFeesUseCase(onboardingRepository = onboardingRepository)
        }

        @Provides
        fun provideCancelTangemPayOrderUseCase(
            customerOrderRepository: CustomerOrderRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
        ): CancelTangemPayOrderUseCase {
            return CancelTangemPayOrderUseCase(
                customerOrderRepository = customerOrderRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
            )
        }

        @Provides
        fun provideCreatePaymentNetworkContractUseCase(
            customerOrderRepository: CustomerOrderRepository,
            pollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): CreatePaymentNetworkContractUseCase {
            return CreatePaymentNetworkContractUseCase(
                customerOrderRepository = customerOrderRepository,
                pollingUseCase = pollingUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideCreateTariffPlanTransitionOrderUseCase(
            customerOrderRepository: CustomerOrderRepository,
            issueCardRepository: TangemPayIssueCardRepository,
            startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
            appCoroutineScope: AppCoroutineScope,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        ): CreateTariffPlanTransitionOrderUseCase {
            return CreateTariffPlanTransitionOrderUseCase(
                customerOrderRepository = customerOrderRepository,
                issueCardRepository = issueCardRepository,
                startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideSubmitTariffTransitionUseCase(
            createTransitionOrder: CreateTariffPlanTransitionOrderUseCase,
            setPendingTransition: SetTariffPlanPendingTransitionUseCase,
        ): SubmitTariffTransitionUseCase {
            return SubmitTariffTransitionUseCase(
                createTransitionOrder = createTransitionOrder,
                setPendingTransition = setPendingTransition,
            )
        }

        @Provides
        fun provideCancelTariffTransitionUseCase(
            cancelTangemPayOrderUseCase: CancelTangemPayOrderUseCase,
            getTariffTransitionUseCase: GetTangemPayTariffPlanTransitionsUseCase,
            submitTariffTransitionUseCase: SubmitTariffTransitionUseCase,
            getCurrentTariffUseCase: GetCurrentTariffUseCase,
        ): CancelTariffTransitionUseCase {
            return CancelTariffTransitionUseCase(
                cancelTangemPayOrderUseCase = cancelTangemPayOrderUseCase,
                getTariffTransitionUseCase = getTariffTransitionUseCase,
                submitTariffTransitionUseCase = submitTariffTransitionUseCase,
                getCurrentTariffUseCase = getCurrentTariffUseCase,
            )
        }

        @Provides
        fun provideGetCurrentTariffUseCase(
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
            paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
        ): GetCurrentTariffUseCase {
            return GetCurrentTariffUseCase(
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            )
        }

        @Provides
        @Singleton
        fun provideBankOfframpRepository(repository: DefaultBankOfframpRepository): BankOfframpRepository = repository

        @Provides
        @Singleton
        fun provideTangemPayResidencyRepository(
            repository: DefaultTangemPayResidencyRepository,
        ): TangemPayResidencyRepository = CachingTangemPayResidencyRepository(origin = repository)

        @Provides
        @Singleton
        fun providePaymentAccountRecentsStore(
            prefs: AppPreferencesStore,
            spendPreferencesKeys: SpendPreferencesKeys,
        ): PaymentAccountRecentsStore {
            return DefaultPaymentAccountRecentsStore(prefs = prefs, spendPreferencesKeys = spendPreferencesKeys)
        }

        @Provides
        fun provideGetRecentTokenTransfersUseCase(
            recentsStore: PaymentAccountRecentsStore,
            singleAccountListSupplier: SingleAccountListSupplier,
        ): GetRecentTokenTransfersUseCase {
            return GetRecentTokenTransfersUseCase(
                recentsStore = recentsStore,
                singleAccountListSupplier = singleAccountListSupplier,
            )
        }

        @Provides
        fun provideRecordTokenTransferFromSwapUseCase(
            recentsStore: PaymentAccountRecentsStore,
        ): RecordTokenTransferFromSwapUseCase {
            return RecordTokenTransferFromSwapUseCase(recentsStore = recentsStore)
        }

        @Provides
        fun provideRecordTokenTransferFromSendUseCase(
            recentsStore: PaymentAccountRecentsStore,
            paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
        ): RecordTokenTransferFromSendUseCase {
            return RecordTokenTransferFromSendUseCase(
                recentsStore = recentsStore,
                paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            )
        }

        @Provides
        fun provideGetBankAccountsUseCase(repository: BankOfframpRepository): GetBankAccountsUseCase {
            return GetBankAccountsUseCase(repository = repository)
        }

        @Provides
        fun provideGetRecentBankAccountsUseCase(repository: BankOfframpRepository): GetRecentBankAccountsUseCase {
            return GetRecentBankAccountsUseCase(repository = repository)
        }

        @Provides
        fun provideGetBankOfframpOptionsUseCase(repository: BankOfframpRepository): GetBankOfframpOptionsUseCase {
            return GetBankOfframpOptionsUseCase(repository = repository)
        }

        @Provides
        fun provideCreateBankOfframpOrderUseCase(
            repository: BankOfframpOrderRepository,
        ): CreateBankOfframpOrderUseCase {
            return CreateBankOfframpOrderUseCase(repository = repository)
        }

        @Provides
        fun provideAwaitBankOfframpSignatureUseCase(
            repository: BankOfframpOrderRepository,
        ): AwaitBankOfframpSignatureUseCase {
            return AwaitBankOfframpSignatureUseCase(repository = repository)
        }

        @Provides
        fun provideSignBankOfframpOrderUseCase(
            repository: BankOfframpOrderRepository,
            authDataSource: TangemPayAuthDataSource,
            userWalletsListRepository: UserWalletsListRepository,
        ): SignBankOfframpOrderUseCase {
            return SignBankOfframpOrderUseCase(
                repository = repository,
                authDataSource = authDataSource,
                userWalletsListRepository = userWalletsListRepository,
            )
        }

        @Provides
        fun provideCancelBankOfframpOrderUseCase(
            repository: BankOfframpOrderRepository,
        ): CancelBankOfframpOrderUseCase {
            return CancelBankOfframpOrderUseCase(repository = repository)
        }

        @Provides
        fun provideFindActiveBankOfframpOrderUseCase(
            repository: CustomerOrderRepository,
        ): FindActiveBankOfframpOrderUseCase {
            return FindActiveBankOfframpOrderUseCase(repository = repository)
        }
    }
}