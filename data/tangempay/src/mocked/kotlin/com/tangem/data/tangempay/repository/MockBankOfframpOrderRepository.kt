package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.data.tangempay.util.BankOfframpQuoteRequestConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.bankofframp.BankAccountDetails
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRoute
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpTransfer
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.Beneficiary
import com.tangem.domain.tangempay.model.bankofframp.OfframpEta
import com.tangem.domain.tangempay.model.bankofframp.OfframpFee
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * In-memory stand-in for placing a withdrawal — order, route, quote and transfer — while those endpoints of the
 * bank off-ramp BFF are still being agreed. Recipients and rails come from the backend, plus the SPEI ones of
 * [MOCK_SPEI_RECIPIENT] and [MOCK_SPEI_DESTINATION].
 *
 * Orders move `Processing → AwaitingSignature` by wall-clock age and `→ Completed` once signed; a few request
 * values trigger each backend error or outcome so every path is testable from the UI. Only the details the
 * wizard has typed in are checked — for a recipient the customer picked from the saved list, the backend is
 * the one that validates.
 */
internal class MockBankOfframpOrderRepository(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BankOfframpOrderRepository {

    private val mutex = Mutex()
    private val orders = mutableMapOf<String, MockOrder>()
    private val routes = mutableMapOf<String, BankOfframpRoute>()
    private val quotes = mutableMapOf<String, BankOfframpQuote>()
    private val transfers = mutableMapOf<String, BankOfframpTransfer>()
    private var nextOrderIndex = 1
    private var nextBankAccountIndex = 1
    private var nextQuoteIndex = 1
    private var nextTransferIndex = 1

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> = mutex.withLock {
        either {
            triggeredError(request)?.let { raise(it) }
            val orderId = "$ORDER_ID_PREFIX${nextOrderIndex++}"
            orders[orderId] = MockOrder(createdAtMillis = currentTimeMillis(), outcome = triggeredOutcome(request))
            BankOfframpOrderResult(
                orderId = orderId,
                bankAccountId = (request.recipient as? BankOfframpRecipient.Saved)?.bankAccountId,
            )
        }
    }

    override suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
        request: BankOfframpOrderRequest,
    ): Either<BankOfframpError, BankOfframpOrderState> = mutex.withLock {
        val order = orders[orderId] ?: return@withLock unknownOrder(orderId).left()
        stateOf(orderId, order).right()
    }

    override suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, Unit> = mutex.withLock {
        either {
            val order = orders[orderId] ?: raise(unknownOrder(orderId))
            ensure(stateOf(orderId, order) is BankOfframpOrderState.AwaitingSignature) {
                BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId is not awaiting a signature"))
            }
            orders[orderId] = order.copy(isSigned = true)
        }
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit> {
        return mutex.withLock {
            either {
                val order = orders[orderId] ?: raise(unknownOrder(orderId))
                ensure(!order.isSigned) {
                    BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId is already completed"))
                }
                orders[orderId] = order.copy(isCanceled = true)
            }
        }
    }

    override suspend fun createRoute(
        userWalletId: UserWalletId,
        productInstanceId: String,
        rail: BankRail,
        recipient: BankOfframpRecipient,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpRoute> = mutex.withLock {
        either {
            (recipient as? BankOfframpRecipient.New)?.details?.triggeredError()?.let { raise(it) }
            routes.getOrPut(idempotencyKey) { createMockRoute(rail = rail, recipient = recipient) }
        }
    }

    private fun Raise<BankOfframpError>.createMockRoute(
        rail: BankRail,
        recipient: BankOfframpRecipient,
    ): BankOfframpRoute {
        val (fee, eta) = when (rail) {
            BankRail.ACH -> ACH_FEE to ACH_ETA
            BankRail.WIRE -> WIRE_FEE to WIRE_ETA
            BankRail.SPEI -> raise(
                BankOfframpError.Unknown(cause = IllegalArgumentException("SPEI is withdrawn by quote, not by route")),
            )
        }
        return BankOfframpRoute(
            depositAddress = FAKE_ROUTE_DEPOSIT_ADDRESS,
            fee = fee,
            eta = eta,
            bankAccountId = bankAccountIdOf(recipient),
        )
    }

    override suspend fun createQuote(
        userWalletId: UserWalletId,
        productInstanceId: String,
        amount: BigDecimal,
        recipient: BankOfframpRecipient,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpQuote> = mutex.withLock {
        either {
            BankOfframpQuoteRequestConverter.convert(
                BankOfframpQuoteRequestConverter.Input(
                    productInstanceId = productInstanceId,
                    amount = amount,
                    recipient = recipient,
                ),
            ).bind()
            amountTriggeredError(amountInCents = amount.movePointRight(2).toLong())?.let { raise(it) }
            quotes.getOrPut(idempotencyKey) { createMockQuote(amount = amount, recipient = recipient) }
        }
    }

    override suspend fun createTransfer(
        userWalletId: UserWalletId,
        quoteId: String,
    ): Either<BankOfframpError, BankOfframpTransfer> = mutex.withLock {
        either {
            transfers.getOrPut(quoteId) {
                val quote = quotes.values.firstOrNull { it.quoteId == quoteId }
                    ?: raise(BankOfframpError.Unknown(cause = IllegalArgumentException("Unknown quote $quoteId")))
                ensure(currentTimeMillis() < quote.expiresAt.millis) {
                    BankOfframpError.Unknown(cause = IllegalStateException("Quote $quoteId has expired"))
                }
                BankOfframpTransfer(
                    transferId = "$TRANSFER_ID_PREFIX${nextTransferIndex++}",
                    depositAddress = FAKE_TRANSFER_DEPOSIT_ADDRESS,
                    sourceAmount = quote.sourceAmount,
                    destinationAmount = quote.destinationAmount,
                    expiresAt = quote.expiresAt,
                )
            }
        }
    }

    private fun createMockQuote(amount: BigDecimal, recipient: BankOfframpRecipient): BankOfframpQuote {
        val fee = MOCK_SPEI_DESTINATION.fee
        val feeAmount = fee.amountFor(amount)
        return BankOfframpQuote(
            quoteId = "$QUOTE_ID_PREFIX${nextQuoteIndex++}",
            bankAccountId = bankAccountIdOf(recipient),
            feeAmount = feeAmount,
            sourceAmount = amount + feeAmount,
            destinationAmount = amount.multiply(MXN_PER_USD).setScale(2, RoundingMode.HALF_UP),
            destinationCurrency = MXN,
            exchangeRate = MXN_PER_USD,
            fee = fee,
            eta = MOCK_SPEI_DESTINATION.eta,
            expiresAt = DateTime(currentTimeMillis() + QUOTE_LIFETIME_MILLIS),
        )
    }

    private fun bankAccountIdOf(recipient: BankOfframpRecipient): String = when (recipient) {
        is BankOfframpRecipient.Saved -> recipient.bankAccountId
        is BankOfframpRecipient.New -> "$NEW_BANK_ACCOUNT_ID_PREFIX${nextBankAccountIndex++}"
    }

    private fun stateOf(orderId: String, order: MockOrder): BankOfframpOrderState {
        val isProcessing = currentTimeMillis() - order.createdAtMillis < AWAITING_SIGNATURE_DELAY_MILLIS
        return when {
            order.isCanceled -> BankOfframpOrderState.Canceled
            order.isSigned -> BankOfframpOrderState.Completed
            isProcessing || order.outcome == MockOutcome.STAYS_PROCESSING ->
                BankOfframpOrderState.Processing(OrderStep.UNKNOWN)
            order.outcome == MockOutcome.FAILS -> BankOfframpOrderState.Failed(
                BankOfframpError.Unknown(cause = IllegalStateException("Mock order $orderId failed on the backend")),
            )
            else -> BankOfframpOrderState.AwaitingSignature(payloadHash = FAKE_PAYLOAD_HASH)
        }
    }

    private fun triggeredError(request: BankOfframpOrderRequest): BankOfframpError? {
        val details = (request.recipient as? BankOfframpRecipient.New)?.details
        return details?.triggeredError() ?: amountTriggeredError(request.amountInCents)
    }

    private fun amountTriggeredError(amountInCents: Long): BankOfframpError? = when (amountInCents) {
        SENDER_REGION_TRIGGER_CENTS -> BankOfframpError.SenderRegionUnsupported
        WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS -> BankOfframpError.WithdrawalInProgress
        else -> null
    }

    private fun BankAccountDetails.triggeredError(): BankOfframpError? {
        val beneficiary = beneficiary
        return when {
            routingNumber == INVALID_ROUTING_NUMBER -> BankOfframpError.Validation
            beneficiaryAddress?.countryCode != SUPPORTED_COUNTRY_CODE -> BankOfframpError.RecipientRegionUnsupported
            beneficiary is Beneficiary.Business && beneficiary.name.equals(BLOCKED_BUSINESS_NAME, ignoreCase = true) ->
                BankOfframpError.BusinessUnsupported
            else -> null
        }
    }

    private fun triggeredOutcome(request: BankOfframpOrderRequest): MockOutcome = when (request.amountInCents) {
        FAILED_TRIGGER_CENTS -> MockOutcome.FAILS
        STUCK_TRIGGER_CENTS -> MockOutcome.STAYS_PROCESSING
        else -> MockOutcome.AWAITS_SIGNATURE
    }

    private fun unknownOrder(orderId: String) = BankOfframpError.Unknown(
        cause = IllegalArgumentException("Unknown bank off-ramp order $orderId"),
    )

    private data class MockOrder(
        val createdAtMillis: Long,
        val outcome: MockOutcome,
        val isSigned: Boolean = false,
        val isCanceled: Boolean = false,
    )

    private enum class MockOutcome { AWAITS_SIGNATURE, FAILS, STAYS_PROCESSING }

    private companion object {
        const val ORDER_ID_PREFIX = "mock-bank-offramp-order-"
        const val FAKE_PAYLOAD_HASH = "0x5f3c4d3e2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b7a6f5e"
        const val AWAITING_SIGNATURE_DELAY_MILLIS = 6_000L
        const val INVALID_ROUTING_NUMBER = "000000001"
        const val SUPPORTED_COUNTRY_CODE = "US"
        const val SENDER_REGION_TRIGGER_CENTS = 66_600L
        const val WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS = 77_700L
        const val FAILED_TRIGGER_CENTS = 88_800L
        const val STUCK_TRIGGER_CENTS = 99_900L
        const val BLOCKED_BUSINESS_NAME = "Blocked"
        const val NEW_BANK_ACCOUNT_ID_PREFIX = "mock-bank-account-new-"
        const val FAKE_ROUTE_DEPOSIT_ADDRESS = "0x0000000000000000000000000000000000000003"
        const val FAKE_TRANSFER_DEPOSIT_ADDRESS = "0x0000000000000000000000000000000000000004"
        const val QUOTE_ID_PREFIX = "mock-bank-offramp-quote-"
        const val TRANSFER_ID_PREFIX = "mock-bank-offramp-transfer-"
        const val QUOTE_LIFETIME_MILLIS = 15 * 60_000L
        const val MXN = "MXN"
        val MXN_PER_USD = BigDecimal("17.25")
        const val USD = "USD"
        val ACH_FEE = fixedFee(name = "ACH", amount = "1.50")
        val ACH_ETA = OfframpEta(minDays = 1, maxDays = 3)
        val WIRE_FEE = fixedFee(name = "FedWire", amount = "15.00")
        val WIRE_ETA = OfframpEta(minDays = 0, maxDays = 1)

        fun fixedFee(name: String, amount: String) = OfframpFee(
            name = name,
            calculationType = AccountFee.CalculationType.FIXED,
            amount = BigDecimal(amount),
            currency = USD,
            description = null,
        )
    }
}