package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerOffersParsingTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(CustomerOffersResponse::class.java)

    @Test
    fun `GIVEN a plastic offer without fee and data WHEN parse THEN the whole response still parses`() {
        // Act
        val offers = adapter.fromJson(PLACEHOLDER_PLASTIC_OFFER_JSON)!!.result.orEmpty()

        // Assert
        assertThat(offers).hasSize(2)
    }

    @Test
    fun `GIVEN a plastic offer without fee and data WHEN parse THEN the virtual offer keeps its fee and data`() {
        // Act
        val offers = adapter.fromJson(PLACEHOLDER_PLASTIC_OFFER_JSON)!!.result.orEmpty()

        // Assert
        val virtual = offers.single { it.type == "CARD_ISSUE_VIRTUAL_RAIN" }
        assertThat(virtual.fee?.amount).isEqualTo(BigDecimal("5.00"))
        assertThat(virtual.fee?.currency).isEqualTo("USD")
        assertThat(virtual.data?.orderType).isEqualTo("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2")
    }

    @Test
    fun `GIVEN a plastic offer without fee and data WHEN parse THEN its fee data are null and its image survives`() {
        // Act
        val offers = adapter.fromJson(PLACEHOLDER_PLASTIC_OFFER_JSON)!!.result.orEmpty()

        // Assert
        val plastic = offers.single { it.type == "CARD_ISSUE_PLASTIC_RAIN" }
        assertThat(plastic.fee).isNull()
        assertThat(plastic.data).isNull()
        assertThat(plastic.images?.single()?.url).isEqualTo(PLASTIC_IMAGE_URL)
    }

    @Test
    fun `GIVEN an offer without a type images and order type WHEN parse THEN they are null`() {
        // Act
        val offers = adapter.fromJson(UNTYPED_OFFER_JSON)!!.result.orEmpty()

        // Assert
        val offer = offers.single()
        assertThat(offer.type).isNull()
        assertThat(offer.images).isNull()
        assertThat(offer.data?.orderType).isNull()
    }

    @Test
    fun `GIVEN a fee object without amount and currency WHEN parse THEN they are null`() {
        // Act
        val offers = adapter.fromJson(EMPTY_FEE_JSON)!!.result.orEmpty()

        // Assert
        val fee = offers.single().fee
        assertThat(fee?.amount).isNull()
        assertThat(fee?.currency).isNull()
    }

    @Test
    fun `GIVEN a null result WHEN parse THEN the response still parses`() {
        // Act
        val result = adapter.fromJson(NULL_RESULT_JSON)!!

        // Assert
        assertThat(result.result).isNull()
    }

    private companion object {

        const val PLASTIC_IMAGE_URL = "https://images.us.paera.com/whitebird-plastic%201x.png"

        val PLACEHOLDER_PLASTIC_OFFER_JSON = """
            {
              "result": [
                {
                  "type": "CARD_ISSUE_VIRTUAL_RAIN",
                  "fee": {
                    "type": "OTC",
                    "amount": 5.00,
                    "currency": "USD",
                    "description": "Card issue fee"
                  },
                  "images": [
                    {
                      "type": "MAIN",
                      "url": "https://images.dev.us.paera.com/Digital-Platinum%201x.png"
                    }
                  ],
                  "data": {
                    "specification_name": "SP_000004",
                    "order_type": "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"
                  }
                },
                {
                  "type": "CARD_ISSUE_PLASTIC_RAIN",
                  "fee": null,
                  "images": [
                    {
                      "type": "MAIN",
                      "url": "$PLASTIC_IMAGE_URL"
                    }
                  ],
                  "data": null
                }
              ],
              "error": null
            }
        """.trimIndent()

        val UNTYPED_OFFER_JSON = """
            {
              "result": [
                {
                  "type": null,
                  "fee": { "amount": 5.00, "currency": "USD" },
                  "images": null,
                  "data": { "specification_name": "SP_000004", "order_type": null }
                }
              ],
              "error": null
            }
        """.trimIndent()

        val EMPTY_FEE_JSON = """
            {
              "result": [
                {
                  "type": "CARD_ISSUE_PLASTIC_RAIN",
                  "fee": { "type": "OTC", "description": "Card issue fee" },
                  "data": { "order_type": "CARD_ISSUE_PLASTIC_RAIN" }
                }
              ],
              "error": null
            }
        """.trimIndent()

        val NULL_RESULT_JSON = """
            {
              "result": null,
              "error": { "code": "SOMETHING_WENT_WRONG" }
            }
        """.trimIndent()
    }
}