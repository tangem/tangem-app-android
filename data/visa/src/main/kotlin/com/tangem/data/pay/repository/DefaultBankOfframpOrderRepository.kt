package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.pay.util.BankOfframpErrorConverter
import com.tangem.data.pay.util.BankOfframpOrderRequestConverter
import com.tangem.data.pay.util.BankOfframpOrderStateConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.BankOfframpSignatureRequest
import com.tangem.utils.extensions.addHexPrefix
import javax.inject.Inject

internal class DefaultBankOfframpOrderRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestPerformer: TangemPayRequestPerformer,
) : BankOfframpOrderRepository {

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> = either {
        val body = BankOfframpOrderRequestConverter.convert(
            request = request,
            idempotencyKey = idempotencyKey,
        ).bind()
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createBankOfframpOrder(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = body,
            )
        }.mapLeft { error -> error.toBankOfframpError(operation = "createOrder") }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "createOrder") }
        BankOfframpOrderResult(orderId = result.id, bankAccountId = result.data.bankAccountId)
    }

    override suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<BankOfframpError, BankOfframpOrderState> = either {
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error -> error.toBankOfframpError(operation = "getOrderState") }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "getOrderState") }
        BankOfframpOrderStateConverter.convert(result)
    }

    override suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, Unit> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.submitBankOfframpSignature(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                orderId = orderId,
                body = BankOfframpSignatureRequest(signature = signature.addHexPrefix()),
            )
        }
            .map {}
            .mapLeft { error -> error.toBankOfframpError(operation = "submitSignature") }
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.cancelOrder(authHeader = authHeader, orderId = orderId)
        }
            .map {}
            .mapLeft { error -> error.toBankOfframpError(operation = "cancelOrder") }
    }

    private fun VisaApiError.toBankOfframpError(operation: String) =
        BankOfframpErrorConverter.convert(error = this, operation = operation)

    private fun emptyResult(operation: String) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp $operation returned an empty result"),
    )
}