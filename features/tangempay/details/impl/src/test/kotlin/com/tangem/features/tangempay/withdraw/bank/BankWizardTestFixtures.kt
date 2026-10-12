package com.tangem.features.tangempay.withdraw.bank

import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.domain.pay.model.bankofframp.OfframpSource
import com.tangem.domain.pay.model.bankofframp.OfframpToken
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawSource
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency

internal const val POLYGON_CHAIN_ID = 137L
internal const val BASE_CHAIN_ID = 8453L
internal const val USDC_POLYGON = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
internal const val USDC_BASE = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"

/** A complete personal recipient's 14 form answers, shared by the new-recipient-draft tests that need one. */
internal val personalFormValues: Map<BankWithdrawFormField, String> = mapOf(
    BankWithdrawFormField.AccountNumber to "1234567890",
    BankWithdrawFormField.RoutingNumber to "021000021",
    BankWithdrawFormField.FirstName to "Saburo",
    BankWithdrawFormField.LastName to "Arasaka",
    BankWithdrawFormField.Country to "US",
    BankWithdrawFormField.Region to "CA",
    BankWithdrawFormField.City to "Night City",
    BankWithdrawFormField.AddressLine1 to "Megatower H10, 322F",
    BankWithdrawFormField.PostalCode to "90001",
    BankWithdrawFormField.BankName to "Arasaka Bank",
    BankWithdrawFormField.BankRegion to "CA",
    BankWithdrawFormField.BankCity to "Night City",
    BankWithdrawFormField.BankAddressLine1 to "Ebunike st., 32A",
    BankWithdrawFormField.BankPostalCode to "90002",
)

internal fun paymentToken(
    networkId: String = "polygon",
    networkName: String = "Polygon",
    contractAddress: String = USDC_POLYGON,
    symbol: String = "USDC",
): CryptoCurrency.Token = MockAccounts.createPaymentAccountToken(
    rawId = "$symbol-$networkId".lowercase(),
    contractAddress = contractAddress,
    symbol = symbol,
    networkId = networkId,
    networkName = networkName,
)

internal fun currencyStatus(token: CryptoCurrency.Token, fiatBalance: BigDecimal?): CryptoCurrencyStatus {
    val value: CryptoCurrencyStatus.Value = mockk {
        every { fiatAmount } returns fiatBalance
    }
    return CryptoCurrencyStatus(currency = token, value = value)
}

internal fun availableNetwork(
    token: CryptoCurrency.Token = paymentToken(),
    chainId: Long = POLYGON_CHAIN_ID,
    fiatBalance: BigDecimal? = BigDecimal("2325.55"),
    extraStatuses: List<CryptoCurrencyStatus> = emptyList(),
): PaymentNetworkStatus.Available = PaymentNetworkStatus.Available(
    network = token.network,
    depositAddress = "0xdeposit",
    chainId = chainId,
    cryptoCurrencyStatuses = listOf(currencyStatus(token, fiatBalance)) + extraStatuses,
)

internal fun offrampSource(
    networkName: String = "Polygon",
    chainId: Long = POLYGON_CHAIN_ID,
    contractAddress: String = USDC_POLYGON,
): OfframpSource = OfframpSource(
    networkName = networkName,
    isTestnet = false,
    chainId = chainId,
    tokens = listOf(OfframpToken(symbol = "USDC", contractAddress = contractAddress)),
)

internal fun offrampDestination(
    rail: BankRail = BankRail.ACH,
    fee: BigDecimal = BigDecimal("1.00"),
    feeName: String? = null,
    minDays: Int = 1,
    maxDays: Int = 3,
): OfframpDestination = OfframpDestination(
    rail = rail,
    fee = OfframpFee(name = feeName, amount = fee, currency = "USD", description = null),
    eta = OfframpEta(minDays = minDays, maxDays = maxDays),
)

internal fun offrampOptions(
    sources: List<OfframpSource> = listOf(offrampSource()),
    destinations: List<OfframpDestination> = listOf(
        offrampDestination(),
        offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0),
    ),
): BankOfframpOptions = BankOfframpOptions(sources = sources, destinations = destinations)

internal fun bankWithdrawSource(
    token: CryptoCurrency.Token = paymentToken(),
    chainId: Long = POLYGON_CHAIN_ID,
    fiatBalance: BigDecimal = BigDecimal("2325.55"),
    isAvailable: Boolean = true,
): BankWithdrawSource = BankWithdrawSource(
    network = token.network,
    chainId = chainId,
    token = token,
    fiatBalance = fiatBalance,
    isAvailable = isAvailable,
)

internal fun bankWithdrawOptions(
    sources: List<BankWithdrawSource> = listOf(bankWithdrawSource()),
    destinations: List<OfframpDestination> = listOf(
        offrampDestination(),
        offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0),
    ),
    minAmount: BigDecimal = BigDecimal("1.00"),
    maxAmount: BigDecimal = BigDecimal("10000.00"),
    currency: Currency = Currency.getInstance("USD"),
): BankWithdrawOptions = BankWithdrawOptions(
    sources = sources.toImmutableList(),
    destinations = destinations.toImmutableList(),
    minAmount = minAmount,
    maxAmount = maxAmount,
    currency = currency,
)