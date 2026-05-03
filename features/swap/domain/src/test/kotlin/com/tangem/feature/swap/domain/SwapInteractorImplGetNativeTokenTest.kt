package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [SwapInteractorImpl.getNativeToken].
 *
 * Behavior:
 *  - Look up cached portfolio coins for the user wallet via [MultiWalletCryptoCurrenciesSupplier].
 *  - Return the coin matching the target network (by `id` and `derivationPath`).
 *  - If supplier returns null or no match → fall back to [CurrenciesRepository.createCoinCurrency].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplGetNativeTokenTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @Test
    fun `should return a Coin from the supplier whose network matches the target`() = runTest {
        // Given — a single matching coin in the supplier
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val targetNetwork = fromStatus.currency.network

        val matchingCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { network } returns targetNetwork
        }

        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(matchingCoin)

        // When
        val result = sut.getNativeToken(fromStatus)

        // Then
        assertThat(result).isSameInstanceAs(matchingCoin)
    }

    @Test
    fun `should fall back to createCoinCurrency when supplier returns null`() = runTest {
        // Given
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val createdCoin = buildCoinCurrency(networkRawId = ethNetwork)

        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns null
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns createdCoin

        // When
        val result = sut.getNativeToken(fromStatus)

        // Then
        assertThat(result).isSameInstanceAs(createdCoin)
        coVerify(exactly = 1) { currenciesRepository.createCoinCurrency(any()) }
    }

    @Test
    fun `should fall back to createCoinCurrency when no matching coin is in the supplier's list`() = runTest {
        // Given — all returned coins are for a different network
        val fromStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true)
        val unrelatedCoin = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { network } returns mockk<Network>(relaxed = true) {
                every { id } returns mockk(relaxed = true)
                every { derivationPath } returns Network.DerivationPath.None
            }
        }
        val createdCoin = buildCoinCurrency(networkRawId = ethNetwork)

        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(unrelatedCoin)
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns createdCoin

        // When
        val result = sut.getNativeToken(fromStatus)

        // Then
        assertThat(result).isSameInstanceAs(createdCoin)
    }
}