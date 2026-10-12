package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterOutcome
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
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()

    private lateinit var testScope: TestScope
    private lateinit var useCase: YieldSupplyPendingTracker

    private val userWalletId = UserWalletId("00")

    @BeforeEach
    fun setUp() {
        testScope = TestScope(StandardTestDispatcher())
        useCase = YieldSupplyPendingTracker(
            yieldSupplyRepository = yieldSupplyRepository,
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            coroutineScope = TestAppCoroutineScope(testScope),
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

        testScope.advanceTimeBy(10_001L * MAX_ATTEMPTS)

        coVerify(exactly = MAX_ATTEMPTS) {
            yieldSupplyRepository.getPendingTxHashes(userWalletId, token)
        }

        testScope.advanceTimeBy(10_001L)

        coVerify(exactly = MAX_ATTEMPTS) {
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

    @Test
    fun `GIVEN enter tx deposited WHEN checked THEN activates protocol and stops tracking`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, YieldSupplyEnterOutcome.Deposited)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 1) {
            yieldSupplyRepository.activateProtocol(userWalletId, token, OWNER_ADDRESS)
        }
    }

    @Test
    fun `GIVEN enter tx failed WHEN checked THEN activates protocol so the relayer can deposit`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, YieldSupplyEnterOutcome.Failed)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 1) {
            yieldSupplyRepository.activateProtocol(userWalletId, token, OWNER_ADDRESS)
        }
    }

    @Test
    fun `GIVEN relayer already deposited WHEN checked THEN does not activate protocol`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, YieldSupplyEnterOutcome.DepositedByRelayer)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 0) { yieldSupplyRepository.activateProtocol(any(), any(), any()) }
    }

    @Test
    fun `GIVEN enter tx not mined WHEN checked THEN does not activate protocol and keeps tracking`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, outcome = null)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        testScope.advanceTimeBy(10_001L * 3)

        // Assert
        coVerify(exactly = 0) { yieldSupplyRepository.activateProtocol(any(), any(), any()) }
        coVerify(exactly = 3) {
            yieldSupplyTransactionRepository.getEnterTransactionOutcome(userWalletId, token, ENTER_TX_HASH)
        }
    }

    @Test
    fun `GIVEN nothing to deposit WHEN setup settles THEN activates protocol`() = runTest {
        // Arrange
        val token = createToken()
        coEvery { yieldSupplyRepository.getPendingTxHashes(userWalletId, token) } returns emptyList()
        coEvery { yieldSupplyRepository.activateProtocol(any(), any(), any()) } returns true
        coEvery { singleNetworkStatusFetcher.invoke(any()) } returns Either.Right(Unit)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext(enterTransactionHash = null))
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 1) {
            yieldSupplyRepository.activateProtocol(userWalletId, token, OWNER_ADDRESS)
        }
        coVerify(exactly = 0) {
            yieldSupplyTransactionRepository.getEnterTransactionOutcome(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN nothing to deposit WHEN setup still pending THEN does not activate protocol`() = runTest {
        // Arrange
        val token = createToken()
        coEvery { yieldSupplyRepository.getPendingTxHashes(userWalletId, token) } returns ENTER_TX_IDS
        coEvery { singleNetworkStatusFetcher.invoke(any()) } returns Either.Right(Unit)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext(enterTransactionHash = null))
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 0) { yieldSupplyRepository.activateProtocol(any(), any(), any()) }
    }

    @Test
    fun `GIVEN deposit settling WHEN stop requested THEN settled deposit does not activate protocol`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, YieldSupplyEnterOutcome.Deposited)
        coEvery { yieldSupplyRepository.getPendingTxHashes(userWalletId, token) } returns emptyList()

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        useCase.addPending(userWalletId, token, listOf("0xexit"))
        testScope.advanceTimeBy(10_001L)

        // Assert
        coVerify(exactly = 0) { yieldSupplyRepository.activateProtocol(any(), any(), any()) }
        coVerify(exactly = 0) { yieldSupplyTransactionRepository.getEnterTransactionOutcome(any(), any(), any()) }
    }

    @Test
    fun `GIVEN deposit settling WHEN hasPendingDeposit THEN true until settled`() = runTest {
        // Arrange
        val token = createToken()
        stubEnterOutcome(token, YieldSupplyEnterOutcome.Deposited)

        // Act
        useCase.addPending(userWalletId, token, ENTER_TX_IDS, depositContext())
        val beforeSettle = useCase.hasPendingDeposit(userWalletId, token.id)
        testScope.advanceTimeBy(10_001L)
        val afterSettle = useCase.hasPendingDeposit(userWalletId, token.id)

        // Assert
        assertThat(beforeSettle).isTrue()
        assertThat(afterSettle).isFalse()
    }

    @Test
    fun `GIVEN tracking without deposit context WHEN hasPendingDeposit THEN false`() = runTest {
        // Arrange
        val token = createToken()
        coEvery { yieldSupplyRepository.getPendingTxHashes(userWalletId, token) } returns listOf("0xapprove")
        coEvery { singleNetworkStatusFetcher.invoke(any()) } returns Either.Right(Unit)

        // Act
        useCase.addPending(userWalletId, token, listOf("0xapprove"))

        // Assert
        assertThat(useCase.hasPendingDeposit(userWalletId, token.id)).isFalse()
    }

    private fun depositContext(enterTransactionHash: String? = ENTER_TX_HASH) =
        YieldSupplyPendingTracker.DepositContext(
            ownerAddress = OWNER_ADDRESS,
            enterTransactionHash = enterTransactionHash,
        )

    private fun stubEnterOutcome(token: CryptoCurrency.Token, outcome: YieldSupplyEnterOutcome?) {
        coEvery {
            yieldSupplyTransactionRepository.getEnterTransactionOutcome(userWalletId, token, ENTER_TX_HASH)
        } returns outcome
        coEvery { yieldSupplyRepository.activateProtocol(any(), any(), any()) } returns true
        coEvery { singleNetworkStatusFetcher.invoke(any()) } returns Either.Right(Unit)
    }

    private companion object {
        /** Mirrors YieldSupplyPendingTracker.MAX_ATTEMPTS, which is private. */
        const val MAX_ATTEMPTS = 15
        const val ENTER_TX_HASH = "0xenter"
        const val OWNER_ADDRESS = "0xowner"
        val ENTER_TX_IDS = listOf(ENTER_TX_HASH)
    }

    private fun createToken(
        networkId: String = "ethereum",
        contractAddress: String = "0xToken",
    ): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        val network = Network(
            id = Network.ID(value = networkId, derivationPath = derivationPath),
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