package com.tangem.data.transaction.di

import com.tangem.core.remote.ApiSerialization
import com.tangem.core.remote.RetrofitApiSpec
import com.tangem.core.remote.RetrofitFactory
import com.tangem.core.remote.build
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.data.transaction.DefaultSendIntentRepository
import com.tangem.data.transaction.api.TransactionIntentApi
import com.tangem.data.transaction.convertes.SendIntentConverter
import com.tangem.data.transaction.convertes.SendIntentErrorConverter
import com.tangem.domain.transaction.SendIntentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SendIntentDataModule {

    @Provides
    @Singleton
    fun provideTransactionIntentApi(factory: RetrofitFactory): TransactionIntentApi {
        return factory.build(
            RetrofitApiSpec(
                apiConfigId = TangemGatewayApiConfig.TANGEM_TECH_ID,
                shouldApplyTimeoutAnnotations = false,
                shouldUseSessionAuth = true,
                serialization = ApiSerialization.KOTLINX,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideSendIntentRepository(
        transactionIntentApi: TransactionIntentApi,
        sendIntentConverter: SendIntentConverter,
        sendIntentErrorConverter: SendIntentErrorConverter,
    ): SendIntentRepository {
        return DefaultSendIntentRepository(
            transactionIntentApi = transactionIntentApi,
            sendIntentConverter = sendIntentConverter,
            sendIntentErrorConverter = sendIntentErrorConverter,
        )
    }
}