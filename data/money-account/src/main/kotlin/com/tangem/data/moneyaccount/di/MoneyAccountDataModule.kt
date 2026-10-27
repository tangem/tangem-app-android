package com.tangem.data.moneyaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.data.moneyaccount.currency.DefaultMoneyAccountCurrencyFactory
import com.tangem.data.moneyaccount.currency.MoneyAccountCurrenciesProvider
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusFetcher
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusProducer
import com.tangem.data.moneyaccount.repository.DefaultMoneyAccountActivationRepository
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusProducer
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.pay.usecase.GetTangemPayProductsAvailabilityForCustomerUseCase
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.moneyaccount.usecase.GetTangemPayProductsAvailabilityUseCase
import com.tangem.domain.moneyaccount.usecase.IsMoneyAccountPathOccupiedUseCase
import com.tangem.domain.moneyaccount.usecase.PollTangemPayAccountStatusesUseCase
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.pay.usecase.CreateOnboardingOrderUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

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
    fun bindMoneyAccountActivationRepository(
        impl: DefaultMoneyAccountActivationRepository,
    ): MoneyAccountActivationRepository

    @Binds
    @Singleton
    fun bindMoneyAccountCurrencyFactory(impl: DefaultMoneyAccountCurrencyFactory): MoneyAccountCurrencyFactory

    @Binds
    @Singleton
    fun bindNonPortfolioCurrenciesProvider(impl: MoneyAccountCurrenciesProvider): NonPortfolioCurrenciesProvider

    companion object {

        @Provides
        @Singleton
        fun provideMoneyAccountStatusesStore(
            @ApplicationContext context: Context,
            scope: AppCoroutineScope,
            converter: MoneyAccountStatusValueDMConverter,
            dataStoreFactory: AppDataStoreFactory,
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
        fun provideStartTangemPayOnboardingUseCase(
            activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
            createOnboardingOrderUseCase: CreateOnboardingOrderUseCase,
            pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
            appCoroutineScope: AppCoroutineScope,
        ): StartTangemPayOnboardingUseCase {
            return StartTangemPayOnboardingUseCase(
                activateMoneyAccountUseCase = activateMoneyAccountUseCase,
                createOnboardingOrderUseCase = createOnboardingOrderUseCase,
                pollTangemPayAccountStatusesUseCase = pollTangemPayAccountStatusesUseCase,
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
            getTangemPayProductsAvailabilityForCustomerUseCase: GetTangemPayProductsAvailabilityForCustomerUseCase,
            isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase,
        ): GetTangemPayProductsAvailabilityUseCase {
            return GetTangemPayProductsAvailabilityUseCase(
                getTangemPayProductsAvailabilityForCustomerUseCase = getTangemPayProductsAvailabilityForCustomerUseCase,
                isMoneyAccountPathOccupiedUseCase = isMoneyAccountPathOccupiedUseCase,
            )
        }

        @Provides
        fun provideGetMoneyAccountBankCredentialsUseCase(
            onboardingRepository: OnboardingRepository,
        ): GetMoneyAccountBankCredentialsUseCase {
            return GetMoneyAccountBankCredentialsUseCase(onboardingRepository = onboardingRepository)
        }
    }
}