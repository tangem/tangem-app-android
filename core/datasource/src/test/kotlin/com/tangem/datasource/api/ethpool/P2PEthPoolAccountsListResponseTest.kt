package com.tangem.datasource.api.ethpool

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Types
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountsListResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class P2PEthPoolAccountsListResponseTest {

    private val adapter = MoshiConverter.networkMoshi.adapter<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>>(
        Types.newParameterizedType(
            P2PEthPoolResponse::class.java,
            P2PEthPoolAccountsListResponse::class.java,
        ),
    )

    @Test
    fun `decode batch payload with valid account and per-address error`() {
        val response = requireNotNull(adapter.fromJson(SAMPLE_JSON))

        val list = requireNotNull(response.result).list
        assertThat(list).hasSize(2)

        val good = list.first { it.account != null }
        val account = requireNotNull(good.account)
        assertThat(account.stake.assets.compareTo(BigDecimal("1.2345"))).isEqualTo(0)
        assertThat(account.availableToWithdraw).isGreaterThan(BigDecimal(15049))
        assertThat(account.exitQueue.requests).isEmpty()

        val bad = list.first { it.account == null }
        assertThat(requireNotNull(bad.error).code).isEqualTo(127108)
    }

    private companion object {
        private val SAMPLE_JSON = """
            {
              "error": null,
              "result": {
                "list": [
                  {
                    "delegatorAddress": "0x008d3cd3e349Cd3D5F7c287b3BaF9e4f3E4ba99b",
                    "account": {
                      "delegatorAddress": "0x008d3cd3e349Cd3D5F7c287b3BaF9e4f3E4ba99b",
                      "vaultAddress": "0x4c09BC47db288F998b33CD63BCc1b6ddCCe13F33",
                      "stake": { "assets": "1.234500000000000000", "totalEarnedAssets": 0.0191 },
                      "availableToUnstake": "0.000000000000000005",
                      "availableToWithdraw": 15049.547647281135,
                      "exitQueue": { "total": 0, "requests": [] }
                    },
                    "error": null
                  },
                  {
                    "delegatorAddress": "0xBADADDRESS",
                    "account": null,
                    "error": {
                      "code": 127108,
                      "message": "The provided delegator address is invalid or not properly formatted."
                    }
                  }
                ]
              }
            }
        """.trimIndent()
    }
}