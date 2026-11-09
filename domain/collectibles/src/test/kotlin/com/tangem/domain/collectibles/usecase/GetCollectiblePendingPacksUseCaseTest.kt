package com.tangem.domain.collectibles.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCollectiblePendingPacksUseCaseTest {

    private val collectiblesRepository: CollectiblesRepository = mockk()
    private val dismissedTxRepository: CollectiblesDismissedTxRepository = mockk()

    private val useCase = GetCollectiblePendingPacksUseCase(
        collectiblesRepository = collectiblesRepository,
        dismissedTxRepository = dismissedTxRepository,
    )

    private val dismissedTxIds = MutableStateFlow(emptySet<String>())

    @BeforeEach
    fun resetMocks() {
        clearMocks(collectiblesRepository, dismissedTxRepository)
        dismissedTxIds.value = emptySet()
        every { dismissedTxRepository.getDismissedTxIds() } returns dismissedTxIds
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an active purchase WHEN invoked THEN it is mapped by its status`(model: StatusModel) = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns listOf(activeTx(status = model.status))

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual).isEqualTo(listOfNotNull(model.expected))
    }

    @Test
    fun `GIVEN purchases in some order WHEN invoked THEN the backend's order is kept`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns listOf(
            activeTx(txId = "refunding", status = CollectibleTxStatus.REFUNDING),
            activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR),
            activeTx(txId = "ready", status = CollectibleTxStatus.PACK_PURCHASED),
        )

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual.map { it.txId }).containsExactly("refunding", "failed", "ready").inOrder()
    }

    @Test
    fun `GIVEN a pack to open without its machine WHEN invoked THEN it is left out`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns
            listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED, machineId = null))

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN dismissed purchases WHEN invoked THEN only the dismissible ones are hidden`() = runTest {
        // Arrange
        dismissedTxIds.value = setOf("failed", "refunding", "ready")
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns listOf(
            activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR),
            activeTx(txId = "refunding", status = CollectibleTxStatus.REFUNDING),
            activeTx(txId = "ready", status = CollectibleTxStatus.PACK_PURCHASED),
        )

        // Act
        val actual = useCase(WALLET_ID).first()

        // Assert
        assertThat(actual.map { it.txId }).containsExactly("ready")
    }

    @Test
    fun `GIVEN a shown banner WHEN its purchase is dismissed THEN it disappears without a new request`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns
            listOf(activeTx(txId = "failed", status = CollectibleTxStatus.PACK_OPENING_ERROR))
        val emissions = mutableListOf<List<CollectiblePendingPack>>()
        val job = backgroundScope.launch { useCase(WALLET_ID).toList(emissions) }
        runCurrent()

        // Act
        dismissedTxIds.value = setOf("failed")
        runCurrent()

        // Assert
        assertThat(emissions.last()).isEmpty()
        coVerify(exactly = 1) { collectiblesRepository.getActiveTxs(WALLET_ID) }
        job.cancel()
    }

    @Test
    fun `GIVEN no payment in progress WHEN invoked THEN the purchases are read once`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returns
            listOf(activeTx(status = CollectibleTxStatus.PACK_PURCHASED))
        val job = backgroundScope.launch { useCase(WALLET_ID).toList() }

        // Act
        advanceTimeBy(POLL_INTERVAL_MS * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { collectiblesRepository.getActiveTxs(WALLET_ID) }
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN it lands THEN polling goes on every five seconds until then`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returnsMany listOf(
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
        coVerify(exactly = 3) { collectiblesRepository.getActiveTxs(WALLET_ID) }
        assertThat(emissions.last()).containsExactly(
            CollectiblePendingPack.ReadyToOpen(txId = TX_ID, machineId = MACHINE_ID, isTurboModeEnabled = false),
        )
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN a read fails THEN the last answer stays and polling goes on`() = runTest {
        // Arrange
        var calls = 0
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } answers {
            calls++
            when (calls) {
                1 -> listOf(activeTx(status = CollectibleTxStatus.WAITING_FOR_PAYMENT))
                2 -> error("offline")
                else -> emptyList()
            }
        }
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
        coVerify(exactly = 3) { collectiblesRepository.getActiveTxs(WALLET_ID) }
        job.cancel()
    }

    @Test
    fun `GIVEN a payment in progress WHEN the same answer comes again THEN it is not emitted twice`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getActiveTxs(WALLET_ID) } returnsMany listOf(
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
        coVerify(exactly = 3) { collectiblesRepository.getActiveTxs(WALLET_ID) }
        job.cancel()
    }

    internal data class StatusModel(val status: CollectibleTxStatus, val expected: CollectiblePendingPack?)

    private fun provideTestModels(): List<StatusModel> {
        val paymentInProgress = CollectiblePendingPack.PaymentInProgress(txId = TX_ID)
        val readyToOpen = CollectiblePendingPack.ReadyToOpen(
            txId = TX_ID,
            machineId = MACHINE_ID,
            isTurboModeEnabled = false,
        )

        return listOf(
            StatusModel(CollectibleTxStatus.CREATED, paymentInProgress),
            StatusModel(CollectibleTxStatus.WAITING_TX_HASH, paymentInProgress),
            StatusModel(CollectibleTxStatus.PURCHASE_TX_SENT, paymentInProgress),
            StatusModel(CollectibleTxStatus.WAITING_FOR_PAYMENT, paymentInProgress),
            StatusModel(CollectibleTxStatus.PACK_PURCHASED, readyToOpen),
            StatusModel(CollectibleTxStatus.PACK_OPENING_ERROR, CollectiblePendingPack.OpenFailed(txId = TX_ID)),
            StatusModel(CollectibleTxStatus.REFUNDING, CollectiblePendingPack.Refunding(txId = TX_ID)),
            StatusModel(CollectibleTxStatus.PAUSED, null),
            StatusModel(CollectibleTxStatus.PACK_OPENED, null),
            StatusModel(CollectibleTxStatus.NFT_RECEIVED, null),
            StatusModel(CollectibleTxStatus.UNKNOWN, null),
        )
    }

    private fun activeTx(
        txId: String = TX_ID,
        status: CollectibleTxStatus,
        machineId: String? = MACHINE_ID,
    ) = CollectibleActiveTx(txId = txId, status = status, machineId = machineId, isTurboModeEnabled = false)

    private companion object {
        val WALLET_ID = UserWalletId("aa")
        const val TX_ID = "tx-id"
        const val MACHINE_ID = "pokemon_50"
        const val POLL_INTERVAL_MS = 5_000L
    }
}