package com.tangem.feature.swap.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.api.surveysparrow.SurveySparrowApi
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.feature.swap.DefaultSwapFeedbackRepository
import com.tangem.feature.swap.DefaultSwapRepository
import com.tangem.feature.swap.NoOpSwapFeedbackRepository
import com.tangem.feature.swap.DefaultSwapTransactionRepository
import com.tangem.feature.swap.converters.ErrorsDataConverter
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class SwapDataModule {

    @Provides
    @Singleton
    internal fun provideSwapRepository(
        tangemExpressApi: TangemExpressApi,
        coroutineDispatcher: CoroutineDispatcherProvider,
        dataSignature: DataSignatureVerifier,
        errorsDataConverter: ErrorsDataConverter,
        @NetworkMoshi moshi: Moshi,
        appPreferencesStore: AppPreferencesStore,
        expressHistoryDao: ExpressHistoryDao,
        txHistoryFeatureToggles: TxHistoryFeatureToggles,
    ): SwapRepository {
        return DefaultSwapRepository(
            tangemExpressApi = tangemExpressApi,
            coroutineDispatcher = coroutineDispatcher,
            errorsDataConverter = errorsDataConverter,
            dataSignatureVerifier = dataSignature,
            moshi = moshi,
            appPreferencesStore = appPreferencesStore,
            expressHistoryDao = expressHistoryDao,
            txHistoryFeatureToggles = txHistoryFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        networkFactory: NetworkFactory,
        singleAccountListSupplier: SingleAccountListSupplier,
        dispatcherProvider: CoroutineDispatcherProvider,
    ): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
            networkFactory = networkFactory,
            singleAccountListSupplier = singleAccountListSupplier,
            dispatchers = dispatcherProvider,
        )
    }

    @Provides
    @Singleton
    internal fun provideErrorsConverter(@NetworkMoshi moshi: Moshi): ErrorsDataConverter {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return ErrorsDataConverter(jsonAdapter)
    }

    @Provides
    @Singleton
    internal fun provideSwapFeedbackRepository(
        api: SurveySparrowApi,
        environmentConfig: EnvironmentConfig,
    ): SwapFeedbackRepository {
        val rating = environmentConfig.surveySparrowSwapRating
            ?: return NoOpSwapFeedbackRepository()
        return DefaultSwapFeedbackRepository(
            api = api,
            surveyId = rating.surveyId,
            ratingQuestionId = rating.ratingQuestionId,
            feedbackQuestionId = rating.feedbackQuestionId,
        )
    }
}