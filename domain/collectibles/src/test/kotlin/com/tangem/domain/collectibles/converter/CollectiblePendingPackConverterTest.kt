package com.tangem.domain.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblePendingPackConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an active purchase WHEN converted THEN it is mapped by its status`(model: StatusModel) {
        // Act
        val actual = CollectiblePendingPackConverter.convert(activeTx(status = model.status))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a pack to open in turbo mode WHEN converted THEN the flag is kept`() {
        // Act
        val actual = CollectiblePendingPackConverter.convert(
            activeTx(status = CollectibleTxStatus.PACK_PURCHASED).copy(isTurboModeEnabled = true),
        )

        // Assert
        assertThat(actual).isEqualTo(
            CollectiblePendingPack.ReadyToOpen(txId = TX_ID, machineId = MACHINE_ID, isTurboModeEnabled = true),
        )
    }

    @Test
    fun `GIVEN a pack to open without its machine WHEN converted THEN it is left out`() {
        // Act
        val actual = CollectiblePendingPackConverter.convert(
            activeTx(status = CollectibleTxStatus.PACK_PURCHASED, machineId = null),
        )

        // Assert
        assertThat(actual).isNull()
    }

    internal data class StatusModel(val status: CollectibleTxStatus, val expected: CollectiblePendingPack?)

    private fun provideTestModels(): List<StatusModel> {
        val paymentInProgress = CollectiblePendingPack.PaymentInProgress(txId = TX_ID)

        return listOf(
            StatusModel(CollectibleTxStatus.CREATED, paymentInProgress),
            StatusModel(CollectibleTxStatus.WAITING_TX_HASH, paymentInProgress),
            StatusModel(CollectibleTxStatus.PURCHASE_TX_SENT, paymentInProgress),
            StatusModel(CollectibleTxStatus.WAITING_FOR_PAYMENT, paymentInProgress),
            StatusModel(
                CollectibleTxStatus.PACK_PURCHASED,
                CollectiblePendingPack.ReadyToOpen(txId = TX_ID, machineId = MACHINE_ID, isTurboModeEnabled = false),
            ),
            StatusModel(CollectibleTxStatus.PACK_OPENING_ERROR, CollectiblePendingPack.OpenFailed(txId = TX_ID)),
            StatusModel(CollectibleTxStatus.REFUNDING, CollectiblePendingPack.Refunding(txId = TX_ID)),
            StatusModel(CollectibleTxStatus.PACK_OPENED, null),
            StatusModel(CollectibleTxStatus.NFT_RECEIVED, null),
            StatusModel(CollectibleTxStatus.AUTO_BUYBACKED, null),
            StatusModel(CollectibleTxStatus.MANUALLY_BUYBACKED, null),
            StatusModel(CollectibleTxStatus.EXPIRED, null),
            StatusModel(CollectibleTxStatus.REFUNDED, null),
            StatusModel(CollectibleTxStatus.PAUSED, null),
            StatusModel(CollectibleTxStatus.UNKNOWN, null),
        )
    }

    private fun activeTx(status: CollectibleTxStatus, machineId: String? = MACHINE_ID) = CollectibleActiveTx(
        txId = TX_ID,
        status = status,
        machineId = machineId,
        isTurboModeEnabled = false,
    )

    private companion object {
        const val TX_ID = "tx-id"
        const val MACHINE_ID = "pokemon_50"
    }
}