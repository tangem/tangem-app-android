package com.tangem.features.polymarket.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEligibleWalletsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.RecordPolymarketConfirmationUseCase
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.domain.polymarket.interactor.CheckNewPolymarketAccountAllowedInteractor
import com.tangem.domain.polymarket.interactor.ResolvePolymarketEntryInteractor
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockComponent
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.DefaultPolymarketComponent
import com.tangem.features.polymarket.impl.details.model.PolymarketEventDetailsModel
import com.tangem.features.polymarket.impl.entry.model.PolymarketEntryModel
import com.tangem.features.polymarket.impl.featuretoggles.DefaultPolymarketFeatureToggles
import com.tangem.features.polymarket.impl.history.model.PolymarketHistoryModel
import com.tangem.features.polymarket.impl.walletblock.DefaultPolymarketWalletBlockComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsModel
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.search.model.PolymarketSearchModel
import com.tangem.features.polymarket.impl.positions.model.PolymarketPositionsModel
import com.tangem.features.polymarket.impl.positiondetails.model.PolymarketPositionDetailsModel
import com.tangem.features.polymarket.impl.positionresult.model.PolymarketPositionResultModel
import com.tangem.features.polymarket.impl.model.PolymarketModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionAmountModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionStatusModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSlippageModel
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketBindsModule {
    @Binds
    @Singleton
    fun providePolymarketComponentFactory(impl: DefaultPolymarketComponent.Factory): PolymarketComponent.Factory

    @Binds
    @Singleton
    fun providePolymarketWalletBlockComponentFactory(
        impl: DefaultPolymarketWalletBlockComponent.Factory,
    ): PolymarketWalletBlockComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface PolymarketModelModule {

    @Binds
    @IntoMap
    @ClassKey(PolymarketModel::class)
    fun bindPolymarketModel(impl: PolymarketModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketMainModel::class)
    fun bindPolymarketMainModel(impl: PolymarketMainModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketHistoryModel::class)
    fun bindPolymarketHistoryModel(impl: PolymarketHistoryModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketAddFundsModel::class)
    fun bindPolymarketAddFundsModel(impl: PolymarketAddFundsModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketSearchModel::class)
    fun bindPolymarketSearchModel(impl: PolymarketSearchModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketPositionsModel::class)
    fun bindPolymarketPositionsModel(impl: PolymarketPositionsModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketPositionDetailsModel::class)
    fun bindPolymarketPositionDetailsModel(impl: PolymarketPositionDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketPositionResultModel::class)
    fun bindPolymarketPositionResultModel(impl: PolymarketPositionResultModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketOnboardingModel::class)
    fun bindPolymarketOnboardingModel(impl: PolymarketOnboardingModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketEntryModel::class)
    fun bindPolymarketEntryModel(impl: PolymarketEntryModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketEventDetailsModel::class)
    fun bindPolymarketEventDetailsModel(impl: PolymarketEventDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(PlacePredictionAmountModel::class)
    fun bindPlacePredictionAmountModel(impl: PlacePredictionAmountModel): Model

    @Binds
    @IntoMap
    @ClassKey(PlacePredictionSummaryModel::class)
    fun bindPlacePredictionSummaryModel(impl: PlacePredictionSummaryModel): Model

    @Binds
    @IntoMap
    @ClassKey(PlacePredictionSlippageModel::class)
    fun bindPlacePredictionSlippageModel(impl: PlacePredictionSlippageModel): Model

    @Binds
    @IntoMap
    @ClassKey(PlacePredictionStatusModel::class)
    fun bindPlacePredictionStatusModel(impl: PlacePredictionStatusModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketFeatureTogglesModule {

    @Provides
    @Singleton
    fun providePolymarketFeatureToggles(featureTogglesManager: FeatureTogglesManager): PolymarketFeatureToggles {
        return DefaultPolymarketFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketDomainUseCasesModule {

    @Provides
    @Singleton
    fun provideCheckNewPolymarketAccountAllowedInteractor(
        getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
        checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    ): CheckNewPolymarketAccountAllowedInteractor = CheckNewPolymarketAccountAllowedInteractor(
        getWalletStatus = getPolymarketWalletStatusUseCase,
        checkGeoblock = checkPolymarketGeoblockUseCase,
    )

    @Provides
    @Singleton
    fun provideResolvePolymarketEntryInteractor(
        derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
        getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
        getPolymarketApiCredentialsUseCase: GetPolymarketApiCredentialsUseCase,
        polymarketOnboardedStore: PolymarketOnboardedStore,
        recordPolymarketConfirmation: RecordPolymarketConfirmationUseCase,
    ): ResolvePolymarketEntryInteractor = ResolvePolymarketEntryInteractor(
        derivePolymarketAddressesUseCase = derivePolymarketAddressesUseCase,
        getPolymarketWalletStatusUseCase = getPolymarketWalletStatusUseCase,
        getPolymarketApiCredentialsUseCase = getPolymarketApiCredentialsUseCase,
        polymarketOnboardedStore = polymarketOnboardedStore,
        recordPolymarketConfirmation = recordPolymarketConfirmation,
    )

    @Provides
    @Singleton
    fun provideRecordPolymarketConfirmationUseCase(
        polymarketOnboardedStore: PolymarketOnboardedStore,
    ): RecordPolymarketConfirmationUseCase = RecordPolymarketConfirmationUseCase(
        onboardedStore = polymarketOnboardedStore,
    )

    @Provides
    @Singleton
    fun provideGetPredictionOrderQuoteUseCase(
        polymarketRepository: PolymarketRepository,
    ): GetPredictionOrderQuoteUseCase = GetPredictionOrderQuoteUseCase(
        polymarketRepository = polymarketRepository,
    )

    @Provides
    @Singleton
    fun provideGetPredictionPriceUseCase(polymarketRepository: PolymarketRepository): GetPredictionPriceUseCase =
        GetPredictionPriceUseCase(
            polymarketRepository = polymarketRepository,
        )

    @Provides
    @Singleton
    fun provideGetPolymarketEligibleWalletsUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GetPolymarketEligibleWalletsUseCase = GetPolymarketEligibleWalletsUseCase(
        userWalletsListRepository = userWalletsListRepository,
    )
}