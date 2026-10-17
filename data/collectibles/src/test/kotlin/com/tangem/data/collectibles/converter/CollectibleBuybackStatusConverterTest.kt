package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.collectibles.models.CollectibleBuybackStatus
import com.tangem.grow.datasource.collectibles.models.response.BuybackStatusResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectibleBuybackStatusConverterTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a wire status WHEN decoded and converted THEN it maps to the buyback state`(model: StatusModel) {
        // Arrange
        val response = json.decodeFromString<BuybackStatusResponse>(
            """{"buybackId":"buyback-id","status":"${model.wire}"}""",
        )

        // Act
        val actual = CollectibleBuybackStatusConverter.convert(response.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class StatusModel(val wire: String, val expected: CollectibleBuybackStatus)

    private fun provideTestModels() = listOf(
        StatusModel(wire = "created", expected = CollectibleBuybackStatus.CREATED),
        StatusModel(wire = "waiting-tx-hash", expected = CollectibleBuybackStatus.WAITING_TX_HASH),
        StatusModel(wire = "buyback-tx-sent", expected = CollectibleBuybackStatus.BUYBACK_TX_SENT),
        StatusModel(wire = "confirming", expected = CollectibleBuybackStatus.CONFIRMING),
        StatusModel(wire = "confirmed", expected = CollectibleBuybackStatus.CONFIRMED),
        StatusModel(wire = "expired", expected = CollectibleBuybackStatus.EXPIRED),
        StatusModel(wire = "something-new", expected = CollectibleBuybackStatus.UNKNOWN),
    )
}