package com.tangem.domain.pay.usecase.bankofframp

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.PostalAddress

internal val TEST_USER_WALLET_ID = UserWalletId("0123456789abcdef")

internal fun postalAddress(
    line1: String = "1 Main St",
    city: String = "New York",
    region: String = "NY",
    postalCode: String = "10001",
    countryCode: String = "US",
): PostalAddress = PostalAddress(
    line1 = line1,
    line2 = null,
    city = city,
    region = region,
    postalCode = postalCode,
    countryCode = countryCode,
)

internal fun bankAccountDetails(
    beneficiary: Beneficiary = Beneficiary.Individual(firstName = "John", lastName = "Doe"),
    routingNumber: String = "021000021",
    beneficiaryAddress: PostalAddress = postalAddress(),
): BankAccountDetails = BankAccountDetails(
    currency = "USD",
    rail = BankRail.ACH,
    isThirdParty = false,
    beneficiary = beneficiary,
    beneficiaryAddress = beneficiaryAddress,
    bankName = "Test Bank",
    bankAddress = postalAddress(line1 = "2 Bank St"),
    accountNumber = "1234567890",
    routingNumber = routingNumber,
    accountType = BankAccountType.CHECKING,
)

internal fun bankAccount(
    id: String = "acc-1",
    displayName: String = "John Doe",
    details: BankAccountDetails = bankAccountDetails(),
): BankAccount = BankAccount(id = id, displayName = displayName, details = details)

internal fun orderRequest(
    amountInCents: Long = 50_000L,
    recipient: BankOfframpRecipient = BankOfframpRecipient.Saved(bankAccountId = "acc-1"),
): BankOfframpOrderRequest = BankOfframpOrderRequest(
    amountInCents = amountInCents,
    chainId = 137L,
    tokenContractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359",
    rail = BankRail.ACH,
    recipient = recipient,
)