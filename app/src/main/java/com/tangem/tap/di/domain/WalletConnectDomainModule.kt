package com.tangem.tap.di.domain

import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.walletconnect.repository.WalletConnectRepository
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectDomainModule {

    @Provides
    @Singleton
    fun providesCheckIsWalletConnectAvailableUseCase(
        walletConnectRepository: WalletConnectRepository,
    ): CheckIsWalletConnectAvailableUseCase {
        return CheckIsWalletConnectAvailableUseCase(walletConnectRepository = walletConnectRepository)
    }

    @Provides
    @Singleton
    fun providesWcSessionsUseCase(sessionsManager: WcSessionsManager): WcSessionsUseCase {
        return WcSessionsUseCase(sessionsManager)
    }
}