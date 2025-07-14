package com.tangem.data.networks.single

import arrow.core.Either
import arrow.core.left
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.utils.assertEither
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleNetworkStatusFetcherTest {

    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()

    private val fetcher = DefaultSingleNetworkStatusFetcher(multiNetworkStatusFetcher = multiNetworkStatusFetcher)

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiNetworkStatusFetcher)
    }

    @Test
    fun `fetch successfully`() = runTest {
        // Arrange
        val params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = ethereum.network)
        val multiParams = MultiNetworkStatusFetcher.Params(userWalletId, setOf(params.network))
        val multiFetcherResult = Either.Right(Unit)

        coEvery { multiNetworkStatusFetcher(params = multiParams) } returns multiFetcherResult

        // Act
        val actual = fetcher(params)

        // Assert
        val expected = multiFetcherResult
        assertEither(actual, expected)

        coVerify(exactly = 1) { multiNetworkStatusFetcher(params = multiParams) }
    }

    @Test
    fun `fetch failure`() = runTest {
        // Arrange
        val params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = ethereum.network)
        val multiParams = MultiNetworkStatusFetcher.Params(userWalletId, setOf(params.network))
        val multiFetcherResult = IllegalStateException("Error").left()

        coEvery { multiNetworkStatusFetcher(params = multiParams) } returns multiFetcherResult

        // Act
        val actual = fetcher(params)

        // Arrange
        val expected = multiFetcherResult
        assertEither(actual, expected)

        coVerify(exactly = 1) { multiNetworkStatusFetcher(params = multiParams) }
    }

    private companion object {

        val userWalletId = UserWalletId("011")
        val ethereum = MockCryptoCurrencyFactory().ethereum
    }
}