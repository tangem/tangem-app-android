package com.tangem.data.transaction.di

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.transaction.*
import com.tangem.data.transaction.error.DefaultFeeErrorResolver
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.datasource.api.gasless.GaslessTxServiceApi
import com.tangem.datasource.api.gasless.GaslessTxServiceApiV2
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.transaction.*
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TransactionDataModule {

    @Provides
    @Singleton
    fun providesTransactionRepository(
        tangemTechApi: TangemTechApi,
        walletManagersFacade: WalletManagersFacade,
        walletManagersStore: WalletManagersStore,
        dispatchers: CoroutineDispatcherProvider,
    ): TransactionRepository {
        return DefaultTransactionRepository(
            tangemTechApi = tangemTechApi,
            walletManagersFacade = walletManagersFacade,
            walletManagersStore = walletManagersStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providesFeeRepository(walletManagersFacade: WalletManagersFacade): FeeRepository {
        return DefaultFeeRepository(
            walletManagersFacade,
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun providesMemoValidatorFacade(
        blockchainSDKFactory: BlockchainSDKFactory,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): MemoValidatorFacade {
        return DefaultMemoValidatorFacade(
            blockchainSDKFactory = blockchainSDKFactory,
            dispatchers = coroutineDispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun providesWalletAddressServiceRepository(
        walletManagersFacade: WalletManagersFacade,
        memoValidatorFacade: MemoValidatorFacade,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): WalletAddressServiceRepository {
        return DefaultWalletAddressServiceRepository(
            walletManagersFacade = walletManagersFacade,
            memoValidatorFacade = memoValidatorFacade,
            dispatchers = coroutineDispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun providerFeeErrorResolver(): FeeErrorResolver {
        return DefaultFeeErrorResolver()
    }

    @Provides
    @Singleton
    fun provideGaslessTransactionRepository(
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        gaslessTxServiceApi: GaslessTxServiceApi,
        gaslessTxServiceApiV2: GaslessTxServiceApiV2,
        featureTogglesManager: FeatureTogglesManager,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): GaslessTransactionRepository {
        return DefaultGaslessTransactionRepository(
            gaslessTxServiceApi = gaslessTxServiceApi,
            gaslessTxServiceApiV2 = gaslessTxServiceApiV2,
            // Single master toggle for the whole gasless v2 protocol (+ yield-withdraw batch).
            isGaslessV2Enabled = featureTogglesManager.isFeatureEnabled(
                toggle = FeatureToggles.AND_15632_GASLESS_YIELD_WITHDRAW_ENABLED,
            ),
            coroutineDispatcherProvider = coroutineDispatcherProvider,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        )
    }

    @Provides
    @Singleton
    fun provideAllowanceRepository(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): AllowanceRepository {
        return DefaultAllowanceRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }
}