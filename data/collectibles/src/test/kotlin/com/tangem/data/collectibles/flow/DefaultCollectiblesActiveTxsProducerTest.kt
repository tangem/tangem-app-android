package com.tangem.data.collectibles.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.store.CollectiblesActiveTxsStore
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsProducer
import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.getEmittedValues
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultCollectiblesActiveTxsProducerTest {

    private val store = CollectiblesActiveTxsStore(store = RuntimeSharedStore())

    private val producer = DefaultCollectiblesActiveTxsProducer(
        params = CollectiblesActiveTxsProducer.Params(WALLET_ID),
        flowProducerTools = mockk(),
        activeTxsStore = store,
    )

    @Test
    fun `GIVEN reads of two wallets WHEN produced THEN only its own wallet's read is emitted`() = runTest {
        // Arrange
        val own = listOf(activeTx("own"))
        store.store(userWalletId = UserWalletId("bb"), activeTxs = listOf(activeTx("other")))
        store.store(userWalletId = WALLET_ID, activeTxs = own)

        // Act
        val actual = getEmittedValues(producer.produce())

        // Assert
        assertThat(actual).containsExactly(own)
    }

    @Test
    fun `GIVEN nothing read for the wallet WHEN produced THEN nothing is emitted`() = runTest {
        // Arrange
        store.store(userWalletId = UserWalletId("bb"), activeTxs = listOf(activeTx("other")))

        // Act
        val actual = getEmittedValues(producer.produce())

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun activeTx(txId: String) = CollectibleActiveTx(
        txId = txId,
        status = CollectibleTxStatus.PACK_PURCHASED,
        machineId = null,
        isTurboModeEnabled = false,
    )

    private companion object {
        val WALLET_ID = UserWalletId("aa")
    }
}