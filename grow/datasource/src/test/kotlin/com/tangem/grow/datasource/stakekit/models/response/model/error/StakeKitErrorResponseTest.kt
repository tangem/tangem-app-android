package com.tangem.grow.datasource.stakekit.models.response.model.error

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class StakeKitErrorResponseTest {

    private val adapter = Moshi.Builder().build().adapter(StakeKitErrorResponse::class.java)

    @Test
    fun `GIVEN numeric details amount WHEN decoded THEN amount is read as string`() {
        // Act
        val actual = adapter.fromJson("""{"code":400,"details":{"amount":0.5}}""")!!

        // Assert
        assertThat(actual.details!!.amount).isEqualTo("0.5")
    }
}