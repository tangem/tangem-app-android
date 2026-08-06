package com.tangem.datasource.api.tangemTech.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class WalletCardDTOTest {

    // The DTO has no custom types (String / enum / List<String>), so the codegen'd adapter needs no extra adapters
    // registered — a bare Moshi behaves exactly like the app's @NetworkMoshi for this class. WalletCardDTO is not
    // annotated @SerializeNulls either, so nulls are omitted on encode here just as they are in production.
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(WalletCardDTO::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    /** Encodes [card] and reads the result back as a map, so assertions don't depend on key order. */
    private fun encode(card: WalletCardDTO): Map<String, Any?> = mapAdapter.fromJson(adapter.toJson(card))!!

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Decode {

        @Test
        fun `GIVEN full card json WHEN parsed THEN every field mapped`() {
            // Arrange
            val json = """
                {
                  "cardId": "AC01000000041225",
                  "cardPublicKey": "0400C7CD",
                  "role": "backup1",
                  "backupStatus": "cardLinked",
                  "curves": ["secp256k1", "ed25519"],
                  "errorCode": "12345",
                  "errorMessage": "Card is not activated"
                }
            """.trimIndent()

            // Act
            val actual = adapter.fromJson(json)

            // Assert
            assertThat(actual).isEqualTo(
                WalletCardDTO(
                    cardId = "AC01000000041225",
                    cardPublicKey = "0400C7CD",
                    role = WalletCardDTO.Role.BACKUP_1,
                    backupStatus = WalletCardDTO.BackupStatus.CARD_LINKED,
                    curves = listOf("secp256k1", "ed25519"),
                    errorCode = "12345",
                    errorMessage = "Card is not activated",
                ),
            )
        }

        @Test
        fun `GIVEN json without error fields WHEN parsed THEN errors are null`() {
            // Arrange — the happy path from the backend: no card command failed, so both error fields are absent
            val json = """
                {
                  "cardId": "AC01",
                  "cardPublicKey": "0400",
                  "role": "primary",
                  "backupStatus": "active",
                  "curves": []
                }
            """.trimIndent()

            // Act
            val actual = adapter.fromJson(json)

            // Assert
            assertThat(actual).isEqualTo(
                WalletCardDTO(
                    cardId = "AC01",
                    cardPublicKey = "0400",
                    role = WalletCardDTO.Role.PRIMARY,
                    backupStatus = WalletCardDTO.BackupStatus.ACTIVE,
                    curves = emptyList(),
                    errorCode = null,
                    errorMessage = null,
                ),
            )
        }

        @Test
        fun `GIVEN json with explicit null errors WHEN parsed THEN errors are null`() {
            // Arrange
            val json = """
                {
                  "cardId": "AC01", "cardPublicKey": "0400", "role": "primary",
                  "backupStatus": "noBackup", "curves": ["secp256k1"],
                  "errorCode": null, "errorMessage": null
                }
            """.trimIndent()

            // Act
            val actual = adapter.fromJson(json)!!

            // Assert
            assertThat(actual.errorCode).isNull()
            assertThat(actual.errorMessage).isNull()
        }

        @Test
        fun `GIVEN json with unknown keys WHEN parsed THEN unknown keys ignored`() {
            // Arrange — the backend may add fields the app doesn't know yet; they must not break parsing
            val json = """
                {
                  "cardId": "AC01", "cardPublicKey": "0400", "role": "primary",
                  "backupStatus": "active", "curves": [], "firmwareVersion": "6.33"
                }
            """.trimIndent()

            // Act
            val actual = adapter.fromJson(json)!!

            // Assert
            assertThat(actual.cardId).isEqualTo("AC01")
        }

        @Test
        fun `GIVEN json with unknown role WHEN parsed THEN fails`() {
            // Arrange
            val json = """
                {
                  "cardId": "AC01", "cardPublicKey": "0400", "role": "backup3",
                  "backupStatus": "active", "curves": []
                }
            """.trimIndent()

            // Act
            val actual = runCatching { adapter.fromJson(json) }.exceptionOrNull()

            // Assert — no fallback is registered for these enums, so an unknown role is a hard parsing error
            assertThat(actual).isInstanceOf(JsonDataException::class.java)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN json without a required field WHEN parsed THEN fails`(model: MissingFieldModel) {
            // Act
            val actual = runCatching { adapter.fromJson(model.json) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(JsonDataException::class.java)
            assertThat(actual).hasMessageThat().contains(model.missingField)
        }

        private fun provideTestModels() = FULL_FIELDS.keys.map(::MissingFieldModel)
    }

    @Nested
    inner class Encode {

        @Test
        fun `GIVEN card with errors WHEN encoded THEN all fields written with wire names`() {
            // Arrange
            val card = WalletCardDTO(
                cardId = "AC01",
                cardPublicKey = "0400",
                role = WalletCardDTO.Role.BACKUP_2,
                backupStatus = WalletCardDTO.BackupStatus.NO_BACKUP,
                curves = listOf("secp256k1"),
                errorCode = "12345",
                errorMessage = "Card is not activated",
            )

            // Act
            val actual = encode(card)

            // Assert
            assertThat(actual).containsExactly(
                "cardId", "AC01",
                "cardPublicKey", "0400",
                "role", "backup2",
                "backupStatus", "noBackup",
                "curves", listOf("secp256k1"),
                "errorCode", "12345",
                "errorMessage", "Card is not activated",
            )
        }

        @Test
        fun `GIVEN card without errors WHEN encoded THEN null error fields omitted`() {
            // Arrange
            val card = WalletCardDTO(
                cardId = "AC01",
                cardPublicKey = "0400",
                role = WalletCardDTO.Role.PRIMARY,
                backupStatus = WalletCardDTO.BackupStatus.ACTIVE,
                curves = emptyList(),
            )

            // Act
            val actual = encode(card)

            // Assert — the DTO is not @SerializeNulls, so absent errors are left out of the request body
            assertThat(actual).containsExactly(
                "cardId", "AC01",
                "cardPublicKey", "0400",
                "role", "primary",
                "backupStatus", "active",
                "curves", emptyList<String>(),
            )
        }

        @Test
        fun `GIVEN card WHEN encoded and parsed back THEN equal to the original`() {
            // Arrange
            val card = WalletCardDTO(
                cardId = "AC01000000041225",
                cardPublicKey = "0400C7CD",
                role = WalletCardDTO.Role.BACKUP_1,
                backupStatus = WalletCardDTO.BackupStatus.CARD_LINKED,
                curves = listOf("secp256k1", "ed25519"),
                errorCode = "12345",
                errorMessage = "Card is not activated",
            )

            // Act
            val actual = adapter.fromJson(adapter.toJson(card))

            // Assert
            assertThat(actual).isEqualTo(card)
        }
    }

    /**
     * The wire names are the contract with the backend, so renaming a constant must not silently change them, and
     * every constant must have one — hence the coverage check alongside the mapping itself.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RoleWireNames {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a role WHEN decoded and encoded THEN matches its wire name`(model: RoleModel) {
            // Act
            val decoded = adapter.fromJson(cardJson(role = model.wireName))!!.role
            val encoded = encode(card(role = model.role))["role"]

            // Assert
            assertThat(decoded).isEqualTo(model.role)
            assertThat(encoded).isEqualTo(model.wireName)
        }

        @Test
        fun `GIVEN the test models WHEN listed THEN every role is covered`() {
            assertThat(provideTestModels().map { it.role })
                .containsExactlyElementsIn(WalletCardDTO.Role.entries)
        }

        private fun provideTestModels() = listOf(
            RoleModel(wireName = "primary", role = WalletCardDTO.Role.PRIMARY),
            RoleModel(wireName = "backup1", role = WalletCardDTO.Role.BACKUP_1),
            RoleModel(wireName = "backup2", role = WalletCardDTO.Role.BACKUP_2),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BackupStatusWireNames {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a backup status WHEN decoded and encoded THEN matches its wire name`(model: BackupStatusModel) {
            // Act
            val decoded = adapter.fromJson(cardJson(backupStatus = model.wireName))!!.backupStatus
            val encoded = encode(card(backupStatus = model.status))["backupStatus"]

            // Assert
            assertThat(decoded).isEqualTo(model.status)
            assertThat(encoded).isEqualTo(model.wireName)
        }

        @Test
        fun `GIVEN the test models WHEN listed THEN every backup status is covered`() {
            assertThat(provideTestModels().map { it.status })
                .containsExactlyElementsIn(WalletCardDTO.BackupStatus.entries)
        }

        private fun provideTestModels() = listOf(
            BackupStatusModel(wireName = "noBackup", status = WalletCardDTO.BackupStatus.NO_BACKUP),
            BackupStatusModel(wireName = "cardLinked", status = WalletCardDTO.BackupStatus.CARD_LINKED),
            BackupStatusModel(wireName = "active", status = WalletCardDTO.BackupStatus.ACTIVE),
        )
    }

    // region fixtures

    data class RoleModel(val wireName: String, val role: WalletCardDTO.Role)

    data class BackupStatusModel(val wireName: String, val status: WalletCardDTO.BackupStatus)

    /** A full card payload with every field but [missingField], to check that the field is required. */
    data class MissingFieldModel(val missingField: String) {

        val json: String = FULL_FIELDS
            .filterKeys { it != missingField }
            .entries
            .joinToString(prefix = "{", postfix = "}") { (key, value) -> """"$key":$value""" }

        override fun toString(): String = "without $missingField"
    }

    private companion object {

        /** Fields without a default in [WalletCardDTO], as raw JSON values. */
        val FULL_FIELDS = mapOf(
            "cardId" to "\"AC01\"",
            "cardPublicKey" to "\"0400\"",
            "role" to "\"primary\"",
            "backupStatus" to "\"active\"",
            "curves" to "[]",
        )

        fun cardJson(role: String = "primary", backupStatus: String = "active"): String = """
            {
              "cardId": "AC01", "cardPublicKey": "0400", "role": "$role",
              "backupStatus": "$backupStatus", "curves": []
            }
        """.trimIndent()

        fun card(
            role: WalletCardDTO.Role = WalletCardDTO.Role.PRIMARY,
            backupStatus: WalletCardDTO.BackupStatus = WalletCardDTO.BackupStatus.ACTIVE,
        ): WalletCardDTO = WalletCardDTO(
            cardId = "AC01",
            cardPublicKey = "0400",
            role = role,
            backupStatus = backupStatus,
            curves = emptyList(),
        )
    }

    // endregion
}