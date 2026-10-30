package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.PredictionDepositRequisitesStore
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.usecase.GetPredictionDepositCurrencyStatusesUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionTopUpMinimumUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PredictionTopUpDomainModule {

    @Provides
    @Singleton
    fun provideGetPredictionTopUpMinimumUseCase(
        requisitesStore: PredictionDepositRequisitesStore,
    ): GetPredictionTopUpMinimumUseCase = GetPredictionTopUpMinimumUseCase(requisitesStore = requisitesStore)

    @Provides
    @Singleton
    fun provideGetPredictionDepositCurrencyStatusesUseCase(
        requisitesStore: PredictionDepositRequisitesStore,
        predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
        collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    ): GetPredictionDepositCurrencyStatusesUseCase = GetPredictionDepositCurrencyStatusesUseCase(
        requisitesStore = requisitesStore,
        predictionAccountStatusSupplier = predictionAccountStatusSupplier,
        collateralCurrencyFactory = collateralCurrencyFactory,
    )
}