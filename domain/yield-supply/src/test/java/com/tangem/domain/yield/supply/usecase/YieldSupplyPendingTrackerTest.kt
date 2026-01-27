package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.yield.supply.YieldSupplyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyPendingTrackerTest {

    private val yieldSupplyRepository: YieldSupplyRepository = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()

    private lateinit var testScope: TestScope
    private lateinit var useCase: YieldSupplyPendingTracker

    private val userWalletId = UserWalletId("00")

    @BeforeEach
    fun setUp() {
        testScope = TestScope(StandardTestDispatcher())
        useCase = YieldSupplyPendingTracker(
            yieldSupplyRepository = yieldSupplyRepository,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            coroutineScope = testScope,
        )
    }

    @Test
    fun `GIVEN new tx ids WHEN addPending THEN stores entry for tracking`() = runTest {
        val token = createToken()
        val txIds = listOf("0xabc123", "0xdef456")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns txIds
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, txIds)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }
    }

    @Test
    fun `GIVEN existing entry WHEN addPending THEN merges tx ids`() = runTest {
        val token = createToken()
        val firstTxIds = listOf("0xfirst")
        val secondTxIds = listOf("0xsecond")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns firstTxIds + secondTxIds
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, firstTxIds)
        useCase.addPending(userWalletId, token, secondTxIds)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }
    }

    @Test
    fun `GIVEN tx still pending WHEN addPending THEN triggers network status refresh`() = runTest {
        val token = createToken()
        val txIds = listOf("0xpending")
        val paramsSlot = slot<SingleNetworkStatusFetcher.Params>()

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns txIds
        coEvery {
            singleNetworkStatusFetcher.invoke(capture(paramsSlot))
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, txIds)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            singleNetworkStatusFetcher.invoke(any())
        }
        assertThat(paramsSlot.captured.userWalletId).isEqualTo(userWalletId)
        assertThat(paramsSlot.captured.network).isEqualTo(token.network)
    }

    @Test
    fun `GIVEN tx no longer pending WHEN addPending THEN removes entry from tracking and refreshes network`() = runTest {
        val token = createToken()
        val txIds = listOf("0xconfirmed")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns emptyList()
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, txIds)

        testScope.advanceTimeBy(10_001L)

        coVerify(exactly = 1) {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher.invoke(any())
        }
    }

    @Test
    fun `GIVEN max attempts reached WHEN addPending THEN stops tracking entry`() = runTest {
        val token = createToken()
        val txIds = listOf("0xstuck")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns txIds
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, txIds)

        testScope.advanceTimeBy(10_001L * 6)

        coVerify(exactly = 6) {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }

        testScope.advanceTimeBy(10_001L)

        coVerify(exactly = 6) {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }
    }

    @Test
    fun `GIVEN multiple wallets WHEN addPending THEN tracks each separately`() = runTest {
        val token = createToken()
        val userWalletId1 = UserWalletId("00")
        val userWalletId2 = UserWalletId("01")
        val txIds1 = listOf("0xtx1")
        val txIds2 = listOf("0xtx2")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId1, token)
        } returns txIds1
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId2, token)
        } returns txIds2
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId1, token, txIds1)
        useCase.addPending(userWalletId2, token, txIds2)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            yieldSupplyRepository.getPendingTxHashes(userWalletId1, token)
            yieldSupplyRepository.getPendingTxHashes(userWalletId2, token)
        }
    }

    @Test
    fun `GIVEN multiple wallets with same network WHEN checkAllTracked THEN fetches network status once per unique pair`() = runTest {
        val token = createToken()
        val userWalletId1 = UserWalletId("00")
        val userWalletId2 = UserWalletId("01")
        val txIds1 = listOf("0xtx1")
        val txIds2 = listOf("0xtx2")
        val capturedParams = mutableListOf<SingleNetworkStatusFetcher.Params>()

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId1, token)
        } returns txIds1
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId2, token)
        } returns txIds2
        coEvery {
            singleNetworkStatusFetcher.invoke(capture(capturedParams))
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId1, token, txIds1)
        useCase.addPending(userWalletId2, token, txIds2)

        testScope.advanceTimeBy(10001)

        assertThat(capturedParams).hasSize(2)
        assertThat(capturedParams.map { it.userWalletId }.toSet())
            .containsExactly(userWalletId1, userWalletId2)
        assertThat(capturedParams.map { it.network }.toSet())
            .containsExactly(token.network)
    }

    @Test
    fun `GIVEN same wallet with different currencies WHEN addPending THEN tracks each currency separately`() = runTest {
        val token1 = createToken(networkId = "ethereum", contractAddress = "0xToken1")
        val token2 = createToken(networkId = "polygon", contractAddress = "0xToken2")
        val txIds1 = listOf("0xtx1")
        val txIds2 = listOf("0xtx2")

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token1)
        } returns txIds1
        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token2)
        } returns txIds2
        coEvery {
            singleNetworkStatusFetcher.invoke(any())
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token1, txIds1)
        useCase.addPending(userWalletId, token2, txIds2)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token1)
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token2)
        }
    }

    @Test
    fun `GIVEN partial tx match WHEN addPending THEN still triggers refresh`() = runTest {
        val token = createToken()
        val trackedTxIds = listOf("0xtracked1", "0xtracked2")
        val pendingTxIds = listOf("0xtracked1")
        val paramsSlot = slot<SingleNetworkStatusFetcher.Params>()

        coEvery {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        } returns pendingTxIds
        coEvery {
            singleNetworkStatusFetcher.invoke(capture(paramsSlot))
        } returns Either.Right(Unit)

        useCase.addPending(userWalletId, token, trackedTxIds)

        testScope.advanceTimeBy(10_001L)

        coVerify {
            singleNetworkStatusFetcher.invoke(any())
        }
        assertThat(paramsSlot.captured.userWalletId).isEqualTo(userWalletId)
    }

    private fun createToken(
        networkId: String = "ethereum",
        contractAddress: String = "0xToken",
    ): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = networkId, derivationPath = derivationPath),
            backendId = networkId,
            name = networkId,
            currencySymbol = networkId.take(3).uppercase(),
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(networkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(contractAddress),
            ),
            network = network,
            name = "TEST_TOKEN",
            symbol = "TTK",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }
}