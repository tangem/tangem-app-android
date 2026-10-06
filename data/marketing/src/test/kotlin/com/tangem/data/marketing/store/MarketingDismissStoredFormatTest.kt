package com.tangem.data.marketing.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.Test

/**
 * Guards the stored format of the dismissed banner ids: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still used while
 * the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class MarketingDismissStoredFormatTest {

    @Test
    fun `GIVEN ids WHEN encoded by Moshi THEN matches the stored format`() {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<Int>>().toJson(IDS)

        // Assert
        assertThat(actual).isEqualTo(JSON)
    }

    @Test
    fun `GIVEN stored format WHEN decoded by Moshi THEN ids are kept`() {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Set<Int>>().fromJson(JSON)

        // Assert
        assertThat(actual).containsExactlyElementsIn(IDS).inOrder()
    }

    @Test
    fun `GIVEN stored format WHEN read from DataStore THEN ids are kept`() {
        // Act
        val actual = readStoredJson(json = JSON, defaultValue = emptySet<Int>())

        // Assert
        assertThat(actual).containsExactlyElementsIn(IDS).inOrder()
    }

    @Test
    fun `GIVEN ids WHEN written to DataStore THEN keeps the stored format`() {
        // Act
        val actual = writeStoredJson(value = IDS, defaultValue = emptySet())

        // Assert
        assertThat(actual).isEqualTo(JSON)
    }

    private companion object {
        val IDS = setOf(3, 1)
        const val JSON = "[3,1]"
    }
}