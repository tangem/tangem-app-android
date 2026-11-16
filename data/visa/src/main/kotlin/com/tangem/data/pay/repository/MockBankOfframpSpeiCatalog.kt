package com.tangem.data.pay.repository

import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import java.math.BigDecimal

internal val MOCK_SPEI_DESTINATION = OfframpDestination(
    rail = BankRail.SPEI,
    currency = "MXN",
    fee = OfframpFee(
        name = "SPEI",
        calculationType = AccountFee.CalculationType.PERCENTAGE,
        amount = BigDecimal("1"),
        currency = "USD",
        description = null,
    ),
    eta = OfframpEta(minDays = 0, maxDays = 0),
)

internal val MOCK_SPEI_RECIPIENT = BankAccount(
    id = "mock-bank-account-spei",
    displayName = "Maria Lopez",
    details = BankAccountDetails(
        currency = "MXN",
        rail = BankRail.SPEI,
        isThirdParty = false,
        beneficiary = Beneficiary.Individual(firstName = "Maria", lastName = "Lopez"),
        beneficiaryAddress = null,
        bankName = null,
        bankAddress = null,
        accountNumber = "002010077777777771",
        routingNumber = null,
        accountType = null,
    ),
)

internal fun withMockSpeiRecipient(accounts: List<BankAccount>, rail: BankRail?): List<BankAccount> =
    (accounts + MOCK_SPEI_RECIPIENT)
        .distinctBy(BankAccount::id)
        .filter { account -> rail == null || account.details.rail == rail }

internal fun withMockSpeiDestination(options: BankOfframpOptions, product: TangemPayProduct): BankOfframpOptions {
    if (product != TangemPayProduct.BANKING_ACCOUNT) return options
    val destinations = (options.destinations + MOCK_SPEI_DESTINATION).distinctBy(OfframpDestination::rail)
    return options.copy(destinations = destinations)
}