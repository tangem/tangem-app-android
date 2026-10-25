package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Duration
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionSignerListsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun lists(model: ListsModel) {
        // Act
        val actual = model.signer.lists(SESSION_KEY, now = NOW)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ListsModel(case = "CLOB scope with the recorded expiry", signer = signer(), expected = true),
        ListsModel(case = "ALL scope", signer = signer(scopes = listOf("ALL")), expected = true),
        ListsModel(case = "scopes ignore case", signer = signer(scopes = listOf("clob")), expected = true),
        ListsModel(
            case = "CLOB among other scopes",
            signer = signer(scopes = listOf("RELAYER", "CLOB")),
            expected = true,
        ),
        ListsModel(
            case = "an expiry other than the recorded one",
            signer = signer(validUntil = VALID_UNTIL.minusSeconds(3_600)),
            expected = true,
        ),
        ListsModel(
            case = "address in another case",
            signer = signer(address = SESSION_KEY.address.uppercase().replace("0X", "0x")),
            expected = true,
        ),
        ListsModel(case = "another address", signer = signer(address = OTHER_ADDRESS), expected = false),
        ListsModel(case = "no trading scope", signer = signer(scopes = listOf("RELAYER")), expected = false),
        ListsModel(case = "no expiry", signer = signer(validUntil = null), expected = false),
        ListsModel(
            case = "expiry exactly ten minutes away",
            signer = signer(validUntil = NOW + Duration.ofMinutes(10)),
            expected = false,
        ),
        ListsModel(
            case = "expiry just over ten minutes away",
            signer = signer(validUntil = NOW + Duration.ofMinutes(10).plusSeconds(1)),
            expected = true,
        ),
    )

    data class ListsModel(val case: String, val signer: PolymarketSessionSigner, val expected: Boolean) {
        override fun toString(): String = case
    }

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val VALID_UNTIL: Instant = NOW + Duration.ofDays(180)
        const val OTHER_ADDRESS = "0x5e55000000000000000000000000000000000002"

        val SESSION_KEY = PolymarketSessionKey(
            address = "0x5e55000000000000000000000000000000000001",
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = true,
            status = PolymarketSessionKeyStatus.ACTIVE,
            scopes = listOf("CLOB"),
            validUntil = VALID_UNTIL,
            renewAfter = null,
            authorizedAt = NOW,
            createdAt = NOW,
        )

        fun signer(
            address: String = SESSION_KEY.address,
            scopes: List<String> = listOf("CLOB"),
            validUntil: Instant? = VALID_UNTIL,
        ) = PolymarketSessionSigner(address = address, scopes = scopes, validUntil = validUntil)
    }
}