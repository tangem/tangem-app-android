package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerMeProfileParsingTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(CustomerMeResponse.Result::class.java)

    @Test
    fun `GIVEN profile with country phone_mask email and emboss_name WHEN parse THEN maps all fields`() {
        // Arrange
        val json = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "kyc": {
                "id": "k1", "provider": "provider", "status": "APPROVED", "risk": "LOW",
                "review_answer": "GREEN", "created_at": "2026-01-01T00:00:00Z"
              },
              "profile": {
                "country": "US", "phone_mask": "+1 ###-###-####", "email": "a@b.co",
                "emboss_name": "JOHNNY SILVERHAND"
              }
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.profile).isEqualTo(
            CustomerMeResponse.Profile(
                country = "US",
                phoneMask = "+1 ###-###-####",
                email = "a@b.co",
                embossName = "JOHNNY SILVERHAND",
            ),
        )
    }

    @Test
    fun `GIVEN response without a profile WHEN parse THEN profile is null`() {
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
        assertThat(result.profile).isNull()
    }

    @Test
    fun `GIVEN a partially filled profile WHEN parse THEN the absent fields are null`() {
        // Arrange
        val json = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "profile": { "country": "DE" }
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.profile).isEqualTo(
            CustomerMeResponse.Profile(country = "DE", phoneMask = null, email = null, embossName = null),
        )
    }
}