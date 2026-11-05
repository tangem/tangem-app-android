package com.tangem.domain.wallets.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.card.BackupValidator
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReportSeenWalletCardUseCaseTest {

    private val walletCardsBackupRepository: WalletCardsBackupRepository = mockk()

    private val useCase = ReportSeenWalletCardUseCase(
        walletCardsBackupRepository = walletCardsBackupRepository,
        isWalletBackupProblematicUseCase = IsWalletBackupProblematicUseCase(BackupValidator()),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletCardsBackupRepository)
        coEvery { walletCardsBackupRepository.saveSeenWalletCard(any(), any(), any()) } returns Unit.right()
        coEvery { walletCardsBackupRepository.hasCardWithIncompleteBackup(any()) } returns false
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN seen card WHEN invoke THEN it is reported only when the scenario requires`(model: ReportModel) =
        runTest {
            // Arrange
            coEvery {
                walletCardsBackupRepository.hasCardWithIncompleteBackup(WALLET_ID)
            } returns model.hasSeenIncompleteCard

            // Act
            useCase(
                userWallet = coldWallet(backupStatus = model.backupStatus, hasBackupError = model.hasBackupError),
                isNewWallet = model.isNewWallet,
            )

            // Assert
            coVerify(exactly = if (model.isReported) 1 else 0) {
                walletCardsBackupRepository.saveSeenWalletCard(any(), any(), any())
            }
        }

    @Test
    fun `GIVEN added wallet WHEN invoke THEN card is reported without role with its status and curves`() = runTest {
        // Act
        useCase(
            userWallet = coldWallet(backupStatus = CardDTO.BackupStatus.Active(cardCount = 1), isImported = true),
            isNewWallet = true,
        )

        // Assert
        coVerify(exactly = 1) {
            walletCardsBackupRepository.saveSeenWalletCard(
                userWalletId = WALLET_ID,
                card = WalletCardBackup(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY_HEX,
                    role = null,
                    backupStatus = CardBackupStatus.ACTIVE,
                    curves = listOf(EllipticCurve.Secp256k1),
                ),
                usedSeed = true,
            )
        }
    }

    @Test
    fun `GIVEN card that cannot be backed up WHEN wallet added THEN nothing is reported`() = runTest {
        // Act
        val actual = useCase(
            userWallet = coldWallet(backupStatus = CardDTO.BackupStatus.NoBackup, isBackupAllowed = false),
            isNewWallet = true,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { walletCardsBackupRepository.saveSeenWalletCard(any(), any(), any()) }
    }

    private fun provideTestModels() = listOf(
        // a wallet being added is always reported (QA rows 2/7/8)
        ReportModel(isNewWallet = true, backupStatus = ACTIVE, isReported = true),
        ReportModel(isNewWallet = true, backupStatus = CARD_LINKED, isReported = true),
        ReportModel(isNewWallet = true, backupStatus = CardDTO.BackupStatus.NoBackup, isReported = true),
        ReportModel(isNewWallet = true, backupStatus = null, isReported = false),
        // saved wallet with a backup problem on the seen card (QA rows 2 step 4, 3, 4)
        ReportModel(isNewWallet = false, backupStatus = CARD_LINKED, isReported = true),
        ReportModel(isNewWallet = false, backupStatus = ACTIVE, hasBackupError = true, isReported = true),
        // saved wallet whose other card was seen with an incomplete backup (QA row 1 step 5)
        ReportModel(isNewWallet = false, backupStatus = ACTIVE, hasSeenIncompleteCard = true, isReported = true),
        // healthy saved wallet (QA rows 5/6)
        ReportModel(isNewWallet = false, backupStatus = ACTIVE, isReported = false),
        ReportModel(isNewWallet = false, backupStatus = CardDTO.BackupStatus.NoBackup, isReported = false),
        ReportModel(
            isNewWallet = false,
            backupStatus = CardDTO.BackupStatus.NoBackup,
            hasBackupError = true,
            isReported = false,
        ),
    )

    private fun coldWallet(
        backupStatus: CardDTO.BackupStatus?,
        hasBackupError: Boolean = false,
        isImported: Boolean = false,
        isBackupAllowed: Boolean = true,
    ): UserWallet.Cold {
        val card = mockk<CardDTO> {
            every { cardId } returns CARD_ID
            every { cardPublicKey } returns CARD_PUBLIC_KEY
            every { this@mockk.backupStatus } returns backupStatus
            every { settings.isBackupAllowed } returns isBackupAllowed
            every { wallets } returns listOf(mockk { every { curve } returns EllipticCurve.Secp256k1 })
        }

        return mockk {
            every { walletId } returns WALLET_ID
            every { this@mockk.isImported } returns isImported
            every { this@mockk.hasBackupError } returns hasBackupError
            every { scanResponse } returns mockk { every { this@mockk.card } returns card }
        }
    }

    data class ReportModel(
        val isNewWallet: Boolean,
        val backupStatus: CardDTO.BackupStatus?,
        val isReported: Boolean,
        val hasBackupError: Boolean = false,
        val hasSeenIncompleteCard: Boolean = false,
    )

    private companion object {

        val WALLET_ID = UserWalletId(stringValue = "0102030405060708")
        const val CARD_ID = "AC01000000000000"
        val CARD_PUBLIC_KEY = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        const val CARD_PUBLIC_KEY_HEX = "AABB"
        val ACTIVE = CardDTO.BackupStatus.Active(cardCount = 1)
        val CARD_LINKED = CardDTO.BackupStatus.CardLinked(cardCount = 1)
    }
}