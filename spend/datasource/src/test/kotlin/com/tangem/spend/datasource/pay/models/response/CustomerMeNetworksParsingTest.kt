package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerMeNetworksParsingTest {

    // `Result` already has other BigDecimal fields (via ProductInstance.actualCardLimit), so the
    // codegen'd Result adapter eagerly needs a BigDecimal adapter to be constructed at all,
    // independent of the new `balance.networks` field under test here. The production adapter lives
    // in core:datasource and is internal to it, so the test declares its own equivalent instead of
    // widening that module's API.
    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(CustomerMeResponse.Result::class.java)

    @Test
    fun `GIVEN networks json WHEN parse THEN maps enabled and disabled entries`() {
        val json = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {
                "networks": [
                  {
                    "name": "base", "is_testnet": true, "chain_id": 84532, "status": "ENABLED",
                    "deposit_address": "0xEED",
                    "tokens": [ { "token": "USDC", "token_contract_address": "0x036", "available_for_withdrawal": 6 } ]
                  },
                  {
                    "name": "tron", "is_testnet": false, "chain_id": 728126428, "status": "DISABLED",
                    "deposit_address": null,
                    "tokens": [ { "token": "USDT", "token_contract_address": "TR7", "available_for_withdrawal": null } ]
                  }
                ]
              }
            }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        val networks = result.balance!!.networks
        assertThat(networks).hasSize(2)
        val base = networks!![0]
        assertThat(base.name).isEqualTo("base")
        assertThat(base.isTestnet).isTrue()
        assertThat(base.chainId).isEqualTo(84532L)
        assertThat(base.status).isEqualTo("ENABLED")
        assertThat(base.depositAddress).isEqualTo("0xEED")
        assertThat(base.tokens[0].token).isEqualTo("USDC")
        assertThat(base.tokens[0].availableForWithdrawal).isEqualTo(BigDecimal("6"))
        val tron = networks[1]
        assertThat(tron.depositAddress).isNull()
        assertThat(tron.tokens[0].availableForWithdrawal).isNull()
    }

    @Test
    fun `GIVEN balance json without networks WHEN parse THEN networks is null`() {
        val json = """
            { "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {} }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        assertThat(result.balance!!.networks).isNull()
    }

    @Test
    fun `GIVEN json without balance WHEN parse THEN balance is null`() {
        val json = """
            { "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [] }
        """.trimIndent()

        val result = adapter.fromJson(json)!!

        assertThat(result.balance).isNull()
    }
}