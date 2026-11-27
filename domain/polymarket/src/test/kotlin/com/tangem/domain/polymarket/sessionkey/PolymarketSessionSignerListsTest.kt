package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSessionSignerListsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun lists(model: ListsModel) {
        // Act
        val actual = model.signer.lists(model.sessionKey, now = NOW)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideTermsModels")
    fun carriesTermsOf(model: TermsModel) {
        // Act
        val actual = model.signer.carriesTermsOf(model.sessionKey)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ListsModel(case = "CLOB scope with the recorded expiry", signer = signer(), expected = true),
        ListsModel(case = "ALL scope", signer = signer(scopes = listOf("ALL")), expected = false),
        ListsModel(case = "scopes ignore case", signer = signer(scopes = listOf("clob")), expected = true),
        ListsModel(
            case = "CLOB among other scopes",
            signer = signer(scopes = listOf("RELAYER", "CLOB")),
            expected = false,
        ),
        ListsModel(
            case = "an expiry other than the recorded one",
            signer = signer(validUntil = VALID_UNTIL.minusSeconds(3_600)),
            expected = false,
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
            case = "recorded expiry exactly ten minutes away",
            signer = signer(validUntil = NOW + Duration.ofMinutes(10)),
            sessionKey = SESSION_KEY.copy(validUntil = NOW + Duration.ofMinutes(10)),
            expected = false,
        ),
        ListsModel(
            case = "recorded expiry just over ten minutes away",
            signer = signer(validUntil = NOW + Duration.ofMinutes(10).plusSeconds(1)),
            sessionKey = SESSION_KEY.copy(validUntil = NOW + Duration.ofMinutes(10).plusSeconds(1)),
            expected = true,
        ),
    )

    private fun provideTermsModels() = listOf(
        TermsModel(case = "CLOB only", signer = signer(), expected = true),
        TermsModel(case = "lowercase clob", signer = signer(scopes = listOf("clob")), expected = true),
        TermsModel(case = "CLOB repeated", signer = signer(scopes = listOf("CLOB", "clob")), expected = true),
        TermsModel(case = "CLOB and ALL", signer = signer(scopes = listOf("CLOB", "ALL")), expected = false),
        TermsModel(case = "ALL only", signer = signer(scopes = listOf("ALL")), expected = false),
        TermsModel(case = "no scopes", signer = signer(scopes = emptyList()), expected = false),
        TermsModel(
            case = "expiry one second later",
            signer = signer(validUntil = VALID_UNTIL.plusSeconds(1)),
            expected = false,
        ),
        TermsModel(
            case = "expiry one second earlier",
            signer = signer(validUntil = VALID_UNTIL.minusSeconds(1)),
            expected = false,
        ),
        TermsModel(
            case = "same second with other millis",
            signer = signer(validUntil = VALID_UNTIL.plusMillis(999)),
            expected = true,
        ),
        TermsModel(case = "no listed expiry", signer = signer(validUntil = null), expected = false),
        TermsModel(
            case = "no recorded expiry",
            signer = signer(),
            sessionKey = SESSION_KEY.copy(validUntil = null),
            expected = false,
        ),
        TermsModel(
            case = "address in another case",
            signer = signer(address = SESSION_KEY.address.uppercase().replace("0X", "0x")),
            expected = true,
        ),
        TermsModel(case = "another address", signer = signer(address = OTHER_ADDRESS), expected = false),
        TermsModel(
            case = "record scopes are not the reference",
            signer = signer(scopes = listOf("ALL")),
            sessionKey = SESSION_KEY.copy(scopes = listOf("ALL")),
            expected = false,
        ),
    )

    data class ListsModel(
        val case: String,
        val signer: PolymarketSessionSigner,
        val expected: Boolean,
        val sessionKey: PolymarketSessionKey = SESSION_KEY,
    ) {
        override fun toString(): String = case
    }

    data class TermsModel(
        val case: String,
        val signer: PolymarketSessionSigner,
        val expected: Boolean,
        val sessionKey: PolymarketSessionKey = SESSION_KEY,
    ) {
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