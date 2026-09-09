package com.tangem.data.pay.repository

import com.tangem.domain.pay.model.bankofframp.AmountLimits
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.domain.pay.model.bankofframp.OfframpToken
import com.tangem.domain.pay.model.bankofframp.PostalAddress
import java.math.BigDecimal

/** Recipients and options the mock repository starts with; names follow the Figma mockups. */
internal object MockBankOfframpSeed {

    const val ACCOUNT_ID_PREFIX = "mock-bank-account-"
    private const val ACH_MAX_DAYS = 3

    val accounts: List<BankAccount> = listOf(
        individual(index = 1, firstName = "Basit", lastName = "Didenko", accountNumber = "4830019271"),
        individual(index = 2, firstName = "Saburo", lastName = "Arasaka", accountNumber = "7719004432"),
        business(index = 3, name = "Raxanax", accountNumber = "1120938475"),
        individual(index = 4, firstName = "Artem", lastName = "Toshmukhamedov", accountNumber = "5590013378"),
        individual(index = 5, firstName = "Serhii", lastName = "Buinov", accountNumber = "3301198822"),
    )

    val options: BankOfframpOptions = BankOfframpOptions(
        sources = listOf(
            OfframpSource(
                networkName = "Polygon",
                isTestnet = false,
                chainId = 137L,
                tokens = listOf(
                    OfframpToken(symbol = "USDC", contractAddress = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"),
                ),
            ),
            OfframpSource(
                networkName = "Base",
                isTestnet = false,
                chainId = 84532L,
                tokens = listOf(
                    OfframpToken(symbol = "USDC", contractAddress = "0x036CbD53842c5426634e7929541eC2318f3dCF7e"),
                ),
            ),
            OfframpSource(
                networkName = "Base",
                isTestnet = false,
                chainId = 84532L,
                tokens = listOf(
                    OfframpToken(symbol = "USDC", contractAddress = "0x10b5be494c2962a7b318afb63f0ee30b959d000b"),
                ),
            ),
            OfframpSource(
                networkName = "Arbitrum One",
                isTestnet = false,
                chainId = 42161L,
                tokens = listOf(
                    OfframpToken(symbol = "USDC", contractAddress = "0xaf88d065e77c8cC2239327C5EDb3A432268e5831"),
                ),
            ),
        ),
        destinations = listOf(
            OfframpDestination(
                rail = BankRail.ACH,
                fee = OfframpFee(amount = BigDecimal("1.00"), currency = "USD", description = "ACH transfer fee"),
                eta = OfframpEta(minDays = 1, maxDays = ACH_MAX_DAYS),
            ),
            OfframpDestination(
                rail = BankRail.WIRE,
                fee = OfframpFee(amount = BigDecimal("11.00"), currency = "USD", description = "FedWire transfer fee"),
                eta = OfframpEta(minDays = 0, maxDays = 0),
            ),
        ),
        amountLimits = AmountLimits(min = BigDecimal("1.00"), max = BigDecimal("10000.00")),
    )

    fun displayName(beneficiary: Beneficiary): String = when (beneficiary) {
        is Beneficiary.Individual -> "${beneficiary.firstName} ${beneficiary.lastName}"
        is Beneficiary.Business -> beneficiary.name
    }

    private fun individual(index: Int, firstName: String, lastName: String, accountNumber: String): BankAccount {
        return account(
            index = index,
            beneficiary = Beneficiary.Individual(firstName, lastName),
            accountNumber = accountNumber,
        )
    }

    private fun business(index: Int, name: String, accountNumber: String): BankAccount {
        return account(index = index, beneficiary = Beneficiary.Business(name), accountNumber = accountNumber)
    }

    private fun account(index: Int, beneficiary: Beneficiary, accountNumber: String): BankAccount {
        val details = BankAccountDetails(
            currency = "USD",
            rail = BankRail.ACH,
            isThirdParty = false,
            beneficiary = beneficiary,
            beneficiaryAddress = usAddress(line1 = "$index Main St"),
            bankName = "First Mock Bank",
            bankAddress = usAddress(line1 = "100 Bank Plaza"),
            accountNumber = accountNumber,
            routingNumber = "021000021",
            accountType = BankAccountType.CHECKING,
        )
        return BankAccount(id = "$ACCOUNT_ID_PREFIX$index", displayName = displayName(beneficiary), details = details)
    }

    private fun usAddress(line1: String) = PostalAddress(
        line1 = line1,
        line2 = null,
        city = "New York",
        region = "NY",
        postalCode = "10001",
        countryCode = "US",
    )
}