package com.tangem.domain.swap.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapPairModel
import com.tangem.domain.swap.models.SwapTxType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSwapSupportedPairsUseCaseTest {

    private val swapRepositoryV2: SwapRepositoryV2 = mockk()
    private val swapErrorResolver: SwapErrorResolver = mockk()

    private val useCase = GetSwapSupportedPairsUseCase(
        swapRepositoryV2 = swapRepositoryV2,
        swapErrorResolver = swapErrorResolver,
    )

    private val initialCurrency = createCurrency("initial")
    private val cexCurrency = createCurrency("cex")
    private val dexOnlyCurrency = createCurrency("dex-only")
    private val unavailableCurrency = createCurrency("unavailable")

    @BeforeEach
    fun setup() {
        clearMocks(swapRepositoryV2)
    }

    @Test
    fun `GIVEN cex, dex-only and missing currencies WHEN invoke THEN split into three buckets`() = runTest {
        // Arrange
        val pairs = listOf(
            createPair(from = initialCurrency, to = cexCurrency, providers = listOf(cexProvider)),
            createPair(from = initialCurrency, to = dexOnlyCurrency, providers = listOf(dexProvider)),
            // unavailableCurrency intentionally has no pair at all
        )
        coEvery {
            swapRepositoryV2.getSupportedPairs(any(), any(), any(), any(), any())
        } returns pairs

        // Act
        val result = useCase.invoke(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = listOf(initialCurrency, cexCurrency, dexOnlyCurrency, unavailableCurrency),
            filterProviderTypes = listOf(ExpressProviderType.CEX),
            swapTxType = SwapTxType.SendWithSwap,
        )

        // Assert
        val fromGroup = result.getOrNull()!!.fromGroup
        assertThat(fromGroup.available.map { it.currencyStatus.currency }).containsExactly(cexCurrency)
        assertThat(fromGroup.availableForSwap.map { it.currencyStatus.currency }).containsExactly(dexOnlyCurrency)
        assertThat(fromGroup.unavailable.map { it.currencyStatus.currency }).containsExactly(unavailableCurrency)
    }

    @Test
    fun `GIVEN dex pair AND no type restriction WHEN invoke THEN currency is available not availableForSwap`() =
        runTest {
            // Arrange — allowedProviderTypes empty means any provider type is eligible for the current flow
            val pairs = listOf(
                createPair(from = initialCurrency, to = dexOnlyCurrency, providers = listOf(dexProvider)),
            )
            coEvery {
                swapRepositoryV2.getSupportedPairs(any(), any(), any(), any(), any())
            } returns pairs

            // Act
            val result = useCase.invoke(
                userWallet = userWallet,
                initialCurrency = initialCurrency,
                cryptoCurrencyList = listOf(initialCurrency, dexOnlyCurrency),
                filterProviderTypes = emptyList(),
                swapTxType = SwapTxType.SendWithSwap,
            )

            // Assert
            val fromGroup = result.getOrNull()!!.fromGroup
            assertThat(fromGroup.available.map { it.currencyStatus.currency }).containsExactly(dexOnlyCurrency)
            assertThat(fromGroup.availableForSwap).isEmpty()
        }

    @Test
    fun `GIVEN memo network with provider without extra-id WHEN invoke THEN currency is availableForSwap`() = runTest {
        // Arrange — to-network requires extra id, but the only CEX provider doesn't support it
        val memoCurrency = createCurrency(rawId = "memo", txExtras = Network.TransactionExtrasType.MEMO)
        val pairs = listOf(
            createPair(from = initialCurrency, to = memoCurrency, providers = listOf(cexProviderNoExtraId)),
        )
        coEvery {
            swapRepositoryV2.getSupportedPairs(any(), any(), any(), any(), any())
        } returns pairs

        // Act
        val result = useCase.invoke(
            userWallet = userWallet,
            initialCurrency = initialCurrency,
            cryptoCurrencyList = listOf(initialCurrency, memoCurrency),
            filterProviderTypes = listOf(ExpressProviderType.CEX),
            swapTxType = SwapTxType.SendWithSwap,
        )

        // Assert
        val fromGroup = result.getOrNull()!!.fromGroup
        assertThat(fromGroup.available).isEmpty()
        assertThat(fromGroup.availableForSwap.map { it.currencyStatus.currency }).containsExactly(memoCurrency)
    }

    @Test
    fun `GIVEN pair with both cex and dex providers WHEN invoke THEN currency is available not availableForSwap`() =
        runTest {
            // Arrange
            val pairs = listOf(
                createPair(from = initialCurrency, to = cexCurrency, providers = listOf(cexProvider, dexProvider)),
            )
            coEvery {
                swapRepositoryV2.getSupportedPairs(any(), any(), any(), any(), any())
            } returns pairs

            // Act
            val result = useCase.invoke(
                userWallet = userWallet,
                initialCurrency = initialCurrency,
                cryptoCurrencyList = listOf(initialCurrency, cexCurrency),
                filterProviderTypes = listOf(ExpressProviderType.CEX),
                swapTxType = SwapTxType.SendWithSwap,
            )

            // Assert
            val fromGroup = result.getOrNull()!!.fromGroup
            assertThat(fromGroup.available.map { it.currencyStatus.currency }).containsExactly(cexCurrency)
            assertThat(fromGroup.availableForSwap).isEmpty()
        }

    private fun createPair(from: CryptoCurrency, to: CryptoCurrency, providers: List<ExpressProvider>) = SwapPairModel(
        from = CryptoCurrencyStatus(currency = from, value = mockk(relaxed = true)),
        to = CryptoCurrencyStatus(currency = to, value = mockk(relaxed = true)),
        providers = providers,
    )

    private companion object {
        val userWallet: UserWallet = mockk(relaxed = true)

        val cexProvider = createProvider("cex-1", ExpressProviderType.CEX, isExtraIdSupported = true)
        val cexProviderNoExtraId = createProvider("cex-2", ExpressProviderType.CEX, isExtraIdSupported = false)
        val dexProvider = createProvider("dex-1", ExpressProviderType.DEX, isExtraIdSupported = false)

        fun createProvider(id: String, type: ExpressProviderType, isExtraIdSupported: Boolean) = ExpressProvider(
            providerId = id,
            rateTypes = listOf(ExpressRateType.Float),
            name = id,
            type = type,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
            isExtraIdSupported = isExtraIdSupported,
        )

        fun createCurrency(
            rawId: String,
            txExtras: Network.TransactionExtrasType = Network.TransactionExtrasType.NONE,
        ): CryptoCurrency {
            val id: CryptoCurrency.ID = mockk {
                every { rawCurrencyId } returns CryptoCurrency.RawID(rawId)
                every { rawNetworkId } returns rawId
            }
            return mockk<CryptoCurrency>(relaxed = true).also { currency ->
                every { currency.id } returns id
                every { currency.network.transactionExtrasType } returns txExtras
            }
        }
    }
}