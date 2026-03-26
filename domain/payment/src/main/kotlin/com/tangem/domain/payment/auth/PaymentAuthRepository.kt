package com.tangem.domain.payment.auth

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.payment.models.auth.PaymentAuthConfig

/**
 * Repository for managing payment authentication and credential produce.
 * Supports multiple payment products (Tangem Pay, Virtual Accounts, possible future products).
 */
interface PaymentAuthRepository {

    /**
     * Checks if initial authentication data exists for the wallet.
     */
    suspend fun isInitialDataProduced(userWalletId: UserWalletId): Boolean

    /**
     * Produces initial credentials: derives address, obtains challenge, signs it,
     * and exchanges for auth tokens. Stores everything locally.
     */
    suspend fun produceInitialData(userWalletId: UserWalletId, config: PaymentAuthConfig)
}

/**
 * Factory for creating [PaymentAuthRepository] with custom implementations.
 * Allows each payment product to inject its own [PaymentRemoteDataSource] and [PaymentAuthStorage].
 *
 * Examples:
 * factory.create(TangemPayRemoteDataSource(), TangemPayStorage())
 * factory.create(VirtualAccountRemoteDataSource(), VirtualAccountStorage())
 */
interface PaymentAuthRepositoryFactory {

    /**
     * Creates a repository with the specified data source and storage implementations
     */
    fun create(remoteDataSource: PaymentRemoteDataSource, storage: PaymentAuthStorage): PaymentAuthRepository
}