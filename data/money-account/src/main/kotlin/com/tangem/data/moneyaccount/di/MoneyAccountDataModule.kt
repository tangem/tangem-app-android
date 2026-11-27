package com.tangem.data.moneyaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.data.moneyaccount.cleaner.MoneyAccountUserWalletDataCleaner
import com.tangem.data.moneyaccount.contribution.MoneyAccountContributions
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.data.moneyaccount.currency.DefaultMoneyAccountCurrencyFactory
import com.tangem.data.moneyaccount.currency.MoneyAccountCurrenciesFetcher
import com.tangem.data.moneyaccount.currency.MoneyAccountCurrenciesProvider
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusFetcher
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusProducer
import com.tangem.data.moneyaccount.repository.DefaultMoneyAccountActivationRepository
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesFetcher
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.spend.datasource.tangempay.entity.MoneyAccountStatusValueDM
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.yield.supply.YieldSupplyBalanceFetcher
import com.tangem.domain.models.yield.supply.YieldSupplyBalanceProvider
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusProducer
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.tangempay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.moneyaccount.usecase.GetTangemPayProductsAvailabilityUseCase
import com.tangem.domain.moneyaccount.usecase.IsMoneyAccountPathOccupiedUseCase
import com.tangem.domain.moneyaccount.usecase.IssueMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.PollTangemPayAccountStatusesUseCase
import com.tangem.domain.moneyaccount.usecase.RenewTangemPaySessionUseCase
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.tangempay.usecase.CreateOnboardingOrderUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
internal interface MoneyAccountDataModule {

    @Binds
    @Singleton
    fun bindMoneyAccountStatusProducerFactory(
        impl: DefaultMoneyAccountStatusProducer.Factory,
    ): MoneyAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindMoneyAccountStatusFetcher(impl: DefaultMoneyAccountStatusFetcher): MoneyAccountStatusFetcher

    @Binds
    @Singleton
    fun bindNonPortfolioCurrenciesFetcher(impl: MoneyAccountCurrenciesFetcher): NonPortfolioCurrenciesFetcher

    @Binds
    @Singleton
    fun bindMoneyAccountActivationRepository(
        impl: DefaultMoneyAccountActivationRepository,
    ): MoneyAccountActivationRepository

    @Binds
    @Singleton
    fun bindMoneyAccountCurrencyFactory(impl: DefaultMoneyAccountCurrencyFactory): MoneyAccountCurrencyFactory

    @Binds
    @Singleton
    fun bindNonPortfolioCurrenciesProvider(impl: MoneyAccountCurrenciesProvider): NonPortfolioCurrenciesProvider

    @Binds
    @IntoSet
    fun bindMoneyAccountUserWalletDataCleaner(impl: MoneyAccountUserWalletDataCleaner): UserWalletDataCleaner

    companion object {

        @Provides
        @Singleton
        fun provideMoneyAccountStatusesStore(
            @ApplicationContext context: Context,
            scope: AppCoroutineScope,
            converter: MoneyAccountStatusValueDMConverter,
            dataStoreFactory: AppDataStoreFactory,
            userWalletsListRepository: UserWalletsListRepository,
        ): MoneyAccountStatusesStore {
            return MoneyAccountStatusesStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = dataStoreFactory.create(
                    serializer = KotlinxDataStoreSerializer(
                        defaultValue = emptyMap(),
                        serializer = MapSerializer(String.serializer(), MoneyAccountStatusValueDM.serializer()),
                        json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" },
                    ),
                    produceFile = { context.dataStoreFile(fileName = "money_account_statuses") },
                    scope = scope,
                ),
                converter = converter,
                userWalletsListRepository = userWalletsListRepository,
                scope = scope,
            )
        }

