package com.tangem.datasource.api.tangemTech.models.account

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * The account type is closed: the two values this build knows parse, an absent field means a crypto account, and a
 * value the backend adds later fails the parse instead of being taken for one of the known kinds.
 */
internal class WalletAccountDTOTypeTest {

    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(WalletAccountDTO::class.java)

    @Test
    fun `GIVEN joint type WHEN parse THEN type is joint`() {
        // Act
        val actual = adapter.fromJson(account(type = "\"joint\""))

        // Assert
        assertThat(actual?.type).isEqualTo(WalletAccountDTO.Type.JOINT)
    }

    @Test
    fun `GIVEN no type WHEN parse THEN type is null`() {
        // Act
        val actual = adapter.fromJson(account(type = null))

        // Assert
        assertThat(actual?.type).isNull()
    }

    @Test
    fun `GIVEN a type this build does not know WHEN parse THEN parsing fails`() {
        // Act
        val actual = runCatching { adapter.fromJson(account(type = "\"shared_v2\"")) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(JsonDataException::class.java)
    }

    @Test
    fun `GIVEN joint type WHEN write THEN the wire value is joint`() {
        // Arrange
        val dto = adapter.fromJson(account(type = "\"joint\""))!!

        // Act
        val actual = adapter.toJson(dto)

        // Assert
        assertThat(actual).contains("\"type\":\"joint\"")
    }

    @Language("JSON")
    private fun account(type: String?): String {
        val typeField = type?.let { ",\"type\":$it" }.orEmpty()

        return """{"id":"A","name":null,"derivation":0,"icon":"Wallet","iconColor":"DullLavender"$typeField}"""
    }
}