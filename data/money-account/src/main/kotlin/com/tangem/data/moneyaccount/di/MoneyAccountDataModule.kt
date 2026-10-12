package com.tangem.data.moneyaccount.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusFetcher
import com.tangem.data.moneyaccount.flow.DefaultMoneyAccountStatusProducer
import com.tangem.data.moneyaccount.repository.DefaultMoneyAccountActivationRepository
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusProducer
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.moneyaccount.repository.MoneyAccountActivationRepository
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountSuitableWalletsUseCase
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
            @NetworkMoshi moshi: Moshi,
            @ApplicationContext context: Context,
            scope: AppCoroutineScope,
            converter: MoneyAccountStatusValueDMConverter,
            dataStoreFactory: AppDataStoreFactory,
        ): MoneyAccountStatusesStore {
            return MoneyAccountStatusesStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = dataStoreFactory.create(
                    serializer = MoshiDataStoreSerializer(
                        moshi = moshi,
                        types = mapWithStringKeyTypes<MoneyAccountStatusValueDM>(),
                        defaultValue = emptyMap(),
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
        fun provideGetMoneyAccountEligibilityUseCase(
            getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase,
            onboardingRepository: OnboardingRepository,
            deviceSecurityInfoProvider: DeviceSecurityInfoProvider,
        ): GetMoneyAccountEligibilityUseCase {
            return GetMoneyAccountEligibilityUseCase(
                getMoneyAccountSuitableWalletsUseCase = getMoneyAccountSuitableWalletsUseCase,
                onboardingRepository = onboardingRepository,
                deviceSecurityInfoProvider = deviceSecurityInfoProvider,
            )
        }
    }
}