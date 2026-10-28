package com.tangem.feature.swap.mcp.holdings

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.feature.swap.mcp.createCoin
import com.tangem.feature.swap.mcp.createExchangeIntent
import com.tangem.feature.swap.mcp.createNetwork
import com.tangem.feature.swap.mcp.createToken
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentHoldingsResolverTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val resolver = ExchangeIntentHoldingsResolver(singleAccountStatusListSupplier)

    private val walletId = UserWalletId("011")
    private val intent = createExchangeIntent()

    private val usdt = createToken()
    private val polygonNetwork = createNetwork(rawId = "polygon-pos", name = "Polygon")
    private val pol = createCoin(network = polygonNetwork, symbol = "POL")

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountStatusListSupplier)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN held currencies WHEN resolve THEN the holdings tell what is missing`(model: HoldingsModel) = runTest {
        // Arrange
        stubAccounts(listOf(personal(statuses = model.held.map { status(it) })))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class HoldingsModel(val held: List<CryptoCurrency>, val expected: ExchangeIntentHoldings)

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        HoldingsModel(held = listOf(usdt, pol), expected = ExchangeIntentHoldings.Both(from = usdt, to = pol)),
        HoldingsModel(held = listOf(usdt), expected = ExchangeIntentHoldings.MissingTo(from = usdt)),
        HoldingsModel(held = listOf(pol), expected = ExchangeIntentHoldings.MissingFrom(to = pol)),
        HoldingsModel(held = emptyList(), expected = ExchangeIntentHoldings.MissingBoth),
    )

    @Test
    fun `GIVEN accounts cannot be read WHEN resolve THEN nothing is resolved`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(walletId) } returns emptyFlow()

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN accounts never emit WHEN resolve THEN nothing is resolved after the wait`() = runTest {
        // Arrange
        every { singleAccountStatusListSupplier(walletId) } returns flow { awaitCancellation() }

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN balances settle only after the wait WHEN resolve THEN the latest emission seen is used`() = runTest {
        // Arrange
        val loadingUsdt = createToken()
        val loading = accountStatusList(listOf(personal(statuses = listOf(loadingStatus(loadingUsdt)))))
        every { singleAccountStatusListSupplier(walletId) } returns flow {
            emit(loading)
            awaitCancellation()
        }

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(ExchangeIntentHoldings.MissingTo(from = loadingUsdt))
    }

    @Test
    fun `GIVEN only an unrelated token still loading WHEN resolve THEN it is not waited for`() =
        runTest {
            // Arrange
            val otherNetwork = createNetwork(rawId = "bsc", name = "BNB Smart Chain")
            val unrelated = createCoin(network = otherNetwork, symbol = "BNB")
            val settled = accountStatusList(
                listOf(personal(statuses = listOf(status(usdt, BigDecimal("5")), status(pol), loadingStatus(unrelated)))),
            )
            every { singleAccountStatusListSupplier(walletId) } returns flow {
                emit(settled)
                awaitCancellation()
            }

            // Act
            val actual = withTimeoutOrNull(timeMillis = 1) { resolver.resolve(walletId, intent) }

            // Assert
            assertThat(actual).isEqualTo(ExchangeIntentHoldings.Both(from = usdt, to = pol))
        }

    @Test
    fun `GIVEN contract address in another case WHEN resolve THEN the token is held`() = runTest {
        // Arrange
        stubAccounts(listOf(personal(statuses = listOf(status(usdt), status(pol)))))
        val shouting = intent.copy(from = intent.from.copy(contractAddress = intent.from.contractAddress.uppercase()))

        // Act
        val actual = resolver.resolve(walletId, shouting)

        // Assert
        assertThat(actual).isEqualTo(ExchangeIntentHoldings.Both(from = usdt, to = pol))
    }

    @Test
    fun `GIVEN contract address in another case on a case-sensitive network WHEN resolve THEN the token is not held`() =
        runTest {
            // Arrange
            val solana = createNetwork(rawId = "solana", name = "Solana")
            val mint = createToken(network = solana, contractAddress = "So11111111111111111111111111111111111111112")
            val solanaIntent = createExchangeIntent(
                from = intent.from.copy(network = "solana", contractAddress = mint.contractAddress.lowercase()),
            )
            stubAccounts(listOf(personal(statuses = listOf(status(mint), status(pol)))))

            // Act
            val actual = resolver.resolve(walletId, solanaIntent)

            // Assert
            assertThat(actual).isEqualTo(ExchangeIntentHoldings.MissingFrom(to = pol))
        }

    @Test
    fun `GIVEN balances still loading in the first emission WHEN resolve THEN the funded account of the next one wins`() =
        runTest {
            // Arrange
            val firstAccountId = mockk<AccountId>()
            val secondAccountId = mockk<AccountId>()
            val usdtOfFirst = createToken()
            val usdtOfSecond = createToken()
            stubAccounts(
                listOf(
                    personal(id = firstAccountId, statuses = listOf(loadingStatus(usdtOfFirst))),
                    personal(id = secondAccountId, statuses = listOf(loadingStatus(usdtOfSecond))),
                ),
                listOf(
                    personal(id = firstAccountId, statuses = listOf(status(usdtOfFirst, fiat = BigDecimal("1")))),
                    personal(id = secondAccountId, statuses = listOf(status(usdtOfSecond, fiat = BigDecimal("700")))),
                ),
            )

            // Act
            val actual = resolver.resolve(walletId, intent)

            // Assert
            assertThat((actual as ExchangeIntentHoldings.MissingTo).from).isSameInstanceAs(usdtOfSecond)
        }

    @Test
    fun `GIVEN same token on another network WHEN resolve THEN it is not held`() = runTest {
        // Arrange
        val usdtOnPolygon = createToken(network = polygonNetwork, contractAddress = usdt.contractAddress)
        stubAccounts(listOf(personal(statuses = listOf(status(usdtOnPolygon), status(pol)))))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(ExchangeIntentHoldings.MissingFrom(to = pol))
    }

    @Test
    fun `GIVEN token held in two accounts WHEN resolve THEN the most funded one is taken to swap from`() = runTest {
        // Arrange
        val poorUsdt = createToken()
        val richUsdt = createToken()
        stubAccounts(
            listOf(
                personal(statuses = listOf(status(poorUsdt, fiat = BigDecimal("5")))),
                personal(statuses = listOf(status(richUsdt, fiat = BigDecimal("500")))),
            ),
        )

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat((actual as ExchangeIntentHoldings.MissingTo).from).isSameInstanceAs(richUsdt)
    }

    @Test
    fun `GIVEN token to swap to held in two accounts WHEN resolve THEN the account of the other token is preferred`() =
        runTest {
            // Arrange
            val usdtOfMain = createToken()
            val polOfMain = createCoin(network = polygonNetwork, symbol = "POL")
            val polOfOther = createCoin(network = polygonNetwork, symbol = "POL")
            val mainId = mockk<AccountId>()
            val otherId = mockk<AccountId>()
            stubAccounts(
                listOf(
                    personal(id = mainId, statuses = listOf(status(usdtOfMain, BigDecimal("10")), status(polOfMain))),
                    personal(id = otherId, statuses = listOf(status(polOfOther, fiat = BigDecimal("900")))),
                ),
            )

            // Act
            val actual = resolver.resolve(walletId, intent)

            // Assert
            assertThat((actual as ExchangeIntentHoldings.Both).to).isSameInstanceAs(polOfMain)
        }

    @Test
    fun `GIVEN token held only in a joint account WHEN resolve THEN it is not held`() = runTest {
        // Arrange
        val joint = mockk<AccountStatus.Joint>()
        stubAccounts(listOf(joint, personal(statuses = listOf(status(pol)))))

        // Act
        val actual = resolver.resolve(walletId, intent)

        // Assert
        assertThat(actual).isEqualTo(ExchangeIntentHoldings.MissingFrom(to = pol))
    }

    @Test
    fun `GIVEN native coin on the intent WHEN resolve THEN the coin of that network is held`() = runTest {
        // Arrange
        val coinIntent = createExchangeIntent(from = intent.to, to = intent.from)
        stubAccounts(listOf(personal(statuses = listOf(status(pol)))))

        // Act
        val actual = resolver.resolve(walletId, coinIntent)

        // Assert
        assertThat(actual).isEqualTo(ExchangeIntentHoldings.MissingTo(from = pol))
    }

    private fun stubAccounts(vararg emissions: List<AccountStatus>) {
        val lists = emissions.map { accountStatusList(it) }
        every { singleAccountStatusListSupplier(walletId) } returns lists.asFlow()
    }

    private fun accountStatusList(accountStatuses: List<AccountStatus>): AccountStatusList =
        mockk { every { this@mockk.accountStatuses } returns accountStatuses }

    private fun personal(
        id: AccountId = mockk(),
        statuses: List<CryptoCurrencyStatus>,
    ): AccountStatus.Personal = mockk {
        every { accountId } returns id
        every { flattenCurrencies() } returns statuses
    }

    private fun status(currency: CryptoCurrency, fiat: BigDecimal? = null): CryptoCurrencyStatus = mockk {
        every { this@mockk.currency } returns currency
        every { value } returns mockk { every { fiatAmount } returns fiat }
    }

    private fun loadingStatus(currency: CryptoCurrency): CryptoCurrencyStatus = mockk {
        every { this@mockk.currency } returns currency
        every { value } returns CryptoCurrencyStatus.Loading
    }
}