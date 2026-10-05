package com.tangem.data.tokens.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.Test

/**
 * Guards the stored format of the viewed token receive warnings: files written by the Moshi serializer must read back
 * unchanged after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer still used
 * while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class TokenReceiveWarningStoredFormatTest {

    @Test
    fun `GIVEN keys WHEN encoded by Moshi THEN matches the stored format`() {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<String>>().toJson(KEYS)

        // Assert
        assertThat(actual).isEqualTo(JSON)
    }

    @Test
    fun `GIVEN stored format WHEN decoded by Moshi THEN keys are kept`() {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<String>>().fromJson(JSON)

        // Assert
        assertThat(actual).containsExactlyElementsIn(KEYS).inOrder()
    }

    @Test
    fun `GIVEN stored format WHEN read from DataStore THEN keys are kept`() {
        // Act
        val actual = readStoredJson(json = JSON, defaultValue = emptySet<String>())

        // Assert
        assertThat(actual).containsExactlyElementsIn(KEYS).inOrder()
    }

    @Test
    fun `GIVEN keys WHEN written to DataStore THEN keeps the stored format`() {
        // Act
        val actual = writeStoredJson(value = KEYS, defaultValue = emptySet())

        // Assert
        assertThat(actual).isEqualTo(JSON)
    }

    private companion object {
        val KEYS = setOf("bitcoin", "ethereum#0xtoken")
        const val JSON = """["bitcoin","ethereum#0xtoken"]"""
    }
}