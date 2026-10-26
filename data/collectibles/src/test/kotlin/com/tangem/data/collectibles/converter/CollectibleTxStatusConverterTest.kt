package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectibleTxStatusConverterTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a wire status WHEN decoded and converted THEN it maps to the lifecycle state`(model: StatusModel) {
        // Arrange
        val response = json.decodeFromString<CollectibleTxResponse>("""{"txId":"tx-id","status":"${model.wire}"}""")

        // Act
        val actual = CollectibleTxStatusConverter.convert(response.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class StatusModel(val wire: String, val expected: CollectibleTxStatus)

    private fun provideTestModels() = listOf(
        StatusModel(wire = "created", expected = CollectibleTxStatus.CREATED),
        StatusModel(wire = "waiting-tx-hash", expected = CollectibleTxStatus.WAITING_TX_HASH),
        StatusModel(wire = "purchase-tx-hash", expected = CollectibleTxStatus.PURCHASE_TX_HASH),
        StatusModel(wire = "waiting-for-payment", expected = CollectibleTxStatus.WAITING_FOR_PAYMENT),
        StatusModel(wire = "pack-purchased", expected = CollectibleTxStatus.PACK_PURCHASED),
        StatusModel(wire = "pack-opening-error", expected = CollectibleTxStatus.PACK_OPENING_ERROR),
        StatusModel(wire = "pack-opened", expected = CollectibleTxStatus.PACK_OPENED),
        StatusModel(wire = "nft-received", expected = CollectibleTxStatus.NFT_RECEIVED),
        StatusModel(wire = "auto-buybacked", expected = CollectibleTxStatus.AUTO_BUYBACKED),
        StatusModel(wire = "manually-buybacked", expected = CollectibleTxStatus.MANUALLY_BUYBACKED),
        StatusModel(wire = "expired", expected = CollectibleTxStatus.EXPIRED),
        StatusModel(wire = "refunded", expected = CollectibleTxStatus.REFUNDED),
        StatusModel(wire = "paused", expected = CollectibleTxStatus.PAUSED),
        StatusModel(wire = "something-new", expected = CollectibleTxStatus.UNKNOWN),
    )
}