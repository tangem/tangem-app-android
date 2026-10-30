package com.tangem.domain.wallets.usecase

import arrow.core.left
import arrow.core.right
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ReportCardLinkedWalletsBackupUseCaseTest {

    private val walletCardsBackupRepository: WalletCardsBackupRepository = mockk(relaxUnitFun = true)
    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase = mockk()

    private val useCase = ReportCardLinkedWalletsBackupUseCase(
        walletCardsBackupRepository = walletCardsBackupRepository,
        reportWalletCardsBackupUseCase = reportWalletCardsBackupUseCase,
    )

    @Test
    fun `GIVEN unreported CardLinked wallet WHEN invoke THEN primary card is reported and flag is set`() = runTest {
        // Arrange
        coEvery { walletCardsBackupRepository.isCardLinkedReported(WALLET_ID) } returns false
        coEvery { reportWalletCardsBackupUseCase(any(), any(), any()) } returns Unit.right()

        // Act
        useCase(listOf(coldWallet(isImported = true)))

        // Assert
        coVerify(exactly = 1) {
            reportWalletCardsBackupUseCase(
                userWalletId = WALLET_ID,
                cards = listOf(
                    WalletCardBackup(
                        cardId = CARD_ID,
                        cardPublicKey = CARD_PUBLIC_KEY_HEX,
                        role = WalletCardBackup.Role.PRIMARY,
                        backupStatus = CardBackupStatus.CARD_LINKED,
                        curves = listOf(EllipticCurve.Secp256k1),
                    ),
                ),
                usedSeed = true,
            )
        }
        coVerify(exactly = 1) { walletCardsBackupRepository.setCardLinkedReported(WALLET_ID) }
    }

    @Test
    fun `GIVEN CardLinked wallet already reported WHEN invoke THEN nothing is reported`() = runTest {
        // Arrange
        coEvery { walletCardsBackupRepository.isCardLinkedReported(WALLET_ID) } returns true

        // Act
        useCase(listOf(coldWallet()))

        // Assert
        coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN wallets without a CardLinked card WHEN invoke THEN nothing is reported`() = runTest {
        // Arrange
        val wallets = listOf(
            coldWallet(backupStatus = CardDTO.BackupStatus.Active(cardCount = 2)),
            coldWallet(backupStatus = CardDTO.BackupStatus.NoBackup),
            coldWallet(backupStatus = null),
            mockk<UserWallet.Hot>(),
        )

        // Act
        useCase(wallets)

        // Assert
        coVerify(exactly = 0) { walletCardsBackupRepository.isCardLinkedReported(any()) }
        coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN locked CardLinked wallet WHEN invoke THEN it is reported only after unlock`() = runTest {
        // Arrange
        coEvery { walletCardsBackupRepository.isCardLinkedReported(WALLET_ID) } returns false
        coEvery { reportWalletCardsBackupUseCase(any(), any(), any()) } returns Unit.right()

        // Act
        useCase(listOf(coldWallet(isLocked = true)))
        useCase(listOf(coldWallet()))

        // Assert
        coVerify(exactly = 1) { reportWalletCardsBackupUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN report not delivered WHEN invoke twice THEN flag is not set and report is queued once`() = runTest {
        // Arrange
        coEvery { walletCardsBackupRepository.isCardLinkedReported(WALLET_ID) } returns false
        coEvery {
            reportWalletCardsBackupUseCase(any(), any(), any())
        } returns WalletCardsBackupError.NoInternetConnection.left()

        // Act
        useCase(listOf(coldWallet()))
        useCase(listOf(coldWallet()))

        // Assert
        coVerify(exactly = 1) { reportWalletCardsBackupUseCase(any(), any(), any()) }
        coVerify(exactly = 0) { walletCardsBackupRepository.setCardLinkedReported(any()) }
    }

    private fun coldWallet(
        backupStatus: CardDTO.BackupStatus? = CardDTO.BackupStatus.CardLinked(cardCount = 2),
        isLocked: Boolean = false,
        isImported: Boolean = false,
    ): UserWallet.Cold {
        val card = mockk<CardDTO> {
            every { cardId } returns CARD_ID
            every { cardPublicKey } returns CARD_PUBLIC_KEY
            every { this@mockk.backupStatus } returns backupStatus
            every { wallets } returns listOf(mockk { every { curve } returns EllipticCurve.Secp256k1 })
        }

        return mockk {
            every { walletId } returns WALLET_ID
            every { this@mockk.isLocked } returns isLocked
            every { this@mockk.isImported } returns isImported
            every { scanResponse } returns mockk { every { this@mockk.card } returns card }
        }
    }

    private companion object {

        val WALLET_ID = UserWalletId(stringValue = "0102030405060708")
        const val CARD_ID = "AC01000000000000"
        val CARD_PUBLIC_KEY = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        const val CARD_PUBLIC_KEY_HEX = "AABB"
    }
}