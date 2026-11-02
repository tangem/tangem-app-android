package com.tangem.data.wallets.balance

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.domain.wallets.models.WalletBalanceState
import org.junit.jupiter.api.Test

internal class WalletBalanceStateConverterTest {

    private val adapter: JsonAdapter<Map<String, WalletBalanceStateDTO>> = Moshi.Builder().build().adapter(
        Types.newParameterizedType(Map::class.java, String::class.java, WalletBalanceStateDTO::class.java),
    )

    @Test
    fun `GIVEN stored json WHEN parse and convert THEN domain states restored`() {
        // Act
        val actual = adapter.fromJson(STORED_JSON)?.mapValues { WalletBalanceStateConverter.convert(it.value) }

        // Assert
        assertThat(actual).isEqualTo(DOMAIN_STATES)
    }

    @Test
    fun `GIVEN domain states WHEN convert back and serialize THEN stored json format kept`() {
        // Act
        val actual = adapter.toJson(DOMAIN_STATES.mapValues { WalletBalanceStateConverter.convertBack(it.value) })

        // Assert
        assertThat(actual).isEqualTo(STORED_JSON)
    }

    private companion object {
        const val STORED_JSON = """{"w1":"ToppedUp","w2":"Empty","w3":"Error"}"""

        val DOMAIN_STATES = mapOf(
            "w1" to WalletBalanceState.ToppedUp,
            "w2" to WalletBalanceState.Empty,
            "w3" to WalletBalanceState.Error,
        )
    }
}