package com.tangem.data.walletconnect.store

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the WalletConnect sessions: files written by the Moshi serializer must read back
 * unchanged after the stores moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still
 * used while the kotlinx DataStore toggle is off reads them too.
 *
 * kotlinx also writes the derived [UserWalletId.value] bytes, which Moshi never wrote; both libraries skip a field
 * the other one adds or leaves out, so writes are checked by reading them back with Moshi rather than byte for byte.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletConnectSessionsStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sessions WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<WcSessionDTO>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN sessions are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<WcSessionDTO>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN sessions are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptySet<WcSessionDTO>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sessions WHEN written to DataStore THEN Moshi reads them back`(model: FormatModel) {
        // Act
        val written = writeStoredJson(value = model.value, defaultValue = emptySet())
        val actual = legacyDataStoreMoshi.adapter<Set<WcSessionDTO>>().fromJson(written)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @Test
    fun `GIVEN session stored before accounts WHEN read from DataStore THEN main account is assumed`() {
        // Arrange
        val json = """[{"topic":"topic","walletId":{"stringValue":"$WALLET_ID"},"url":"https://dapp"}]"""

        // Act
        val actual = readStoredJson(json = json, defaultValue = emptySet<WcSessionDTO>())

        // Assert
        assertThat(actual).containsExactly(
            WcSessionDTO(topic = "topic", walletId = WALLET, url = "https://dapp"),
        )
        assertThat(actual.single().accountId).isEqualTo(AccountId.forMainCryptoPortfolio(WALLET))
    }

    @Test
    fun `GIVEN pending approval sessions WHEN stored by either library THEN the other reads them`() {
        // Arrange
        val value = setOf(WcPendingApprovalSessionDTO(pairingTopic = "pairing", session = SESSION, expiredTime = 1L))
        val adapter = legacyDataStoreMoshi.adapter<Set<WcPendingApprovalSessionDTO>>()

        // Act
        val readByKotlinx = readStoredJson(
            json = adapter.toJson(value),
            defaultValue = emptySet<WcPendingApprovalSessionDTO>(),
        )
        val readByMoshi = adapter.fromJson(writeStoredJson(value = value, defaultValue = emptySet()))

        // Assert
        assertThat(readByKotlinx).isEqualTo(value)
        assertThat(readByMoshi).isEqualTo(value)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = setOf(SESSION),
            json = """[{"topic":"topic","walletId":{"stringValue":"$WALLET_ID"},""" +
                """"accountId":$ACCOUNT_ID_JSON,"url":"https://dapp",""" +
                """"securityStatus":"SAFE","connectingTime":1700000000000}]""",
        ),
        FormatModel(
            value = setOf(
                SESSION.copy(url = null, connectingTime = null, securityStatus = CheckDAppResult.FAILED_TO_VERIFY),
            ),
            json = """[{"topic":"topic","walletId":{"stringValue":"$WALLET_ID"},""" +
                """"accountId":$ACCOUNT_ID_JSON,"securityStatus":"FAILED_TO_VERIFY"}]""",
        ),
    )

    data class FormatModel(val value: Set<WcSessionDTO>, val json: String)

    private companion object {
        const val WALLET_ID = "0123456789abcdef"
        const val WALLET_ID_JSON = """{"stringValue":"$WALLET_ID"}"""
        val WALLET = UserWalletId(WALLET_ID)
        val SESSION = WcSessionDTO(
            topic = "topic",
            walletId = WALLET,
            accountId = AccountId.forMainCryptoPortfolio(WALLET),
            url = "https://dapp",
            securityStatus = CheckDAppResult.SAFE,
            connectingTime = 1_700_000_000_000L,
        )
        val ACCOUNT_ID_JSON = """{"value":"${SESSION.accountId.value}","userWalletId":$WALLET_ID_JSON}"""
    }
}