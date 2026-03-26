package com.tangem.data.payment.repository

import arrow.core.Either
import com.tangem.data.payment.datasource.PaymentColdWalletSdkManager
import com.tangem.data.payment.datasource.PaymentHotWalletSdkManager
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.payment.models.auth.PaymentAuthConfig
import com.tangem.domain.payment.models.auth.PaymentAuthTokens
import com.tangem.domain.payment.auth.PaymentAuthRepository
import com.tangem.domain.payment.auth.PaymentAuthStorage
import com.tangem.domain.payment.models.auth.PaymentInitialCredentials
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultPaymentAuthRepository(
    private val dispatchers: CoroutineDispatcherProvider,
    private val remoteDataSource: PaymentRemoteDataSource,
    private val storage: PaymentAuthStorage,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val paymentHotWalletSdkManagerFactory: PaymentHotWalletSdkManager.Factory,
    private val paymentColdWalletSdkManagerFactory: PaymentColdWalletSdkManager.Factory,
) : PaymentAuthRepository {

    private val paymentHotWalletSdkManager by lazy(mode = LazyThreadSafetyMode.NONE) {
        paymentHotWalletSdkManagerFactory.create(remoteDataSource)
    }

    private val paymentColdWalletSdkManager by lazy(mode = LazyThreadSafetyMode.NONE) {
        paymentColdWalletSdkManagerFactory.create(remoteDataSource)
    }

    override suspend fun isInitialDataProduced(userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) {
            val customerWalletAddress =
                storage.getCustomerWalletAddress(userWalletId) ?: return@withContext false
            storage.getAuthTokens(customerWalletAddress) ?: return@withContext false

            return@withContext true
        }
    }

    override suspend fun produceInitialData(userWalletId: UserWalletId, config: PaymentAuthConfig) {
        withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.userWallets.value?.firstOrNull { it.walletId == userWalletId }
                ?: error("No User Wallet found")

            val initialCredentials = produceInitialCredentials(userWallet, config)
                .fold(
                    ifLeft = { error("Can not produce initial data: ${it.message}") },
                    ifRight = { it },
                )

            storage.storeCustomerWalletAddress(
                userWalletId = userWallet.walletId,
                customerWalletAddress = initialCredentials.customerWalletAddress,
            )
            storage.storeAuthTokens(
                customerWalletAddress = initialCredentials.customerWalletAddress,
                tokens = PaymentAuthTokens(
                    accessToken = initialCredentials.authTokens.accessToken,
                    expiresAt = initialCredentials.authTokens.expiresAt,
                    refreshToken = initialCredentials.authTokens.refreshToken,
                    refreshExpiresAt = initialCredentials.authTokens.refreshExpiresAt,
                    idempotencyKey = initialCredentials.authTokens.idempotencyKey,
                ),
            )
        }
    }

    private suspend fun produceInitialCredentials(
        userWallet: UserWallet,
        config: PaymentAuthConfig,
    ): Either<Throwable, PaymentInitialCredentials> {
        return when (userWallet) {
            is UserWallet.Cold -> paymentColdWalletSdkManager
                .produceInitialCredentials(coldWallet = userWallet, config = config)
            is UserWallet.Hot -> paymentHotWalletSdkManager
                .produceInitialCredentials(hotWallet = userWallet, config = config)
        }
    }
}