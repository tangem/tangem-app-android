package com.tangem.data.pay.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.pay.DefaultTangemPayCryptoCurrencyFactory
import com.tangem.data.pay.DefaultTangemPayEligibilityManager
import com.tangem.data.pay.flow.DefaultPaymentAccountStatusFetcher
import com.tangem.data.pay.flow.DefaultPaymentAccountStatusProducer
import com.tangem.data.pay.repository.*
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.data.pay.usecase.DefaultGetTangemPayCurrencyStatusUseCase
import com.tangem.data.pay.usecase.DefaultGetTangemPayCustomerIdUseCase
import com.tangem.data.pay.usecase.DefaultTangemPayWithdrawUseCase
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.domain.pay.TangemPayCryptoCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.*
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TangemPayDataModule {

    @Binds
    @Singleton
    fun bindKycRepository(repository: DefaultKycRepository): KycRepository

    @Binds
    @Singleton
    fun bindOnboardingRepository(repository: DefaultOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    fun bindTangemPayTxHistoryRepository(repository: DefaultTangemPayTxHistoryRepository): TangemPayTxHistoryRepository

    @Binds
    @Singleton
    fun bindCardDetailsRepository(repository: DefaultTangemPayCardDetailsRepository): TangemPayCardDetailsRepository

    @Binds
    @Singleton
    fun bindTangemPaySwapRepository(repository: DefaultTangemPayWithdrawRepository): TangemPayWithdrawRepository

    @Binds
    @Singleton
    fun bindCustomerOrderRepository(repository: DefaultCustomerOrderRepository): CustomerOrderRepository

    @Binds
    @Singleton
    fun bindTangemPayCryptoCurrencyFactory(
        factory: DefaultTangemPayCryptoCurrencyFactory,
    ): TangemPayCryptoCurrencyFactory

    @Binds
    @Singleton
    fun bindGetTangemPayCurrencyStatusUseCase(
        impl: DefaultGetTangemPayCurrencyStatusUseCase,
    ): GetTangemPayCurrencyStatusUseCase

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

    companion object {

        @Provides
        @Singleton
        fun providePaymentAccountStatusesStore(
            @NetworkMoshi moshi: Moshi,
            @ApplicationContext context: Context,
            dispatchers: CoroutineDispatcherProvider,
            scope: AppCoroutineScope,
        ): PaymentAccountStatusesStore {
            return PaymentAccountStatusesStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = DataStoreFactory.create(
                    serializer = MoshiDataStoreSerializer(
                        moshi = moshi,
                        types = mapWithStringKeyTypes<PaymentAccountStatusValueDM>(),
                        defaultValue = emptyMap(),
                    ),
                    produceFile = { context.dataStoreFile(fileName = "payment_account_statuses") },
                    scope = scope,
                ),
                scope = scope,
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
        @Singleton
        fun provideTangemPayMainScreenCustomerInfoUseCase(
            repository: OnboardingRepository,
            customerOrderRepository: CustomerOrderRepository,
            tangemPayOnboardingRepository: OnboardingRepository,
            eligibilityManager: TangemPayEligibilityManager,
            deviceSecurity: DeviceSecurityInfoProvider,
        ): TangemPayMainScreenCustomerInfoUseCase {
            return TangemPayMainScreenCustomerInfoUseCase(
                onboardingRepository = repository,
                customerOrderRepository = customerOrderRepository,
                eligibilityManager = eligibilityManager,
                deviceSecurity = deviceSecurity,
            )
        }

        @Provides
        @Singleton
        fun provideProduceTangemPayInitialDataUseCase(
            repository: OnboardingRepository,
        ): ProduceTangemPayInitialDataUseCase {
            return ProduceTangemPayInitialDataUseCase(repository = repository)
        }
    }
}