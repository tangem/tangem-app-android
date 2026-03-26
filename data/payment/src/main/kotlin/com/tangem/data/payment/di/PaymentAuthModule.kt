package com.tangem.data.payment.di

import com.tangem.data.payment.datasource.PaymentColdWalletSdkManager
import com.tangem.data.payment.datasource.PaymentHotWalletSdkManager
import com.tangem.data.payment.repository.DefaultPaymentAuthRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.payment.auth.PaymentAuthRepository
import com.tangem.domain.payment.auth.PaymentAuthRepositoryFactory
import com.tangem.domain.payment.auth.PaymentAuthStorage
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PaymentAuthModule {

    @Provides
    @Singleton
    fun providePaymentHotSdkManagerFactory(
        tangemHotSdk: TangemHotSdk,
        hotWalletAccessor: HotWalletAccessor,
    ): PaymentHotWalletSdkManager.Factory {
        return object : PaymentHotWalletSdkManager.Factory {
            override fun create(remoteDataSource: PaymentRemoteDataSource): PaymentHotWalletSdkManager {
                return PaymentHotWalletSdkManager(remoteDataSource, tangemHotSdk, hotWalletAccessor)
            }
        }
    }

    @Provides
    @Singleton
    fun providePaymentAuthRepositoryFactory(
        dispatchers: CoroutineDispatcherProvider,
        userWalletsListRepository: UserWalletsListRepository,
        paymentHotWalletSdkManagerFactory: PaymentHotWalletSdkManager.Factory,
        paymentColdWalletSdkManagerFactory: PaymentColdWalletSdkManager.Factory,
    ): PaymentAuthRepositoryFactory {
        return object : PaymentAuthRepositoryFactory {
            override fun create(
                remoteDataSource: PaymentRemoteDataSource,
                storage: PaymentAuthStorage,
            ): PaymentAuthRepository {
                return DefaultPaymentAuthRepository(
                    dispatchers = dispatchers,
                    remoteDataSource = remoteDataSource,
                    storage = storage,
                    userWalletsListRepository = userWalletsListRepository,
                    paymentHotWalletSdkManagerFactory = paymentHotWalletSdkManagerFactory,
                    paymentColdWalletSdkManagerFactory = paymentColdWalletSdkManagerFactory,
                )
            }
        }
    }
}