package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolStakeDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the P2P ETH pool balances: files written by the Moshi serializer must read back
 * unchanged after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer still used
 * while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolBalancesStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN balances WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, Set<P2PEthPoolAccountResponse>>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN balances are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, Set<P2PEthPoolAccountResponse>>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN balances are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(
            json = model.json,
            defaultValue = emptyMap<String, Set<P2PEthPoolAccountResponse>>(),
        )

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN balances WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf("wallet" to setOf(ACCOUNT)),
            json = """{"wallet":[{$ACCOUNT_HEAD,"exitQueue":{"total":"0.5","requests":[$REQUEST_JSON]}}]}""",
        ),
        FormatModel(
            value = mapOf("wallet" to setOf(ACCOUNT_WITHOUT_WITHDRAWAL)),
            json = """{"wallet":[{$ACCOUNT_HEAD,"exitQueue":{"total":"0.5",""" +
                """"requests":[$REQUEST_NO_WITHDRAWAL_JSON]}}]}""",
        ),
    )

    data class FormatModel(val value: Map<String, Set<P2PEthPoolAccountResponse>>, val json: String)

    private companion object {
        val REQUEST = P2PEthPoolExitRequestDTO(
            ticket = "ticket",
            totalAssets = BigDecimal("0.00000001"),
            timestamp = 1_700_000_000L,
            withdrawalTimestamp = 1_700_100_000L,
            isClaimable = true,
        )
        val ACCOUNT = P2PEthPoolAccountResponse(
            delegatorAddress = "0xdelegator",
            vaultAddress = "0xvault",
            stake = P2PEthPoolStakeDTO(assets = BigDecimal("32.5"), totalEarnedAssets = BigDecimal("0.125")),
            availableToUnstake = BigDecimal("30"),
            availableToWithdraw = BigDecimal("1.5"),
            exitQueue = P2PEthPoolExitQueueDTO(total = BigDecimal("0.5"), requests = listOf(REQUEST)),
        )
        val ACCOUNT_WITHOUT_WITHDRAWAL = ACCOUNT.copy(
            exitQueue = ACCOUNT.exitQueue.copy(requests = listOf(REQUEST.copy(withdrawalTimestamp = null))),
        )
        const val ACCOUNT_HEAD = """"delegatorAddress":"0xdelegator","vaultAddress":"0xvault",""" +
            """"stake":{"assets":"32.5","totalEarnedAssets":"0.125"},"availableToUnstake":"30",""" +
            """"availableToWithdraw":"1.5""""
        const val REQUEST_JSON = """{"ticket":"ticket","totalAssets":"1E-8","timestamp":1700000000,""" +
            """"withdrawalTimestamp":1700100000,"isClaimable":true}"""
        const val REQUEST_NO_WITHDRAWAL_JSON = """{"ticket":"ticket","totalAssets":"1E-8","timestamp":1700000000,""" +
            """"isClaimable":true}"""
    }
}