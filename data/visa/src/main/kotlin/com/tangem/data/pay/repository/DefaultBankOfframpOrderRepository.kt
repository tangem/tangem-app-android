package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.data.pay.util.BankOfframpErrorConverter
import com.tangem.data.pay.util.BankOfframpOrderRequestConverter
import com.tangem.data.pay.util.BankOfframpOrderStateConverter
import com.tangem.data.pay.util.BankOfframpQuoteConverter
import com.tangem.data.pay.util.BankOfframpQuoteRequestConverter
import com.tangem.data.pay.util.BankOfframpRouteConverter
import com.tangem.data.pay.util.BankOfframpRouteRequestConverter
import com.tangem.data.pay.util.BankOfframpTransferConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankOfframpRoute
import com.tangem.domain.pay.model.bankofframp.BankOfframpTransfer
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.request.BankOfframpSignatureRequest
import com.tangem.spend.datasource.pay.models.request.BankOfframpTransferRequest
import com.tangem.utils.extensions.addHexPrefix
import java.math.BigDecimal
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
        val body = BankOfframpOrderRequestConverter.convert(request).bind()
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
        request: BankOfframpOrderRequest,
    ): Either<BankOfframpError, BankOfframpOrderState> = either {
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error -> error.toBankOfframpError(operation = "getOrderState") }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "getOrderState") }
        BankOfframpOrderStateConverter.convert(BankOfframpOrderStateConverter.Input(result = result, request = request))
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

    override suspend fun createRoute(
        userWalletId: UserWalletId,
        productInstanceId: String,
        rail: BankRail,
        recipient: BankOfframpRecipient,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpRoute> = either {
        val body = BankOfframpRouteRequestConverter.convert(
            BankOfframpRouteRequestConverter.Input(
                productInstanceId = productInstanceId,
                rail = rail,
                recipient = recipient,
            ),
        ).bind()
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createBankOfframpRoute(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = body,
            )
        }.mapLeft { error -> error.toAccountOfframpError(operation = "createRoute") }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "createRoute") }
        ensureNotNull(BankOfframpRouteConverter.convert(result)) { incompleteResult(operation = "createRoute") }
    }

    override suspend fun createQuote(
        userWalletId: UserWalletId,
        productInstanceId: String,
        amount: BigDecimal,
        recipient: BankOfframpRecipient,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpQuote> = either {
        val body = BankOfframpQuoteRequestConverter.convert(
            BankOfframpQuoteRequestConverter.Input(
                productInstanceId = productInstanceId,
                amount = amount,
                recipient = recipient,
            ),
        ).bind()
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createBankOfframpQuote(
                authHeader = authHeader,
                idempotencyKey = idempotencyKey,
                body = body,
            )
        }.mapLeft { error -> error.toAccountOfframpError(operation = "createQuote") }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "createQuote") }
        ensureNotNull(BankOfframpQuoteConverter.convert(result)) { incompleteResult(operation = "createQuote") }
    }

    override suspend fun createTransfer(
        userWalletId: UserWalletId,
        quoteId: String,
    ): Either<BankOfframpError, BankOfframpTransfer> = either {
        val response = requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.createBankOfframpTransfer(
                authHeader = authHeader,
                body = BankOfframpTransferRequest(quoteId = quoteId),
            )
        }.mapLeft { error -> error.toTransferError() }.bind()

        val result = ensureNotNull(response.result) { emptyResult(operation = "createTransfer") }
        ensureNotNull(BankOfframpTransferConverter.convert(result)) { incompleteResult(operation = "createTransfer") }
    }

    private fun VisaApiError.toBankOfframpError(operation: String) =
        BankOfframpErrorConverter.convert(BankOfframpErrorConverter.Input(error = this, operation = operation))

    private fun VisaApiError.toAccountOfframpError(operation: String): BankOfframpError =
        if (this == VisaApiError.NotFound || errorCode == FORBIDDEN_ERROR_CODE) {
            BankOfframpError.Unavailable(errorCode = errorCode)
        } else {
            toBankOfframpError(operation = operation)
        }

    private fun VisaApiError.toTransferError(): BankOfframpError = if (errorCode == FORBIDDEN_ERROR_CODE) {
        BankOfframpError.Unavailable(errorCode = errorCode)
    } else {
        toBankOfframpError(operation = "createTransfer")
    }

    private fun emptyResult(operation: String) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp $operation returned an empty result"),
    )

    private fun incompleteResult(operation: String) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp $operation returned an incomplete result"),
    )
}

private const val FORBIDDEN_ERROR_CODE = 104_120_100