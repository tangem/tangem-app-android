package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerMeKycParsingTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(CustomerMeResponse.Result::class.java)

    @Test
    fun `GIVEN kyc with country phone_mask and email WHEN parse THEN maps all fields`() {
        // Arrange
        val json = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "kyc": {
                "id": "k1", "provider": "provider", "status": "APPROVED", "risk": "LOW",
                "review_answer": "GREEN", "created_at": "2026-01-01T00:00:00Z",
                "country": "US", "phone_mask": "+1 (###) ###-####", "email": "a@b.co"
              }
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.kyc).isEqualTo(
            expectedKyc(country = "US", phoneMask = "+1 (###) ###-####", email = "a@b.co"),
        )
    }

    @Test
    fun `GIVEN kyc without the new fields WHEN parse THEN they are null`() {
        // Arrange
        val json = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "kyc": {
                "id": "k1", "provider": "provider", "status": "APPROVED", "risk": "LOW",
                "review_answer": "GREEN", "created_at": "2026-01-01T00:00:00Z"
              }
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.kyc).isEqualTo(expectedKyc(country = null, phoneMask = null, email = null))
    }

    private fun expectedKyc(country: String?, phoneMask: String?, email: String?) = CustomerMeResponse.Kyc(
        id = "k1",
        provider = "provider",
        status = "APPROVED",
        risk = "LOW",
        reviewAnswer = "GREEN",
        createdAt = "2026-01-01T00:00:00Z",
        country = country,
        phoneMask = phoneMask,
        email = email,
    )
}