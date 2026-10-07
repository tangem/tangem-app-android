package com.tangem.data.moneyaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.core.local.datastore.DeleteObsoleteFileMigration
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusFetcher
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusProducer
import com.tangem.data.moneyaccount.repository.DefaultMoneyAccountActivationRepository
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusProducer
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.FindActiveMoneyAccountCreationOrderUseCase
import com.tangem.domain.pay.usecase.StartTangemPayOrderPollingUseCase
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.CreateMoneyAccountOrderUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountFeeTableUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountStatusUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountSuitableWalletsUseCase
import com.tangem.domain.moneyaccount.usecase.RestoreMoneyAccountOrderUseCase
import com.tangem.domain.moneyaccount.usecase.TrackMoneyAccountOrderUseCase
import com.tangem.security.DeviceSecurityInfoProvider
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
                    produceFile = { context.dataStoreFile(fileName = "money_account_statuses_v2") },
                    migrations = listOf(
                        DeleteObsoleteFileMigration { context.dataStoreFile(fileName = "money_account_statuses") },
                        DeleteObsoleteFileMigration { context.dataStoreFile(fileName = "virtual_account_statuses") },
                    ),
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
        fun provideGetMoneyAccountSuitableWalletsUseCase(
            userWalletsListRepository: UserWalletsListRepository,
        ): GetMoneyAccountSuitableWalletsUseCase {
            return GetMoneyAccountSuitableWalletsUseCase(userWalletsListRepository = userWalletsListRepository)
        }

        @Provides
        @Singleton
        fun provideGetMoneyAccountBalanceUseCase(
            tangemPayCurrencyFactory: TangemPayCurrencyFactory,
            singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
            singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
        ): GetMoneyAccountBalanceUseCase {
            return GetMoneyAccountBalanceUseCase(
                tangemPayCurrencyFactory = tangemPayCurrencyFactory,
                singleNetworkStatusFetcher = singleNetworkStatusFetcher,
                singleNetworkStatusSupplier = singleNetworkStatusSupplier,
            )
        }

        @Provides
        @Singleton
        fun provideTrackMoneyAccountOrderUseCase(
            orderRepository: MoneyAccountOrderRepository,
            startPolling: StartTangemPayOrderPollingUseCase,
            moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
            appCoroutineScope: AppCoroutineScope,
        ): TrackMoneyAccountOrderUseCase {
            return TrackMoneyAccountOrderUseCase(
                orderRepository = orderRepository,
                startPolling = startPolling,
                moneyAccountStatusFetcher = moneyAccountStatusFetcher,
                appCoroutineScope = appCoroutineScope,
            )
        }

        @Provides
        fun provideCreateMoneyAccountOrderUseCase(
            orderRepository: MoneyAccountOrderRepository,
            trackOrder: TrackMoneyAccountOrderUseCase,
            findActiveMoneyAccountCreationOrderUseCase: FindActiveMoneyAccountCreationOrderUseCase,
        ): CreateMoneyAccountOrderUseCase {
            return CreateMoneyAccountOrderUseCase(
                orderRepository = orderRepository,
                trackOrder = trackOrder,
                findActiveMoneyAccountCreationOrderUseCase = findActiveMoneyAccountCreationOrderUseCase,
            )
        }

        @Provides
        fun provideRestoreMoneyAccountOrderUseCase(
            orderRepository: MoneyAccountOrderRepository,
            onboardingRepository: OnboardingRepository,
            trackOrder: TrackMoneyAccountOrderUseCase,
            findActiveMoneyAccountCreationOrderUseCase: FindActiveMoneyAccountCreationOrderUseCase,
        ): RestoreMoneyAccountOrderUseCase {
            return RestoreMoneyAccountOrderUseCase(
                orderRepository = orderRepository,
                onboardingRepository = onboardingRepository,
                trackOrder = trackOrder,
                findActiveMoneyAccountCreationOrderUseCase = findActiveMoneyAccountCreationOrderUseCase,
            )
        }

        @Provides
        fun provideGetMoneyAccountStatusUseCase(
            moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
            moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
        ): GetMoneyAccountStatusUseCase {
            return GetMoneyAccountStatusUseCase(
                moneyAccountStatusFetcher = moneyAccountStatusFetcher,
                moneyAccountStatusSupplier = moneyAccountStatusSupplier,
            )
        }

        @Provides
        fun provideGetMoneyAccountEligibilityUseCase(
            getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase,
            getMoneyAccountStatusUseCase: GetMoneyAccountStatusUseCase,
            deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
            onboardingRepository: OnboardingRepository,
            singleAccountListSupplier: SingleAccountListSupplier,
        ): GetMoneyAccountEligibilityUseCase {
            return GetMoneyAccountEligibilityUseCase(
                getMoneyAccountSuitableWalletsUseCase = getMoneyAccountSuitableWalletsUseCase,
                getMoneyAccountStatusUseCase = getMoneyAccountStatusUseCase,
                deviceSecurityInfoProvider = deviceSecurityInfoProvider,
                onboardingRepository = onboardingRepository,
                singleAccountListSupplier = singleAccountListSupplier,
            )
        }

        @Provides
        @Singleton
        fun provideGetMoneyAccountFeeTableUseCase(
            moneyAccountRepository: MoneyAccountRepository,
        ): GetMoneyAccountFeeTableUseCase {
            return GetMoneyAccountFeeTableUseCase(moneyAccountRepository = moneyAccountRepository)
        }
    }
}