package com.tangem.data.account.repository

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the expanded accounts: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer still used while the
 * kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountsExpandedStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN states WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, Set<AccountsExpandedDTO>>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN states are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, Set<AccountsExpandedDTO>>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN states are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyMap<String, Set<AccountsExpandedDTO>>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN states WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf(
                "wallet" to setOf(
                    AccountsExpandedDTO(accountId = "first", isExpanded = true),
                    AccountsExpandedDTO(accountId = "second", isExpanded = false),
                ),
            ),
            json = """{"wallet":[{"accountId":"first","isExpanded":true},{"accountId":"second","isExpanded":false}]}""",
        ),
        FormatModel(value = mapOf("wallet" to emptySet()), json = """{"wallet":[]}"""),
    )

    data class FormatModel(val value: Map<String, Set<AccountsExpandedDTO>>, val json: String)
}