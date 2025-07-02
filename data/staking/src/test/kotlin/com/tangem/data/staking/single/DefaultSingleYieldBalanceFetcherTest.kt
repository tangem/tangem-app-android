package com.tangem.data.staking.single

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
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
internal class DefaultSingleYieldBalanceFetcherTest {

    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher = mockk()

    private val fetcher = DefaultSingleYieldBalanceFetcher(
        multiYieldBalanceFetcher = multiYieldBalanceFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiYieldBalanceFetcher)
    }

    @Test
    fun `fetch yield balance successfully`() = runTest {
        // Arrange
        val params = SingleYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        val multiParams = MultiYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyIdWithNetworkMap = mapOf(ton.id to ton.network),
        )

        val multiResult = Unit.right()

        coEvery { multiYieldBalanceFetcher(params = multiParams) } returns multiResult

        // Act
        val actual = fetcher.invoke(params).isRight()

        // Assert
        Truth.assertThat(actual).isTrue()

        coVerify { multiYieldBalanceFetcher(params = multiParams) }
    }

    @Test
    fun `fetch yield balance failure`() = runTest {
        // Arrange
        val params = SingleYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        val multiParams = MultiYieldBalanceFetcher.Params(
            userWalletId = userWalletId,
            currencyIdWithNetworkMap = mapOf(ton.id to ton.network),
        )

        val multiResult = IllegalStateException().left()

        coEvery { multiYieldBalanceFetcher(params = multiParams) } returns multiResult

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        Truth.assertThat(actual).isEqualTo(multiResult)
        coVerify { multiYieldBalanceFetcher(params = multiParams) }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val ton = MockCryptoCurrencyFactory().createCoin(Blockchain.TON)
    }
}