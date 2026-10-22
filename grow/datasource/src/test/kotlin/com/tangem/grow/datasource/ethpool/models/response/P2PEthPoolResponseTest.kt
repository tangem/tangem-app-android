package com.tangem.grow.datasource.ethpool.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.grow.datasource.stakekit.addStakeKitEnumFallbackAdapters
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import org.junit.jupiter.api.TestInstance

internal class P2PEthPoolResponseTest {

    private val vaultsType = Types.newParameterizedType(
        P2PEthPoolResponse::class.java,
        P2PEthPoolVaultsResponse::class.java,
    )
    private val vaultsAdapter = legacyDataStoreMoshi.adapter<P2PEthPoolResponse<P2PEthPoolVaultsResponse>>(vaultsType)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Envelope {

        @Test
        fun `GIVEN success envelope WHEN decoded THEN result is read and error is null`() {
            // Arrange
            val json = """{"error":null,"result":{"network":"mainnet","vaults":[$VAULT_JSON]}}"""

            // Act
            val actual = vaultsAdapter.fromJson(json)

            // Assert
            val expected = P2PEthPoolResponse(
                error = null,
                result = P2PEthPoolVaultsResponse(
                    network = P2PEthPoolNetworkDTO.MAINNET,
                    vaults = listOf(expectedVault()),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN error without code WHEN decoded THEN throws JsonDataException`() {
            // Act
            val error = runCatching { vaultsAdapter.fromJson("""{"error":{"message":"x"},"result":null}""") }
                .exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Network {

        @Test
        fun `GIVEN hoodi network WHEN decoded THEN maps to HOODI`() {
            // Act
            val actual = vaultsAdapter.fromJson("""{"error":null,"result":{"network":"hoodi","vaults":[]}}""")

            // Assert
            assertThat(actual!!.result!!.network).isEqualTo(P2PEthPoolNetworkDTO.HOODI)
        }

        // Pins current behaviour: no unknown fallback, an unknown network drops the whole vaults response
        @Test
        fun `GIVEN unknown network WHEN decoded THEN whole response fails`() {
            // Arrange
            val json = """{"error":null,"result":{"network":"holesky","vaults":[$VAULT_JSON]}}"""

            // Act
            val error = runCatching { vaultsAdapter.fromJson(json) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
            assertThat(error).hasMessageThat()
                .isEqualTo("Expected one of [mainnet, hoodi] but was holesky at path $.result.network")
        }

        // Pins current behaviour: no unknown fallback, an unknown network drops the whole vaults response
        @Test
        fun `GIVEN stakekit fallback adapters WHEN unknown p2p network decoded THEN still fails`() {
            // Arrange
            val moshi: Moshi = legacyDataStoreMoshi.newBuilder().addStakeKitEnumFallbackAdapters().build()
            val adapter = moshi.adapter<P2PEthPoolResponse<P2PEthPoolVaultsResponse>>(vaultsType)
            val json = """{"error":null,"result":{"network":"holesky","vaults":[$VAULT_JSON]}}"""

            // Act
            val error = runCatching { adapter.fromJson(json) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
        }
    }

    private fun expectedVault() = P2PEthPoolVaultDTO(
        vaultAddress = "0xvault1",
        displayName = "Vault 1",
        apy = BigDecimal("3.5"),
        baseApy = BigDecimal("3.0"),
        capacity = BigDecimal("1000"),
        totalAssets = BigDecimal("100"),
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = true,
        isSmoothingPool = false,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 1700000000L,
    )

    private companion object {
        const val VAULT_JSON = """{"vaultAddress":"0xvault1","displayName":"Vault 1","apy":3.5,"baseApy":3.0,""" +
            """"capacity":"1000","totalAssets":"100","feePercent":"10","isPrivate":false,"isGenesis":true,""" +
            """"isSmoothingPool":false,"isErc20":false,"tokenName":null,"tokenSymbol":null,"createdAt":1700000000}"""
    }
}