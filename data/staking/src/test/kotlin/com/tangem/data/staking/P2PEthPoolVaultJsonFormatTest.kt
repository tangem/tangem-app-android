package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored JSON format of [P2PEthPoolVault] now that it has a generated Moshi adapter. The expected
 * strings are what the reflective `KotlinJsonAdapterFactory` wrote before, so vaults already cached on devices
 * must still read back unchanged. The Moshi instance has no reflective factory on purpose: without the generated
 * adapter it could not serialize the class at all. The kotlinx cases check that the DataStore reads and writes the
 * same format.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolVaultJsonFormatTest {

    private object BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal): String = value.toString()
    }

    private val adapter = Moshi.Builder().add(BigDecimalAdapter).build().adapter(P2PEthPoolVault::class.java)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN fromJson THEN reads the same vault`(model: FormatModel) {
        // Act
        val actual = adapter.fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.vault)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN vault WHEN toJson THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = adapter.toJson(model.vault)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN json in the stored format WHEN read from DataStore THEN reads the same vault`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = "[${model.json}]", defaultValue = emptyList<P2PEthPoolVault>())

        // Assert
        assertThat(actual).containsExactly(model.vault)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN vault WHEN written to DataStore THEN writes the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = listOf(model.vault), defaultValue = emptyList())

        // Assert
        assertThat(actual).isEqualTo("[${model.json}]")
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            vault = VAULT,
            json = """{$COMMON_FIELDS,"tokenName":"Staked ETH","tokenSymbol":"sETH","createdAt":1700000000000}""",
        ),
        FormatModel(
            vault = VAULT.copy(tokenName = null, tokenSymbol = null),
            json = """{$COMMON_FIELDS,"createdAt":1700000000000}""",
        ),
    )

    data class FormatModel(val vault: P2PEthPoolVault, val json: String)

    private companion object {
        val VAULT = P2PEthPoolVault(
            vaultAddress = "0xvault",
            displayName = "P2P Vault",
            apy = BigDecimal("3.25"),
            baseApy = BigDecimal("3.1"),
            capacity = BigDecimal("10000"),
            totalAssets = BigDecimal("4200.5"),
            feePercent = BigDecimal("10"),
            isPrivate = false,
            isGenesis = true,
            isSmoothingPool = false,
            isErc20 = true,
            tokenName = "Staked ETH",
            tokenSymbol = "sETH",
            createdAt = 1_700_000_000_000L,
        )
        const val COMMON_FIELDS = """"vaultAddress":"0xvault","displayName":"P2P Vault","apy":"3.25",""" +
            """"baseApy":"3.1","capacity":"10000","totalAssets":"4200.5","feePercent":"10","isPrivate":false,""" +
            """"isGenesis":true,"isSmoothingPool":false,"isErc20":true"""
    }
}