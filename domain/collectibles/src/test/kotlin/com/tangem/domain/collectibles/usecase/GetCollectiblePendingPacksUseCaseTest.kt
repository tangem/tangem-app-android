package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import arrow.core.left
import arrow.core.right
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsSupplier
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCollectiblePendingPacksUseCaseTest {

    private val activeTxsSupplier: CollectiblesActiveTxsSupplier = mockk()
    private val activeTxsFetcher: CollectiblesActiveTxsFetcher = mockk()
    private val dismissedTxRepository: CollectiblesDismissedTxRepository = mockk()

    private val useCase = GetCollectiblePendingPacksUseCase(
        activeTxsSupplier = activeTxsSupplier,
        activeTxsFetcher = activeTxsFetcher,
        dismissedTxRepository = dismissedTxRepository,
    )

    private val storedActiveTxs = MutableStateFlow<List<CollectibleActiveTx>?>(null)
    private val dismissedTxIds = MutableStateFlow(emptySet<String>())

    @BeforeEach
    fun resetMocks() {
        clearMocks(activeTxsSupplier, activeTxsFetcher, dismissedTxRepository)
        storedActiveTxs.value = null
        dismissedTxIds.value = emptySet()
        every { activeTxsSupplier(WALLET_ID) } returns storedActiveTxs.filterNotNull()
        every { dismissedTxRepository.getDismissedTxIds() } returns dismissedTxIds
    }

    @Test
    fun `GIVEN purchases in some order WHEN invoked THEN the backend's order is kept`() = runTest {
        // Arrange
        givenReads(
            listOf(
                activeTx(txId = "refunding", status = CollectibleTxStatus.REFUNDING),
                activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR),
                activeTx(txId = "ready", status = CollectibleTxStatus.PACK_PURCHASED),
            ),
        )

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual.map { it.txId }).containsExactly("refunding", "failed", "ready").inOrder()
    }

    @Test
    fun `GIVEN dismissed purchases WHEN invoked THEN only the dismissible ones are hidden`() = runTest {
        // Arrange
        dismissedTxIds.value = setOf("failed", "refunding", "ready")
        givenReads(
            listOf(
                activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR),
                activeTx(txId = "refunding", status = CollectibleTxStatus.REFUNDING),
                activeTx(txId = "ready", status = CollectibleTxStatus.PACK_PURCHASED),
            ),
        )

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual.map { it.txId }).containsExactly("ready")
    }

    @Test
    fun `GIVEN a shown banner WHEN its purchase is dismissed THEN it disappears without a new request`() = runTest {
        // Arrange
        givenReads(listOf(activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR)))
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }
        runCurrent()

        // Act
        dismissedTxIds.value = setOf("failed")
        runCurrent()

        // Assert
        assertThat(emissions.last()).isEmpty()
        coVerify(exactly = 1) { activeTxsFetcher(FETCHER_PARAMS) }
        job.cancel()
    }

    @Test
    fun `GIVEN no payment in progress WHEN invoked THEN the purchases are read once`() = runTest {
        // Arrange
        givenReads(listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED)))
        val job = backgroundScope.launch { useCase(WALLET_ID).toList() }

        // Act
        advanceTimeBy(POLL_INTERVAL_MS * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { activeTxsFetcher(FETCHER_PARAMS) }
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN it lands THEN polling goes on every five seconds until then`() = runTest {
        // Arrange
        givenReads(
            listOf(activeTx(status = CollectibleTxStatus.WAITING_FOR_PAYMENT)),
            listOf(activeTx(status = CollectibleTxStatus.PURCHASE_TX_SENT)),
            listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED)),
        )
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }
        runCurrent()

        // Act
        advanceTimeBy(POLL_INTERVAL_MS * 5)
        runCurrent()

        // Assert
        coVerify(exactly = 3) { activeTxsFetcher(FETCHER_PARAMS) }
        assertThat(emissions.last()).containsExactly(
            CollectiblePendingPack.ReadyToOpen(txId = TX_ID, machineId = MACHINE_ID, isTurboModeEnabled = false),
        )
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN a read fails THEN the last answer stays and polling goes on`() = runTest {
        // Arrange
        givenReads(
            listOf(activeTx(status = CollectibleTxStatus.WAITING_FOR_PAYMENT)),
            null,
            emptyList(),
        )
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }
        runCurrent()

        // Act
        advanceTimeBy(POLL_INTERVAL_MS + 1)
        runCurrent()
        val afterFailure = emissions.last()
        advanceTimeBy(POLL_INTERVAL_MS)
        runCurrent()

        // Assert
        assertThat(afterFailure).containsExactly(CollectiblePendingPack.PaymentInProgress(txId = TX_ID))
        assertThat(emissions.last()).isEmpty()
        coVerify(exactly = 3) { activeTxsFetcher(FETCHER_PARAMS) }
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN the same answer comes again THEN it is not emitted twice`() = runTest {
        // Arrange
        givenReads(
            listOf(activeTx(status = CollectibleTxStatus.WAITING_FOR_PAYMENT)),
            listOf(activeTx(status = CollectibleTxStatus.WAITING_FOR_PAYMENT)),
            emptyList(),
        )
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }

        // Act
        advanceTimeBy(POLL_INTERVAL_MS * 2 + 1)
        runCurrent()

        // Assert
        assertThat(emissions).containsExactly(
            listOf(CollectiblePendingPack.PaymentInProgress(txId = TX_ID)),
            emptyList<CollectiblePendingPack>(),
        ).inOrder()
        coVerify(exactly = 3) { activeTxsFetcher(FETCHER_PARAMS) }
        job.cancel()
    }

    @Test
    fun `GIVEN an earlier read WHEN invoked THEN it is shown before the new read answers`() = runTest {
        // Arrange
        storedActiveTxs.value = listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED))
        coEvery { activeTxsFetcher(FETCHER_PARAMS) } coAnswers { awaitCancellation() }

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual).containsExactly(
            CollectiblePendingPack.ReadyToOpen(txId = TX_ID, machineId = MACHINE_ID, isTurboModeEnabled = false),
        )
    }

    @Test
    fun `GIVEN a pack to open WHEN the store changes outside THEN the new state is emitted`() = runTest {
        // Arrange
        givenReads(listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED)))
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }
        runCurrent()

        // Act
        storedActiveTxs.value = emptyList()
        runCurrent()

        // Assert
        assertThat(emissions.last()).isEmpty()
        coVerify(exactly = 1) { activeTxsFetcher(FETCHER_PARAMS) }
        job.cancel()
    }

    /** Each fetch stores the next read; `null` is a failed fetch, which keeps the store. The last read repeats. */
    private fun givenReads(vararg reads: List<CollectibleActiveTx>?) {
        val queue = ArrayDeque(reads.toList())
        coEvery { activeTxsFetcher(FETCHER_PARAMS) } answers {
            val read = if (queue.size > 1) queue.removeFirst() else queue.first()
            if (read == null) {
                IllegalStateException("offline").left()
            } else {
                storedActiveTxs.value = read
                Unit.right()
            }
        }
    }

    private fun activeTx(
        txId: String = TX_ID,
        status: CollectibleTxStatus,
        machineId: String? = MACHINE_ID,
    ) = CollectibleActiveTx(txId = txId, status = status, machineId = machineId, isTurboModeEnabled = false)

    private companion object {
        val WALLET_ID = UserWalletId("aa")
        val FETCHER_PARAMS = CollectiblesActiveTxsFetcher.Params(WALLET_ID)
        const val TX_ID = "tx-id"
        const val MACHINE_ID = "pokemon_50"
        const val POLL_INTERVAL_MS = 5_000L
    }
}