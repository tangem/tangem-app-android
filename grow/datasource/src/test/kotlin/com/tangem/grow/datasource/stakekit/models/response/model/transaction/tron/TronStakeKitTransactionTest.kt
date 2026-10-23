package com.tangem.grow.datasource.stakekit.models.response.model.transaction.tron

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class TronStakeKitTransactionTest {

    private val adapter = Moshi.Builder().build().adapter(TronStakeKitTransaction::class.java)

    @Test
    fun `GIVEN raw_data_hex WHEN decoded THEN raw data hex is read`() {
        // Act
        val actual = adapter.fromJson("""{"raw_data_hex":"0a02c0de"}""")

        // Assert
        assertThat(actual).isEqualTo(TronStakeKitTransaction(rawDataHex = "0a02c0de"))
    }
}