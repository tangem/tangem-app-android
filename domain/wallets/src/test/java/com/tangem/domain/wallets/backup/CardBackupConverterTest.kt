package com.tangem.domain.wallets.backup

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.backup.CardBackupError
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CardBackupConverterTest {

    @ParameterizedTest(name = "{0}")
    @ProvideTestModels
    fun `GIVEN card backup status WHEN convert THEN status is mapped`(model: StatusModel) {
        // Act
        val actual = CardBackupConverter.convert(
            card = card(backupStatus = model.cardStatus),
            role = WalletCardBackup.Role.PRIMARY,
        )

        // Assert
        assertThat(actual.backupStatus).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN scanned card WHEN convert THEN public key is hex encoded and curves are taken from wallets`() {
        // Act
        val actual = CardBackupConverter.convert(
            card = card(backupStatus = CardDTO.BackupStatus.Active(cardCount = 2)),
            role = WalletCardBackup.Role.BACKUP_1,
            error = CardBackupError(code = "40001", message = "Backup failed"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            WalletCardBackup(
                cardId = CARD_ID,
                cardPublicKey = "0AFF",
                role = WalletCardBackup.Role.BACKUP_1,
                backupStatus = CardBackupStatus.ACTIVE,
                curves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519),
                error = CardBackupError(code = "40001", message = "Backup failed"),
            ),
        )
    }

    internal data class StatusModel(val cardStatus: CardDTO.BackupStatus?, val expected: CardBackupStatus) {

        override fun toString() = "${cardStatus ?: "null"} -> $expected"
    }

    private fun provideTestModels() = listOf(
        StatusModel(cardStatus = CardDTO.BackupStatus.NoBackup, expected = CardBackupStatus.NO_BACKUP),
        StatusModel(cardStatus = CardDTO.BackupStatus.CardLinked(cardCount = 1), CardBackupStatus.CARD_LINKED),
        StatusModel(cardStatus = CardDTO.BackupStatus.Active(cardCount = 2), expected = CardBackupStatus.ACTIVE),
        StatusModel(cardStatus = null, expected = CardBackupStatus.NO_BACKUP),
    )

    private fun card(backupStatus: CardDTO.BackupStatus?): CardDTO = mockk {
        every { cardId } returns CARD_ID
        every { cardPublicKey } returns byteArrayOf(0x0A, 0xFF.toByte())
        every { this@mockk.backupStatus } returns backupStatus
        every { wallets } returns listOf(
            mockk { every { curve } returns EllipticCurve.Secp256k1 },
            mockk { every { curve } returns EllipticCurve.Ed25519 },
        )
    }

    private companion object {
        const val CARD_ID = "AC01000000000000"
    }
}