        @Provides
        @Singleton
        fun provideMoneyAccountStatusSupplier(
            factory: MoneyAccountStatusProducer.Factory,
        ): MoneyAccountStatusSupplier {
            return object : MoneyAccountStatusSupplier(
                factory = factory,
                keyCreator = { "money_account_status_${it.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        @Singleton
        fun provideActivateMoneyAccountUseCase(
            repository: MoneyAccountActivationRepository,
        ): ActivateMoneyAccountUseCase {
            return ActivateMoneyAccountUseCase(repository = repository)
        }

        @Provides
        @Singleton
        fun provideIssueMoneyAccountUseCase(
            activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
            orderRepository: TangemPayOrderRepository,
            pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): IssueMoneyAccountUseCase {
            return IssueMoneyAccountUseCase(
                activateMoneyAccountUseCase = activateMoneyAccountUseCase,
                orderRepository = orderRepository,
                pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        @Singleton
        fun provideStartTangemPayOnboardingUseCase(
            activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
            createOnboardingOrderUseCase: CreateOnboardingOrderUseCase,
            pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
            userWalletsListRepository: UserWalletsListRepository,
            analytics: AnalyticsEventHandler,
            appCoroutineScope: AppCoroutineScope,
        ): StartTangemPayOnboardingUseCase {
            return StartTangemPayOnboardingUseCase(
                activateMoneyAccountUseCase = activateMoneyAccountUseCase,
                createOnboardingOrderUseCase = createOnboardingOrderUseCase,
                pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
                userWalletsListRepository = userWalletsListRepository,
                analytics = analytics,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        @Singleton
        fun provideGetMoneyAccountBalanceUseCase(
            singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
            singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
            singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
            singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
        ): GetMoneyAccountBalanceUseCase {
            return GetMoneyAccountBalanceUseCase(
                singleNetworkStatusFetcher = singleNetworkStatusFetcher,
                singleNetworkStatusSupplier = singleNetworkStatusSupplier,
                singleQuoteStatusFetcher = singleQuoteStatusFetcher,
                singleQuoteStatusSupplier = singleQuoteStatusSupplier,
            )
        }

        @Provides
        fun providePollTangemPayAccountStatusesUseCase(
            getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
            moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
        ): PollTangemPayAccountStatusesUseCase {
            return PollTangemPayAccountStatusesUseCase(
                getCustomerSummaryUseCase = getCustomerSummaryUseCase,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                moneyAccountStatusFetcher = moneyAccountStatusFetcher,
                timeSource = TimeSource.Monotonic,
            )
        }

        @Provides
        fun provideRenewTangemPaySessionUseCase(
            produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
            paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
            moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
        ): RenewTangemPaySessionUseCase {
            return RenewTangemPaySessionUseCase(
                produceTangemPayInitialDataUseCase = produceTangemPayInitialDataUseCase,
                paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                moneyAccountStatusFetcher = moneyAccountStatusFetcher,
            )
        }

        @Provides
        @Singleton
        fun provideGetMoneyAccountCryptoCurrencyStatusUseCase(
            moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
        ): GetMoneyAccountCryptoCurrencyStatusUseCase {
            return GetMoneyAccountCryptoCurrencyStatusUseCase(moneyAccountStatusSupplier = moneyAccountStatusSupplier)
        }

        @Provides
        fun provideIsMoneyAccountPathOccupiedUseCase(
            singleAccountListSupplier: SingleAccountListSupplier,
        ): IsMoneyAccountPathOccupiedUseCase {
            return IsMoneyAccountPathOccupiedUseCase(singleAccountListSupplier = singleAccountListSupplier)
        }

        @Provides
        fun provideGetTangemPayProductsAvailabilityUseCase(
            isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase,
            getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
        ): GetTangemPayProductsAvailabilityUseCase {
            return GetTangemPayProductsAvailabilityUseCase(
                isMoneyAccountPathOccupiedUseCase = isMoneyAccountPathOccupiedUseCase,
                getCustomerSummaryUseCase = getCustomerSummaryUseCase,
            )
        }

        @Provides
        fun provideGetMoneyAccountBankCredentialsUseCase(
            onboardingRepository: OnboardingRepository,
        ): GetMoneyAccountBankCredentialsUseCase {
            return GetMoneyAccountBankCredentialsUseCase(onboardingRepository = onboardingRepository)
        }

        @Provides
        @MoneyAccountContributions
        fun provideMoneyAccountContributionProviders(
            yieldSupply: YieldSupplyBalanceProvider,
        ): List<BalanceContributionProvider> {
            return listOf(yieldSupply)
        }

        @Provides
        @MoneyAccountContributions
        fun provideMoneyAccountContributionFetchers(
            yieldSupply: YieldSupplyBalanceFetcher,
        ): List<BalanceContributionFetcher> {
            return listOf(yieldSupply)
        }
    }
}