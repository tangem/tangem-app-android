package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.DefaultPolymarketBridgeRepository
import com.tangem.data.polymarket.DefaultPolymarketRepository
import com.tangem.data.polymarket.DefaultPolymarketPositionsRepository
import com.tangem.data.polymarket.DefaultPolymarketRedeemRepository
import com.tangem.data.polymarket.DefaultPolymarketWithdrawRepository
import com.tangem.data.polymarket.builder.DefaultPredictionTransferCallCodec
import com.tangem.data.polymarket.entity.DefaultPolymarketCollateralCurrencyFactory
import com.tangem.data.polymarket.derivation.DefaultPolymarketDepositWalletDeriver
import com.tangem.data.polymarket.derivation.DefaultPolymarketEoaDeriver
import com.tangem.data.polymarket.flow.DefaultPredictionAccountStatusFetcher
import com.tangem.data.polymarket.flow.DefaultPredictionAccountStatusProducer
import com.tangem.data.polymarket.store.DefaultPolymarketBridgeAssetsStore
import com.tangem.data.polymarket.store.DefaultPredictionDepositAddressWriter
import com.tangem.data.polymarket.store.DefaultPredictionDepositRequisitesStore
import com.tangem.domain.polymarket.PolymarketBridgeAssetsStore
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.flow.DefaultPredictionFundingWindow
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionAccountStatusProducer
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionBalancePoller
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.flow.PredictionWithdrawalTracker
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import com.tangem.domain.polymarket.model.PredictionWithdrawalTrackingConfig
import com.tangem.domain.polymarket.usecase.GetPredictionWithdrawalStateUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketDataModule {

    @Binds
    @Singleton
    fun bindPolymarketPositionsRepository(impl: DefaultPolymarketPositionsRepository): PolymarketPositionsRepository

    @Binds
    @Singleton
    fun bindPolymarketRepository(impl: DefaultPolymarketRepository): PolymarketRepository

    @Binds
    @Singleton
    fun bindPolymarketBridgeRepository(impl: DefaultPolymarketBridgeRepository): PolymarketBridgeRepository

    @Binds
    @Singleton
    fun bindPolymarketBridgeAssetsStore(impl: DefaultPolymarketBridgeAssetsStore): PolymarketBridgeAssetsStore

    @Binds
    @Singleton
    fun bindPolymarketCollateralCurrencyFactory(
        impl: DefaultPolymarketCollateralCurrencyFactory,
    ): PolymarketCollateralCurrencyFactory

    @Binds
    @Singleton
    fun bindPolymarketDepositWalletDeriver(impl: DefaultPolymarketDepositWalletDeriver): PolymarketDepositWalletDeriver

    @Binds
    @Singleton
    fun bindPolymarketEoaDeriver(impl: DefaultPolymarketEoaDeriver): PolymarketEoaDeriver

    @Binds
    @Singleton
    fun bindPredictionAccountStatusProducerFactory(
        impl: DefaultPredictionAccountStatusProducer.Factory,
    ): PredictionAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindPredictionAccountStatusFetcher(impl: DefaultPredictionAccountStatusFetcher): PredictionAccountStatusFetcher

    @Binds
    @Singleton
    fun bindPredictionDepositRequisitesStore(
        impl: DefaultPredictionDepositRequisitesStore,
    ): PredictionDepositRequisitesStore

    @Binds
    @Singleton
    fun bindPredictionDepositAddressWriter(impl: DefaultPredictionDepositAddressWriter): PredictionDepositAddressWriter

    @Binds
    @Singleton
    fun bindPolymarketWithdrawRepository(impl: DefaultPolymarketWithdrawRepository): PolymarketWithdrawRepository

    @Binds
    @Singleton
    fun bindPolymarketRedeemRepository(impl: DefaultPolymarketRedeemRepository): PolymarketRedeemRepository

    @Binds
    @Singleton
    fun bindPredictionTransferCallCodec(impl: DefaultPredictionTransferCallCodec): PredictionTransferCallCodec

    companion object {

        @Provides
        @Singleton
        fun providePredictionAccountStatusSupplier(
            factory: PredictionAccountStatusProducer.Factory,
        ): PredictionAccountStatusSupplier {
            return object : PredictionAccountStatusSupplier(
                factory = factory,
                keyCreator = { params -> "prediction_account_status_${params.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        @Singleton
        fun providePredictionBalancePollingConfig(): PredictionBalancePollingConfig = PredictionBalancePollingConfig()

        @Provides
        @Singleton
        fun providePredictionBalancePoller(
            fetcher: PredictionAccountStatusFetcher,
            fundingWindow: PredictionFundingWindow,
            config: PredictionBalancePollingConfig,
            appScope: AppCoroutineScope,
        ): PredictionBalancePoller = PredictionBalancePoller(
            fetcher = fetcher,
            fundingWindow = fundingWindow,
            config = config,
            appScope = appScope,
        )

        @Provides
        @Singleton
        fun providePredictionWithdrawalTrackingConfig(): PredictionWithdrawalTrackingConfig =
            PredictionWithdrawalTrackingConfig()

        @Provides
        @Singleton
        fun providePredictionWithdrawalTracker(
            store: PredictionWithdrawalStore,
            submitInteractor: SubmitPredictionWithdrawalInteractor,
            getState: GetPredictionWithdrawalStateUseCase,
            config: PredictionWithdrawalTrackingConfig,
            appScope: AppCoroutineScope,
        ): PredictionWithdrawalTracker = PredictionWithdrawalTracker(
            store = store,
            submitInteractor = submitInteractor,
            getState = getState,
            config = config,
            appScope = appScope,
        )

        @Provides
        @Singleton
        fun providePredictionFundingWindow(
            predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
            config: PredictionBalancePollingConfig,
            appScope: AppCoroutineScope,
        ): PredictionFundingWindow = DefaultPredictionFundingWindow(
            predictionAccountStatusSupplier = predictionAccountStatusSupplier,
            config = config,
            appScope = appScope,
        )
    }
}