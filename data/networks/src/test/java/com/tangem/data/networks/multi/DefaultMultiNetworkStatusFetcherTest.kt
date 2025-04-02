package com.tangem.data.networks.multi

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class DefaultMultiNetworkStatusFetcherTest {

    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()

    private val fetcher = DefaultMultiNetworkStatusFetcher(singleNetworkStatusFetcher = singleNetworkStatusFetcher)

    @Test
    fun `fetch networks statuses successfully`() = runTest {
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = ethereumAndStellar)

        val ethParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.first(),
        )

        val stellarParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.last(),
        )

        coEvery { singleNetworkStatusFetcher(ethParams) } returns Unit.right()
        coEvery { singleNetworkStatusFetcher(stellarParams) } returns Unit.right()

        val actual = fetcher(params)

        coVerify {
            singleNetworkStatusFetcher(ethParams)
            singleNetworkStatusFetcher(stellarParams)
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch networks statuses failure if one of them fails`() = runTest {
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = ethereumAndStellar)

        val ethParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.first(),
        )

        val stellarParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.last(),
        )

        val ethException = IllegalStateException("eth")
        coEvery { singleNetworkStatusFetcher(ethParams) } returns ethException.left()
        coEvery { singleNetworkStatusFetcher(stellarParams) } returns Unit.right()

        val actual = fetcher(params)

        coVerify {
            singleNetworkStatusFetcher(ethParams)
            singleNetworkStatusFetcher(stellarParams)
        }

        val expected = IllegalStateException("Failed to fetch network statuses")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch networks statuses failure if all of them fails`() = runTest {
        val params = MultiNetworkStatusFetcher.Params(userWalletId = userWalletId, networks = ethereumAndStellar)

        val ethParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.first(),
        )

        val stellarParams = SingleNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            network = ethereumAndStellar.last(),
        )

        coEvery { singleNetworkStatusFetcher(ethParams) } returns IllegalStateException("eth").left()
        coEvery { singleNetworkStatusFetcher(stellarParams) } returns IllegalStateException("stellar").left()

        val actual = fetcher(params)

        coVerify {
            singleNetworkStatusFetcher(ethParams)
            singleNetworkStatusFetcher(stellarParams)
        }

        val expected = IllegalStateException("Failed to fetch network statuses")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val ethereumAndStellar = MockCryptoCurrencyFactory().ethereumAndStellar.map { it.network }.toSet()
    }
}