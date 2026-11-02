package com.tangem.blockchainsdk.di

import com.tangem.blockchain.common.ExceptionHandlerOutput
import com.tangem.blockchainsdk.analytics.BlockchainExceptionHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface BlockchainAnalyticsModule {

    @Binds
    fun bindExceptionHandlerOutput(handler: BlockchainExceptionHandler): ExceptionHandlerOutput
}