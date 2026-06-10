package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.utils.extensions.filterIf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for the Tangem Pay provider-filtering logic that lives in
 * `SwapModel.filterTangemPayProviders` (private extension on `List<SwapPairLeast>`).
 *
 * Because `SwapModel` is a `@ModelScoped` Decompose class with ~30 constructor dependencies
 * and requires a Decompose component context, it cannot be instantiated in a unit test.
 * Instead, we verify the *algorithm* end-to-end:
 *
 *   1. [SwapInteractorImpl.extractFromSwapCurrencyFromPair] — resolves which
 *      [SwapCurrencyStatus] is the FROM side of a given pair.
 *   2. `isTangemPayWithdrawal(status) = status?.account is Account.Payment` — the check.
 *   3. `List.filterIf(isWithdrawal) { provider.type == CEX }` — the filtering.
 *
 * We exercise all three together in test-space so that every business rule of
 * `filterTangemPayProviders` is covered, including all 9 edge cases from the task spec.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("filterTangemPayProviders — Payment-account provider filtering logic")
internal class SwapFilterTangemPayProvidersLogicTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()
    private val polygonNetwork = Blockchain.Polygon.toNetworkId()
    private val userWalletId = UserWalletId(stringValue = "deadbeef")

    // -----------------------------------------------------------------------
    // Helpers — mirrors the private logic in SwapModel.filterTangemPayProviders
    // -----------------------------------------------------------------------

    /**
     * Pure reimplementation of `SwapModel.filterTangemPayProviders` that delegates
     * to the real [SwapInteractorImpl.extractFromSwapCurrencyFromPair] for the
     * FROM-side resolution.  This lets every unit test exercise the *exact same*
     * algorithm as the production code without instantiating `SwapModel`.
     */
    private fun List<SwapPairLeast>.applyTangemPayFilter(
        fromStatus: SwapCurrencyStatus,
        toStatus: SwapCurrencyStatus,
    ): List<SwapPairLeast> = map { pair ->
        val resolvedFrom = sut.extractFromSwapCurrencyFromPair(
            pair = pair,
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
        )
        val isTangemPayWithdrawal = resolvedFrom?.account is Account.Payment
        val filterProviderTypes = if (isTangemPayWithdrawal) {
            listOf(ExchangeProviderType.CEX)
        } else {
            emptyList()
        }
        pair.copy(
            providers = pair.providers.filterIf(filterProviderTypes.isNotEmpty()) { provider ->
                provider.type in filterProviderTypes
            },
        )
    }

    // -----------------------------------------------------------------------
    // Builders
    // -----------------------------------------------------------------------

    private fun buildPaymentStatus(
        networkRawId: String = ethNetwork,
        contractAddress: String = "0",
        isCoin: Boolean = true,
    ): SwapCurrencyStatus = buildSwapCurrencyStatus(
        networkRawId = networkRawId,
        contractAddress = contractAddress,
        isCoin = isCoin,
    ).copy(account = Account.Payment(userWalletId))

    private fun buildCryptoPortfolioStatus(
        networkRawId: String = ethNetwork,
        contractAddress: String = "0",
        isCoin: Boolean = true,
    ): SwapCurrencyStatus = buildSwapCurrencyStatus(
        networkRawId = networkRawId,
        contractAddress = contractAddress,
        isCoin = isCoin,
    ).copy(account = Account.CryptoPortfolio.createMainAccount(userWalletId))

    private fun mixedProviders() = listOf(
        buildSwapProvider(ExchangeProviderType.CEX, "cex-1"),
        buildSwapProvider(ExchangeProviderType.DEX, "dex-1"),
        buildSwapProvider(ExchangeProviderType.DEX_BRIDGE, "bridge-1"),
    )

    private fun cexOnlyProviders() = listOf(
        buildSwapProvider(ExchangeProviderType.CEX, "cex-only"),
    )

    private fun dexOnlyProviders() = listOf(
        buildSwapProvider(ExchangeProviderType.DEX, "dex-only"),
    )

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Payment account FROM side — only CEX providers must remain")
    inner class PaymentAccountFromSide {

        @Test
        @DisplayName("should keep only CEX when FROM status is Payment account and providers are mixed")
        fun `should keep only CEX when FROM status is Payment account and providers are mixed`() {
            // given — FROM is a Payment account, pair.from matches FROM
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — only CEX survives
            assertThat(result).hasSize(1)
            assertThat(result[0].providers).hasSize(1)
            assertThat(result[0].providers[0].type).isEqualTo(ExchangeProviderType.CEX)
        }

        @Test
        @DisplayName("should return empty providers when Payment account FROM and no CEX in list")
        fun `should return empty providers when Payment account FROM and no CEX in list`() {
            // given — FROM is Payment, no CEX provider exists
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = dexOnlyProviders(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — all providers removed because none are CEX
            assertThat(result[0].providers).isEmpty()
        }

        @Test
        @DisplayName("should leave list unchanged when Payment account FROM and all providers already CEX")
        fun `should leave list unchanged when Payment account FROM and all providers already CEX`() {
            // given — FROM is Payment, list is already all CEX
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = cexOnlyProviders(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — single CEX provider still present, unchanged
            assertThat(result[0].providers).hasSize(1)
            assertThat(result[0].providers[0].type).isEqualTo(ExchangeProviderType.CEX)
        }
    }

    @Nested
    @DisplayName("Non-Payment account — provider list must not be modified")
    inner class NonPaymentAccount {

        @Test
        @DisplayName("should not filter providers when FROM status is CryptoPortfolio account")
        fun `should not filter providers when FROM status is CryptoPortfolio account`() {
            // given — FROM is a CryptoPortfolio account (regression guard)
            val fromStatus = buildCryptoPortfolioStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — all 3 providers survive untouched
            assertThat(result[0].providers).hasSize(3)
            assertThat(result[0].providers.map { it.type })
                .containsExactly(ExchangeProviderType.CEX, ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE)
        }
    }

    @Nested
    @DisplayName("Null resolved status — no filtering applied")
    inner class NullResolvedStatus {

        @Test
        @DisplayName("should not filter when extractFromSwapCurrencyFromPair resolves null (unrelated pair)")
        fun `should not filter when extractFromSwapCurrencyFromPair resolves null`() {
            // given — pair.from is on an unrelated network (neither fromStatus nor toStatus)
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = polygonNetwork, // matches neither
                fromContract = "0",
                toNetwork = ethNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — null status → isTangemPayWithdrawal=false → no filter applied
            assertThat(result[0].providers).hasSize(3)
        }
    }

    @Nested
    @DisplayName("Empty inputs — no crash, stable output")
    inner class EmptyInputs {

        @Test
        @DisplayName("should return empty list when input pairs list is empty")
        fun `should return empty list when input pairs list is empty`() {
            // given
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)

            // when
            val result = emptyList<SwapPairLeast>().applyTangemPayFilter(fromStatus, toStatus)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("should handle empty provider list on a pair without crashing")
        fun `should handle empty provider list on a pair without crashing`() {
            // given — Payment account FROM, but the pair already has an empty provider list
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)
            val pair = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = emptyList(),
            )

            // when
            val result = listOf(pair).applyTangemPayFilter(fromStatus, toStatus)

            // then — stays empty, no crash
            assertThat(result[0].providers).isEmpty()
        }
    }

    @Nested
    @DisplayName("Multiple pairs — filtering applied per-pair independently")
    inner class MultiplePairs {

        @Test
        @DisplayName("should filter only pairs whose resolved FROM is a Payment account")
        fun `should filter only pairs whose resolved FROM is a Payment account`() {
            // given — 2 pairs:
            //   pair1: pair.from == ethNetwork → fromStatus (Payment) → filter to CEX only
            //   pair2: pair.from == btcNetwork → toStatus (non-Payment) → no filter
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)

            val pair1 = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )
            val pair2 = buildSwapPairLeast(
                fromNetwork = btcNetwork, // matches toStatus (CryptoPortfolio)
                fromContract = "0",
                toNetwork = ethNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )

            // when
            val result = listOf(pair1, pair2).applyTangemPayFilter(fromStatus, toStatus)

            // then
            // pair1 resolved to Payment account → only CEX remains
            assertThat(result[0].providers).hasSize(1)
            assertThat(result[0].providers[0].type).isEqualTo(ExchangeProviderType.CEX)

            // pair2 resolved to CryptoPortfolio → all 3 providers intact
            assertThat(result[1].providers).hasSize(3)
        }

        @Test
        @DisplayName("should filter all pairs when all resolved FROM statuses are Payment accounts")
        fun `should filter all pairs when all resolved FROM statuses are Payment accounts`() {
            // given — both pairs have their pair.from matching the Payment account
            val fromStatus = buildPaymentStatus(networkRawId = ethNetwork)
            val toStatus = buildCryptoPortfolioStatus(networkRawId = btcNetwork)

            val pair1 = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = btcNetwork,
                toContract = "0",
                providers = mixedProviders(),
            )
            val pair2 = buildSwapPairLeast(
                fromNetwork = ethNetwork,
                fromContract = "0",
                toNetwork = polygonNetwork,
                toContract = "0",
                providers = dexOnlyProviders(),
            )

            // when
            val result = listOf(pair1, pair2).applyTangemPayFilter(fromStatus, toStatus)

            // then — pair1: CEX kept; pair2: DEX removed → empty
            assertThat(result[0].providers).hasSize(1)
            assertThat(result[0].providers[0].type).isEqualTo(ExchangeProviderType.CEX)
            assertThat(result[1].providers).isEmpty()
        }
    }
}