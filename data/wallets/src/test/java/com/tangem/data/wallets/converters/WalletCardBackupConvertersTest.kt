package com.tangem.data.wallets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.domain.wallets.models.backup.CardBackupError
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletCardBackupConvertersTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToDTO {

        @Test
        fun `GIVEN card with error WHEN convert THEN all fields are mapped to the wire model`() {
            // Act
            val actual = WalletCardDTOConverter.convert(
                WalletCardBackup(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY,
                    role = WalletCardBackup.Role.BACKUP_2,
                    backupStatus = CardBackupStatus.CARD_LINKED,
                    curves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519),
                    error = CardBackupError(code = "40001", message = "Backup failed"),
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(
                WalletCardDTO(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY,
                    role = WalletCardDTO.Role.BACKUP_2,
                    backupStatus = WalletCardDTO.BackupStatus.CARD_LINKED,
                    curves = listOf("secp256k1", "ed25519"),
                    errorCode = "40001",
                    errorMessage = "Backup failed",
                ),
            )
        }

        @Test
        fun `GIVEN card without error WHEN convert THEN error fields are null`() {
            // Act
            val actual = WalletCardDTOConverter.convert(domainCard(CardBackupStatus.ACTIVE))

            // Assert
            assertThat(actual.errorCode).isNull()
            assertThat(actual.errorMessage).isNull()
        }

        @ParameterizedTest(name = "{0}")
        @ProvideTestModels
        fun `GIVEN domain backup status WHEN convert THEN dto status is mapped`(model: StatusModel) {
            // Act
            val actual = WalletCardDTOConverter.convert(domainCard(model.domain))

            // Assert
            assertThat(actual.backupStatus).isEqualTo(model.dto)
        }

        private fun provideTestModels() = STATUS_MODELS
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToDomain {

        @Test
        fun `GIVEN backend card WHEN convert THEN all fields are mapped to the domain model`() {
            // Act
            val actual = WalletCardBackupConverter.convert(
                WalletCardDTO(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY,
                    role = WalletCardDTO.Role.PRIMARY,
                    backupStatus = WalletCardDTO.BackupStatus.ACTIVE,
                    curves = listOf("secp256k1", "ed25519"),
                    errorCode = "40001",
                    errorMessage = "Backup failed",
                ),
            )

            // Assert
            assertThat(actual).isEqualTo(
                WalletCardBackup(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY,
                    role = WalletCardBackup.Role.PRIMARY,
                    backupStatus = CardBackupStatus.ACTIVE,
                    curves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519),
                    error = CardBackupError(code = "40001", message = "Backup failed"),
                ),
            )
        }

        @Test
        fun `GIVEN unknown curve WHEN convert THEN it is dropped and the known ones are kept`() {
            // Act
            val actual = WalletCardBackupConverter.convert(
                dtoCard(WalletCardDTO.BackupStatus.ACTIVE).copy(curves = listOf("secp256k1", "curve25519")),
            )

            // Assert
            assertThat(actual.curves).containsExactly(EllipticCurve.Secp256k1)
        }

        @Test
        fun `GIVEN no error fields WHEN convert THEN error is null`() {
            // Act
            val actual = WalletCardBackupConverter.convert(dtoCard(WalletCardDTO.BackupStatus.NO_BACKUP))

            // Assert
            assertThat(actual.error).isNull()
        }

        @Test
        fun `GIVEN only an error message WHEN convert THEN error is present`() {
            // Act
            val actual = WalletCardBackupConverter.convert(
                dtoCard(WalletCardDTO.BackupStatus.NO_BACKUP).copy(errorMessage = "Backup failed"),
            )

            // Assert
            assertThat(actual.error).isEqualTo(CardBackupError(code = null, message = "Backup failed"))
        }

        @ParameterizedTest(name = "{0}")
        @ProvideTestModels
        fun convert(model: StatusModel) {
            // Act
            val actual = WalletCardBackupConverter.convert(dtoCard(model.dto))

            // Assert
            assertThat(actual.backupStatus).isEqualTo(model.domain)
        }

        private fun provideTestModels() = STATUS_MODELS
    }

    internal data class StatusModel(val domain: CardBackupStatus, val dto: WalletCardDTO.BackupStatus) {

        override fun toString() = "$domain <-> $dto"
    }

    private companion object {

        const val CARD_ID = "AC01000000000000"
        const val CARD_PUBLIC_KEY = "0AFF"

        val STATUS_MODELS = listOf(
            StatusModel(CardBackupStatus.NO_BACKUP, WalletCardDTO.BackupStatus.NO_BACKUP),
            StatusModel(CardBackupStatus.CARD_LINKED, WalletCardDTO.BackupStatus.CARD_LINKED),
            StatusModel(CardBackupStatus.ACTIVE, WalletCardDTO.BackupStatus.ACTIVE),
        )

        fun domainCard(backupStatus: CardBackupStatus) = WalletCardBackup(
            cardId = CARD_ID,
            cardPublicKey = CARD_PUBLIC_KEY,
            role = WalletCardBackup.Role.PRIMARY,
            backupStatus = backupStatus,
            curves = emptyList(),
        )

        fun dtoCard(backupStatus: WalletCardDTO.BackupStatus) = WalletCardDTO(
            cardId = CARD_ID,
            cardPublicKey = CARD_PUBLIC_KEY,
            role = WalletCardDTO.Role.PRIMARY,
            backupStatus = backupStatus,
            curves = emptyList(),
        )
    }
}