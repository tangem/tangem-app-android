package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import com.tangem.spend.datasource.config.TangemPay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The mocked BFF serves recipients and routes but has no order endpoints, so against it a withdrawal is placed
 * in memory by [MockBankOfframpOrderRepository]; every other environment goes to the backend.
 */
@Singleton
internal class MockAwareBankOfframpOrderRepository @Inject constructor(
    private val real: DefaultBankOfframpOrderRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : BankOfframpOrderRepository {

    private val mock = MockBankOfframpOrderRepository()

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    private val delegate: BankOfframpOrderRepository
        get() = if (isMockMode) mock else real

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> = delegate.createOrder(
        userWalletId = userWalletId,
        request = request,
        idempotencyKey = idempotencyKey,
    )

    override suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<BankOfframpError, BankOfframpOrderState> = delegate.getOrderState(
        userWalletId = userWalletId,
        orderId = orderId,
    )

    override suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, Unit> = delegate.submitSignature(
        userWalletId = userWalletId,
        orderId = orderId,
        signature = signature,
        idempotencyKey = idempotencyKey,
    )

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit> =
        delegate.cancelOrder(userWalletId = userWalletId, orderId = orderId)
}