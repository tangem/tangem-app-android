package com.tangem.features.onboarding.v2.multiwallet.impl.common

import arrow.core.right
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.usecase.ReportWalletCardsBackupUseCase
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.coroutines.CoroutineContext

internal class WalletCardsBackupReporterTest {

    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase = mockk()
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles = mockk()

    private val appScope = object : AppCoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Unconfined
    }

    private val reporter = WalletCardsBackupReporter(
        reportWalletCardsBackupUseCase = reportWalletCardsBackupUseCase,
        onboardingV2FeatureToggles = onboardingV2FeatureToggles,
        appScope = appScope,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(reportWalletCardsBackupUseCase, onboardingV2FeatureToggles)
        coEvery { reportWalletCardsBackupUseCase(any(), any(), any()) } returns Unit.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReportWalletCreated {

        @Test
        fun `GIVEN toggle enabled WHEN report wallet created THEN primary card reported as not backed up`() =
            runTest {
                // Arrange
                every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

                // Act
                reporter.reportWalletCreated(scanResponse = scanResponse(), usedSeed = false)

                // Assert
                coVerify(exactly = 1) {
                    reportWalletCardsBackupUseCase(
                        userWalletId = UserWalletIdBuilder.walletPublicKey(WALLET_PUBLIC_KEY),
                        cards = listOf(primaryCardBackup()),
                        usedSeed = false,
                    )
                }
            }

        @Test
        fun `GIVEN wallet created from seed phrase WHEN report wallet created THEN used seed reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

            // Act
            reporter.reportWalletCreated(scanResponse = scanResponse(), usedSeed = true)

            // Assert
            coVerify(exactly = 1) {
                reportWalletCardsBackupUseCase(userWalletId = any(), cards = any(), usedSeed = true)
            }
        }

        @Test
        fun `GIVEN toggle disabled WHEN report wallet created THEN nothing reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns false

            // Act
            reporter.reportWalletCreated(scanResponse = scanResponse(), usedSeed = false)

            // Assert
            coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN card without wallets WHEN report wallet created THEN nothing reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

            // Act
            reporter.reportWalletCreated(scanResponse = scanResponse(wallets = emptyList()), usedSeed = false)

            // Assert
            coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReportBackupCardAdded {

        @Test
        fun `GIVEN one backup card added WHEN report THEN primary and backup1 reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

            // Act
            reporter.reportBackupCardAdded(
                scanResponse = scanResponse(),
                backupCards = listOf(backupCard(BACKUP_1_CARD_ID)),
            )

            // Assert
            coVerify(exactly = 1) {
                reportWalletCardsBackupUseCase(
                    userWalletId = UserWalletIdBuilder.walletPublicKey(WALLET_PUBLIC_KEY),
                    cards = listOf(
                        primaryCardBackup(),
                        backupCardBackup(cardId = BACKUP_1_CARD_ID, role = WalletCardBackup.Role.BACKUP_1),
                    ),
                    usedSeed = false,
                )
            }
        }

        @Test
        fun `GIVEN two backup cards added WHEN report THEN primary and both backups reported in order`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

            // Act
            reporter.reportBackupCardAdded(
                scanResponse = scanResponse(),
                backupCards = listOf(backupCard(BACKUP_1_CARD_ID), backupCard(BACKUP_2_CARD_ID)),
            )

            // Assert
            coVerify(exactly = 1) {
                reportWalletCardsBackupUseCase(
                    userWalletId = any(),
                    cards = listOf(
                        primaryCardBackup(),
                        backupCardBackup(cardId = BACKUP_1_CARD_ID, role = WalletCardBackup.Role.BACKUP_1),
                        backupCardBackup(cardId = BACKUP_2_CARD_ID, role = WalletCardBackup.Role.BACKUP_2),
                    ),
                    usedSeed = any(),
                )
            }
        }

        @Test
        fun `GIVEN imported wallet on the primary card WHEN report THEN used seed reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

            // Act
            reporter.reportBackupCardAdded(
                scanResponse = scanResponse(wallets = createdWallets(isImported = true)),
                backupCards = listOf(backupCard(BACKUP_1_CARD_ID)),
            )

            // Assert
            coVerify(exactly = 1) {
                reportWalletCardsBackupUseCase(userWalletId = any(), cards = any(), usedSeed = true)
            }
        }

        @Test
        fun `GIVEN toggle disabled WHEN report backup card added THEN nothing reported`() = runTest {
            // Arrange
            every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns false

            // Act
            reporter.reportBackupCardAdded(
                scanResponse = scanResponse(),
                backupCards = listOf(backupCard(BACKUP_1_CARD_ID)),
            )

            // Assert
            coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
        }
    }

    private fun scanResponse(wallets: List<CardDTO.Wallet> = createdWallets()): ScanResponse {
        val card = mockk<CardDTO> {
            every { cardId } returns CARD_ID
            every { cardPublicKey } returns CARD_PUBLIC_KEY
            every { backupStatus } returns CardDTO.BackupStatus.NoBackup
            every { this@mockk.wallets } returns wallets
        }

        return mockk {
            every { this@mockk.card } returns card
            every { productType } returns ProductType.Wallet2
        }
    }

    private fun createdWallets(isImported: Boolean = false): List<CardDTO.Wallet> = listOf(
        mockk {
            every { publicKey } returns WALLET_PUBLIC_KEY
            every { curve } returns EllipticCurve.Secp256k1
            every { this@mockk.isImported } returns isImported
        },
        mockk {
            every { publicKey } returns byteArrayOf(0x0A, 0x0B)
            every { curve } returns EllipticCurve.Ed25519Slip0010
            every { this@mockk.isImported } returns isImported
        },
    )

    /** A backup card as it is right after being added: no wallets on it yet, so no curves and no backup */
    private fun backupCard(cardId: String): CardDTO = mockk {
        every { this@mockk.cardId } returns cardId
        every { cardPublicKey } returns BACKUP_CARD_PUBLIC_KEY
        every { backupStatus } returns CardDTO.BackupStatus.NoBackup
        every { wallets } returns emptyList()
    }

    private fun primaryCardBackup() = WalletCardBackup(
        cardId = CARD_ID,
        cardPublicKey = CARD_PUBLIC_KEY.toHexString(),
        role = WalletCardBackup.Role.PRIMARY,
        backupStatus = CardBackupStatus.NO_BACKUP,
        curves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519Slip0010),
        error = null,
    )

    private fun backupCardBackup(cardId: String, role: WalletCardBackup.Role) = WalletCardBackup(
        cardId = cardId,
        cardPublicKey = BACKUP_CARD_PUBLIC_KEY.toHexString(),
        role = role,
        backupStatus = CardBackupStatus.NO_BACKUP,
        curves = emptyList(),
        error = null,
    )

    private companion object {
        const val CARD_ID = "AC05000000000001"
        const val BACKUP_1_CARD_ID = "AC05000000000002"
        const val BACKUP_2_CARD_ID = "AC05000000000003"
        val CARD_PUBLIC_KEY = byteArrayOf(0x01, 0x02, 0x03)
        val BACKUP_CARD_PUBLIC_KEY = byteArrayOf(0x07, 0x08, 0x09)
        val WALLET_PUBLIC_KEY = byteArrayOf(0x04, 0x05, 0x06)
    }
}