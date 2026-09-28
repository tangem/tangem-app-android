package com.tangem.data.transaction.di

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.transaction.*
import com.tangem.data.transaction.error.DefaultFeeErrorResolver
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.grow.datasource.gasless.GaslessTxServiceApi
import com.tangem.grow.datasource.gasless.GaslessTxServiceApiV2
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.datasource.api.tangemTech.TangemTechApi
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
        dispatchers: CoroutineDispatcherProvider,
    ): TransactionRepository {
        return DefaultTransactionRepository(
            tangemTechApi = tangemTechApi,
            walletManagersFacade = walletManagersFacade,
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
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): GaslessTransactionRepository {
        return DefaultGaslessTransactionRepository(
            gaslessTxServiceApi = gaslessTxServiceApi,
            gaslessTxServiceApiV2 = gaslessTxServiceApiV2,
            coroutineDispatcherProvider = coroutineDispatcherProvider,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        )
    }

    @Provides
    @Singleton
    fun provideTronGaslessTransactionRepository(
        tronGaslessApi: TronGaslessApi,
        @NetworkMoshi moshi: Moshi,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): TronGaslessTransactionRepository {
        return DefaultTronGaslessTransactionRepository(
            api = tronGaslessApi,
            errorAdapter = moshi.adapter(GaslessErrorResponse::class.java),
            dispatchers = coroutineDispatcherProvider,
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