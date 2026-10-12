package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import com.tangem.spend.datasource.pay.models.request.BankOfframpPlaceOrderRequest

/**
 * Builds the create-order body. A saved recipient is sent by id alone; a new one is described in full, and the
 * backend requires every field of that description while the domain model allows a recipient to be partial — so
 * anything missing ends the call as [BankOfframpError.Validation] instead of reaching the wire half-filled.
 */
internal object BankOfframpOrderRequestConverter {

    private const val INDIVIDUAL_TYPE = "INDIVIDUAL"
    private const val BUSINESS_TYPE = "BUSINESS"

    fun convert(
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpPlaceOrderRequest> = either {
        val data = when (val recipient = request.recipient) {
            is BankOfframpRecipient.Saved -> data(request = request, bankAccountId = recipient.bankAccountId)
            is BankOfframpRecipient.New -> data(
                request = request,
                bankAccount = convertBankAccount(details = recipient.details, rail = request.rail),
            )
        }
        BankOfframpPlaceOrderRequest(data = data, idempotencyKey = idempotencyKey)
    }

    private fun data(
        request: BankOfframpOrderRequest,
        bankAccountId: String? = null,
        bankAccount: BankOfframpPlaceOrderRequest.BankAccount? = null,
    ) = BankOfframpPlaceOrderRequest.Data(
        chainId = request.chainId,
        amountInCents = request.amountInCents,
        tokenContractAddress = request.tokenContractAddress,
        bankAccountId = bankAccountId,
        bankAccount = bankAccount,
    )

    /** The rail comes from the route the customer picked, not from the recipient the wizard has just described. */
    private fun Raise<BankOfframpError>.convertBankAccount(details: BankAccountDetails, rail: BankRail) =
        BankOfframpPlaceOrderRequest.BankAccount(
            details = BankOfframpPlaceOrderRequest.Details(
                currency = required(details.currency),
                rail = rail.name,
                isThirdParty = details.isThirdParty,
                beneficiary = convertBeneficiary(details.beneficiary),
                beneficiaryAddress = convertAddress(required(details.beneficiaryAddress)),
                bankName = required(details.bankName),
                bankAddress = convertAddress(required(details.bankAddress)),
                accountNumber = details.accountNumber,
                routingNumber = required(details.routingNumber),
                bankAccountType = required(details.accountType).name,
            ),
        )

    private fun convertBeneficiary(value: Beneficiary) = when (value) {
        is Beneficiary.Individual -> BankOfframpPlaceOrderRequest.Beneficiary(
            type = INDIVIDUAL_TYPE,
            firstName = value.firstName,
            lastName = value.lastName,
        )
        is Beneficiary.Business -> BankOfframpPlaceOrderRequest.Beneficiary(
            type = BUSINESS_TYPE,
            businessName = value.name,
        )
    }

    private fun convertAddress(value: PostalAddress) = BankOfframpPlaceOrderRequest.Address(
        line1 = value.line1,
        line2 = value.line2,
        city = value.city,
        region = value.region,
        postalCode = value.postalCode,
        countryCode = value.countryCode,
    )

    private fun <T : Any> Raise<BankOfframpError>.required(value: T?): T =
        ensureNotNull(value) { BankOfframpError.Validation }
}