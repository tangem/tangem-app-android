package com.tangem.tap.di.domain

import com.tangem.domain.feedback.models.SupportChatInfoProvider
import com.tangem.domain.feedback.models.SupportChatTopic
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionPositionsValueSupplier
import com.tangem.domain.polymarket.support.PredictionSupportChatInfoProvider
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PredictionSupportChatModule {

    @Provides
    @Singleton
    @IntoMap
    @SupportChatTopicKey(SupportChatTopic.PREDICTIONS)
    fun providePredictionSupportChatInfoProvider(
        predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
        predictionPositionsValueSupplier: PredictionPositionsValueSupplier,
        derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    ): SupportChatInfoProvider = PredictionSupportChatInfoProvider(
        predictionAccountStatusSupplier = predictionAccountStatusSupplier,
        predictionPositionsValueSupplier = predictionPositionsValueSupplier,
        derivePolymarketAddressesUseCase = derivePolymarketAddressesUseCase,
    )
}