package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderResponse
import org.junit.jupiter.api.Test

internal class PolymarketPlaceOrderWireFormatTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun `GIVEN the exchange's own answer WHEN decoded THEN every field is read under its wire name`() {
        // Arrange — a real matched order, reduced to one line
        val json = """
            {"errorMsg":"","orderID":"0xf1503cb4de25010eaea8a8d51c1d666f8c0b9191d710af13cac99832c6a52608",
            "takingAmount":"7.692306","makingAmount":"0.999999","status":"matched",
            "transactionsHashes":["0x7365a3d24a458b9dd00c1b713bb1beca11822432a929ced3124d29fa300ad530"],
            "success":true}
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PolymarketPlaceOrderResponse::class.java).fromJson(json)!!

        // Assert
        assertThat(actual.isSuccess).isTrue()
        assertThat(actual.orderId).isEqualTo("0xf1503cb4de25010eaea8a8d51c1d666f8c0b9191d710af13cac99832c6a52608")
        assertThat(actual.status).isEqualTo("matched")
        assertThat(actual.makingAmount).isEqualTo("0.999999")
        assertThat(actual.takingAmount).isEqualTo("7.692306")
        assertThat(actual.transactionHashes)
            .containsExactly("0x7365a3d24a458b9dd00c1b713bb1beca11822432a929ced3124d29fa300ad530")
        assertThat(actual.errorMessage).isEmpty()
    }

    @Test
    fun `GIVEN a refusal reported inside a success WHEN decoded THEN the reason survives`() {
        // Arrange — the exchange states some refusals with 200 and success=false
        val json = """{"success":false,"errorMsg":"not enough balance / allowance","orderID":null}"""

        // Act
        val actual = moshi.adapter(PolymarketPlaceOrderResponse::class.java).fromJson(json)!!

        // Assert
        assertThat(actual.isSuccess).isFalse()
        assertThat(actual.errorMessage).isEqualTo("not enough balance / allowance")
        assertThat(actual.orderId).isNull()
        assertThat(actual.makingAmount).isNull()
    }
}