package com.tangem.domain.models.account

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PaymentAccountStatusValueMultichainTest {

    private val polygon = createNetwork(id = "polygon")
    private val ethereum = createNetwork(id = "ethereum")
    private val primaryCurrency = createToken(network = polygon)

    private fun balance() = PaymentAccountStatusValue.Balance(
        fiatBalance = PaymentAccountStatusValue.FiatBalance(BigDecimal("100"), "USD"),
    )

    private fun loaded(
        networks: List<PaymentNetworkStatus>,
        balance: PaymentAccountStatusValue.Balance? = balance(),
    ) = PaymentAccountStatusValue.Loaded(
        source = StatusSource.ACTUAL,
        customerId = "c1",
        paymentAccountAddress = "0xDEPOSIT",
        balance = balance,
        cryptoCurrency = primaryCurrency,
        networks = networks,
        cards = emptyList(),
        fiatRate = BigDecimal("1.0"),
        error = null,
        virtualAccount = null,
        tariffPlan = null,
    )

    @Test
    fun `GIVEN no networks WHEN read statuses THEN empty and no single status`() {
        // Arrange
        val loaded = loaded(networks = emptyList())

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatuses).isEmpty()
        assertThat(loaded.cryptoCurrencyStatus).isNull()
        assertThat(loaded.availableForWithdrawal).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN only non-Available networks WHEN read statuses THEN empty`() {
        // Arrange
        val loaded = loaded(
            networks = listOf(
                PaymentNetworkStatus.NotIssued(network = ethereum, chainId = 1L),
                PaymentNetworkStatus.Disabled(network = ethereum, cryptoCurrencies = listOf(mockk()), chainId = 1L),
            ),
        )

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatuses).isEmpty()
        assertThat(loaded.cryptoCurrencyStatus).isNull()
    }

    @Test
    fun `GIVEN Available networks WHEN read statuses THEN flattens their statuses in order`() {
        // Arrange
        val s1 = createStatus(network = ethereum, amount = BigDecimal("1"))
        val s2 = createStatus(network = ethereum, amount = BigDecimal("2"))
        val s3 = createStatus(network = polygon, amount = BigDecimal("3"))
        val loaded = loaded(
            networks = listOf(
                available(ethereum, listOf(s1, s2)),
                PaymentNetworkStatus.NotIssued(network = createNetwork(id = "tron"), chainId = 1L),
                available(polygon, listOf(s3)),
            ),
        )

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(s1, s2, s3).inOrder()
    }

    @Test
    fun `GIVEN account currency issued WHEN read single status THEN its own network status is taken`() {
        // Arrange
        val onEthereum = createStatus(network = ethereum, amount = BigDecimal("1"))
        val onPolygon = createStatus(network = polygon, amount = BigDecimal("2"))
        val loaded = loaded(
            networks = listOf(available(ethereum, listOf(onEthereum)), available(polygon, listOf(onPolygon))),
        )

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatus).isEqualTo(onPolygon)
    }

    @Test
    fun `GIVEN account currency not issued WHEN read single status THEN first issued one is taken`() {
        // Arrange
        val onEthereum = createStatus(network = ethereum, amount = BigDecimal("1"))
        val loaded = loaded(networks = listOf(available(ethereum, listOf(onEthereum))))

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatus).isEqualTo(onEthereum)
    }

    @Test
    fun `GIVEN same funds reachable on two networks WHEN read withdrawable THEN the largest amount is taken`() {
        // Arrange
        val loaded = loaded(
            networks = listOf(
                available(
                    network = ethereum,
                    statuses = listOf(
                        createStatus(network = ethereum, amount = BigDecimal("30")),
                        createStatus(network = ethereum, amount = BigDecimal.ZERO),
                    ),
                ),
                available(polygon, listOf(createStatus(network = polygon, amount = BigDecimal("30")))),
            ),
        )

        // Act & Assert
        assertThat(loaded.availableForWithdrawal).isEqualTo(BigDecimal("30"))
    }

    @Test
    fun `GIVEN no balance and no networks WHEN read statuses THEN empty and total balance failed`() {
        // Arrange
        val loaded = loaded(networks = emptyList(), balance = null)

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatus).isNull()
        assertThat(loaded.cryptoCurrencyStatuses).isEmpty()
        assertThat(loaded.totalFiatBalance).isEqualTo(TotalFiatBalance.Failed)
    }

    @Test
    fun `GIVEN no balance and Available networks WHEN read statuses THEN network statuses are used`() {
        // Arrange
        val networkStatus = createStatus(network = ethereum, amount = BigDecimal("5"))
        val loaded = loaded(networks = listOf(available(ethereum, listOf(networkStatus))), balance = null)

        // Act & Assert
        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(networkStatus)
    }

    private fun available(network: Network, statuses: List<CryptoCurrencyStatus>) = PaymentNetworkStatus.Available(
        network = network,
        depositAddress = "0xDEPOSIT",
        chainId = 137L,
        cryptoCurrencyStatuses = statuses,
    )

    private fun createStatus(network: Network, amount: BigDecimal): CryptoCurrencyStatus {
        val value: CryptoCurrencyStatus.Value = mockk()
        every { value.amount } returns amount
        return CryptoCurrencyStatus(currency = createToken(network = network), value = value)
    }

    private fun createToken(network: Network) = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = network.id.rawId.value),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin"),
        ),
        network = network,
        name = "USD Coin",
        symbol = "USDC",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xCONTRACT",
    )

    private fun createNetwork(id: String) = Network(
        id = Network.ID(value = id, derivationPath = Network.DerivationPath.None),
        name = id,
        currencySymbol = id.uppercase(),
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}