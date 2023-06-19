package com.tangem.tap.domain.walletconnect2.di

import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper
import com.tangem.tap.domain.walletconnect2.app.TangemWcBlockchainHelper
import com.tangem.tap.domain.walletconnect2.app.WalletConnectEventsHandlerImpl
import com.tangem.tap.domain.walletconnect2.data.WalletConnectRepositoryImpl
import com.tangem.tap.domain.walletconnect2.data.WalletConnectSessionsRepositoryImpl
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectRepository
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
class WalletConnectInteractorModule {
    @Provides
    @ActivityScoped
    fun provideWalletConnectInteractor(
        wcRepository: WalletConnectRepository,
        wcSessionsRepository: WalletConnectSessionsRepository,
    ): WalletConnectInteractor {
        return WalletConnectInteractor(
            handler = WalletConnectEventsHandlerImpl(),
            walletConnectRepository = wcRepository,
            sessionsRepository = wcSessionsRepository,
            sdkHelper = WalletConnectSdkHelper(),
            blockchainHelper = TangemWcBlockchainHelper(),
            dispatcher = AppCoroutineDispatcherProvider(),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WalletConnectModule {

    @Binds
    @Singleton
    fun bindWalletConnectRepository(repository: WalletConnectRepositoryImpl): WalletConnectRepository

    @Binds
    @Singleton
    fun bindWalletConnectSessionsRepository(
        repository: WalletConnectSessionsRepositoryImpl,
    ): WalletConnectSessionsRepository
}