package com.tangem.data.polymarket.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Pins the on-disk shape of the cached prediction account status.
 *
 * The names asserted here are the file format, not an implementation detail: every one of them is written into
 * the user's DataStore file. If a name changes, every previously written file stops decoding — and the store's
 * serializer reports that as file corruption, which wipes the cache of every wallet at once. So the test spells
 * the literals out instead of comparing the model to itself.
 */
internal class PredictionAccountStatusValueDTOSerializationTest {

    private val json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" }

    private val serializer = MapSerializer(
        keySerializer = String.serializer(),
        valueSerializer = PredictionAccountStatusValueDTO.serializer(),
    )

    @Test
    fun `GIVEN every stored state WHEN serialized and deserialized THEN the value is preserved`() {
        // Arrange
        val original = mapOf(
            "wallet-active" to PredictionAccountStatusValueDTO.Active(
                balance = BigDecimal("12.50"),
                isTradingAllowed = false,
            ),
            "wallet-onboarding" to PredictionAccountStatusValueDTO.Onboarding(
                stage = PredictionAccountStatusValueDTO.Stage.APPROVING,
            ),
            "wallet-new" to PredictionAccountStatusValueDTO.NotOnboarded,
        )

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, original))

        // Assert
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `GIVEN a stored state WHEN serialized THEN the file carries the pinned names`() {
        // Arrange
        val value = mapOf(
            "wallet-1" to PredictionAccountStatusValueDTO.Active(
                balance = BigDecimal("40"),
                isTradingAllowed = true,
            ),
            "wallet-2" to PredictionAccountStatusValueDTO.Onboarding(
                stage = PredictionAccountStatusValueDTO.Stage.DEPLOYING,
            ),
            "wallet-3" to PredictionAccountStatusValueDTO.NotOnboarded,
        )

        // Act
        val encoded = json.encodeToString(serializer, value)

        // Assert — no fully qualified class name may appear: that would tie the file to the Kotlin package
        assertThat(encoded).contains("\"__type\":\"active\"")
        assertThat(encoded).contains("\"__type\":\"onboarding\"")
        assertThat(encoded).contains("\"__type\":\"not_onboarded\"")
        assertThat(encoded).contains("\"is_trading_allowed\":true")
        assertThat(encoded).contains("\"stage\":\"deploying\"")
        assertThat(encoded).doesNotContain("com.tangem")
    }

    @Test
    fun `GIVEN a balance with a scale WHEN restored THEN it keeps its exact value`() {
        // Arrange — a rounded balance would be a wrong number on the wallet screen, not a formatting detail
        val value = mapOf(
            "wallet-1" to PredictionAccountStatusValueDTO.Active(
                balance = BigDecimal("0.000001"),
                isTradingAllowed = true,
            ),
        )

        // Act
        val restored = json.decodeFromString(serializer, json.encodeToString(serializer, value))

        // Assert
        val balance = (restored.getValue("wallet-1") as PredictionAccountStatusValueDTO.Active).balance
        assertThat(balance).isEqualTo(BigDecimal("0.000001"))
    }

    @Test
    fun `GIVEN a file written by a later version WHEN decoded THEN the unknown key is ignored`() {
        // Arrange — a field added in a newer build must not make the older one wipe the whole cache
        val encoded = """{"wallet-1":{"__type":"active","balance":"40","is_trading_allowed":true,"positions":"7"}}"""

        // Act
        val restored = json.decodeFromString(serializer, encoded)

        // Assert
        assertThat(restored).containsExactly(
            "wallet-1",
            PredictionAccountStatusValueDTO.Active(balance = BigDecimal("40"), isTradingAllowed = true),
        )
    }
}