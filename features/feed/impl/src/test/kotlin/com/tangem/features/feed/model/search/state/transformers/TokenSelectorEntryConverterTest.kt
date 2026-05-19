package com.tangem.features.feed.model.search.state.transformers

import com.google.common.truth.Truth
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.FiatCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.common.ui.markets.tokenselector.BalanceDisplayState
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenSelectorEntryConverterTest {

    private val appCurrency: AppCurrency = AppCurrency.Default
    private val onTokenSelected: (UserAssetEntry) -> Unit = mockk(relaxed = true)

    private lateinit var converter: TokenSelectorEntryConverter

    @BeforeEach
    fun setup() {
        clearMocks(onTokenSelected)
        converter = TokenSelectorEntryConverter(
            appCurrency = appCurrency,
            isBalanceHidden = false,
            onTokenSelected = onTokenSelected,
        )
    }

    @Test
    fun `should convert entry with Loaded status to Single with Loaded balance`() {
        val entry = createMockEntry(
            walletId = "wallet1",
            accountId = "account1",
            currencyId = "btc",
            currencyName = "Bitcoin",
            currencySymbol = "BTC",
            networkName = "Bitcoin",
            decimals = 8,
            value = createLoadedValue(
                amount = BigDecimal("1.5"),
                fiatAmount = BigDecimal("45000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("2.5"),
            ),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.id).isEqualTo("wallet1_account1_btc")
        Truth.assertThat(result.tokenName).isEqualTo("Bitcoin")
        Truth.assertThat(result.tokenSymbol).isEqualTo("BTC")
        Truth.assertThat(result.networkName).isEqualTo("Bitcoin")
        Truth.assertThat(result.isBalanceHidden).isFalse()
        Truth.assertThat(result.balanceState).isInstanceOf(BalanceDisplayState.Loaded::class.java)
    }

    @Test
    fun `should set balance hidden when isBalanceHidden is true`() {
        converter = TokenSelectorEntryConverter(
            appCurrency = appCurrency,
            isBalanceHidden = true,
            onTokenSelected = onTokenSelected,
        )

        val entry = createMockEntry(
            value = createLoadedValue(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("1.0"),
            ),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.isBalanceHidden).isTrue()
    }

    @Test
    fun `should return Loading balance state for Loading value without amount`() {
        val entry = createMockEntry(
            value = CryptoCurrencyStatus.Loading,
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.balanceState).isEqualTo(BalanceDisplayState.Loading)
    }

    @Test
    fun `should return Unreachable balance state for Unreachable value without amount`() {
        val entry = createMockEntry(
            value = createUnreachableValue(),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.balanceState).isEqualTo(BalanceDisplayState.Unreachable)
    }

    @Test
    fun `should return Unknown price change state for Loading value`() {
        val entry = createMockEntry(
            value = CryptoCurrencyStatus.Loading,
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isEqualTo(PriceChangeState.Unknown)
    }

    @Test
    fun `should return Unknown price change state for Unreachable value`() {
        val entry = createMockEntry(
            value = createUnreachableValue(),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isEqualTo(PriceChangeState.Unknown)
    }

    @Test
    fun `should return Unknown price change state for MissedDerivation value`() {
        val entry = createMockEntry(
            value = createMissedDerivationValue(),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isEqualTo(PriceChangeState.Unknown)
    }

    @Test
    fun `should return Unknown price change state for NoAmount value`() {
        val entry = createMockEntry(
            value = createNoAmountValue(),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isEqualTo(PriceChangeState.Unknown)
    }

    @Test
    fun `should return Content price change state with UP type for positive change`() {
        val entry = createMockEntry(
            value = createLoadedValue(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("5.0"),
            ),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isInstanceOf(PriceChangeState.Content::class.java)
        val content = result.priceChangeState as PriceChangeState.Content
        Truth.assertThat(content.type).isEqualTo(PriceChangeType.UP)
    }

    @Test
    fun `should return Content price change state with DOWN type for negative change`() {
        val entry = createMockEntry(
            value = createLoadedValue(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("-3.0"),
            ),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.priceChangeState).isInstanceOf(PriceChangeState.Content::class.java)
        val content = result.priceChangeState as PriceChangeState.Content
        Truth.assertThat(content.type).isEqualTo(PriceChangeType.DOWN)
    }

    @Test
    fun `should invoke onTokenSelected callback when onClick is called`() {
        val entry = createMockEntry(
            value = createLoadedValue(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("1.0"),
            ),
        )

        val result = converter.convert(entry)
        result.onClick()

        verify(exactly = 1) { onTokenSelected(entry) }
    }

    @Test
    fun `should generate correct composite id from wallet account and currency`() {
        val entry = createMockEntry(
            walletId = "myWallet",
            accountId = "myAccount",
            currencyId = "eth",
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.id).isEqualTo("myWallet_myAccount_eth")
    }

    @Test
    fun `should convert list of entries`() {
        val entries = listOf(
            createMockEntry(currencyId = "btc", currencyName = "Bitcoin", currencySymbol = "BTC"),
            createMockEntry(currencyId = "eth", currencyName = "Ethereum", currencySymbol = "ETH"),
        )

        val results = converter.convertList(entries)

        Truth.assertThat(results).hasSize(2)
        Truth.assertThat(results[0].tokenName).isEqualTo("Bitcoin")
        Truth.assertThat(results[1].tokenName).isEqualTo("Ethereum")
    }

    @Test
    fun `should return fiatRate formatted string for Loaded value`() {
        val entry = createMockEntry(
            value = createLoadedValue(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("1.0"),
            ),
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.fiatRate).isNotNull()
    }

    @Test
    fun `should return null fiatRate when value has no fiatRate`() {
        val entry = createMockEntry(
            value = CryptoCurrencyStatus.Loading,
        )

        val result = converter.convert(entry)

        Truth.assertThat(result.fiatRate).isNull()
    }

    // region Helpers

    private fun createMockEntry(
        walletId: String = "wallet1",
        accountId: String = "account1",
        currencyId: String = "btc",
        currencyName: String = "Bitcoin",
        currencySymbol: String = "BTC",
        networkName: String = "Bitcoin",
        decimals: Int = 8,
        value: CryptoCurrencyStatus.Value = createLoadedValue(
            amount = BigDecimal("1.0"),
            fiatAmount = BigDecimal("30000.0"),
            fiatRate = BigDecimal("30000.0"),
            priceChange = BigDecimal("1.0"),
        ),
    ): UserAssetEntry {
        val userWalletId = mockk<UserWalletId> {
            every { stringValue } returns walletId
        }
        val accountIdMock = mockk<com.tangem.domain.models.account.AccountId> {
            every { this@mockk.value } returns accountId
        }
        val network = mockk<Network>(relaxed = true) {
            every { name } returns networkName
        }
        val currencyIdObj = mockk<CryptoCurrency.ID> {
            every { this@mockk.value } returns currencyId
        }
        val currency = mockk<CryptoCurrency.Coin> {
            every { id } returns currencyIdObj
            every { name } returns currencyName
            every { symbol } returns currencySymbol
            every { this@mockk.network } returns network
            every { this@mockk.decimals } returns decimals
            every { iconUrl } returns null
            every { isCustom } returns false
        }
        val currencyStatus = mockk<CryptoCurrencyStatus> {
            every { this@mockk.currency } returns currency
            every { this@mockk.value } returns value
        }
        return mockk<UserAssetEntry> {
            every { this@mockk.userWalletId } returns userWalletId
            every { this@mockk.userWalletName } returns "Wallet"
            every { this@mockk.accountId } returns accountIdMock
            every { this@mockk.accountName } returns mockk(relaxed = true)
            every { this@mockk.accountIcon } returns mockk(relaxed = true)
            every { this@mockk.currencyStatus } returns currencyStatus
        }
    }

    private fun createLoadedValue(
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        fiatRate: BigDecimal,
        priceChange: BigDecimal,
    ): CryptoCurrencyStatus.Loaded {
        return CryptoCurrencyStatus.Loaded(
            fiatCurrency = FiatCurrency.Default,
            amount = amount,
            fiatAmount = fiatAmount,
            fiatRate = fiatRate,
            priceChange = priceChange,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
        )
    }

    private fun createUnreachableValue(): CryptoCurrencyStatus.Unreachable {
        return CryptoCurrencyStatus.Unreachable(
            fiatCurrency = FiatCurrency.Default,
            priceChange = null,
            fiatRate = null,
            networkAddress = null,
        )
    }

    private fun createMissedDerivationValue(): CryptoCurrencyStatus.MissedDerivation {
        return CryptoCurrencyStatus.MissedDerivation(
            fiatCurrency = FiatCurrency.Default,
            priceChange = null,
            fiatRate = null,
        )
    }

    private fun createNoAmountValue(): CryptoCurrencyStatus.NoAmount {
        return CryptoCurrencyStatus.NoAmount(
            fiatCurrency = FiatCurrency.Default,
            priceChange = null,
            fiatRate = null,
        )
    }

    // endregion
}