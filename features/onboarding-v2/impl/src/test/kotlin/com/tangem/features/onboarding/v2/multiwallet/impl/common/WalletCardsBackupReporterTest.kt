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
import org.junit.jupiter.api.Test
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

    @Test
    fun `GIVEN toggle enabled WHEN report wallet created THEN primary card reported as not backed up`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

        // Act
        reporter.reportWalletCreated(scanResponse = scanResponse(), usedSeed = false)

        // Assert
        coVerify(exactly = 1) {
            reportWalletCardsBackupUseCase(
                userWalletId = UserWalletIdBuilder.walletPublicKey(WALLET_PUBLIC_KEY),
                cards = listOf(
                    WalletCardBackup(
                        cardId = CARD_ID,
                        cardPublicKey = CARD_PUBLIC_KEY.toHexString(),
                        role = WalletCardBackup.Role.PRIMARY,
                        backupStatus = CardBackupStatus.NO_BACKUP,
                        curves = listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519Slip0010),
                        error = null,
                    ),
                ),
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

    private fun createdWallets(): List<CardDTO.Wallet> = listOf(
        mockk {
            every { publicKey } returns WALLET_PUBLIC_KEY
            every { curve } returns EllipticCurve.Secp256k1
        },
        mockk {
            every { publicKey } returns byteArrayOf(0x0A, 0x0B)
            every { curve } returns EllipticCurve.Ed25519Slip0010
        },
    )

    private companion object {
        const val CARD_ID = "AC05000000000001"
        val CARD_PUBLIC_KEY = byteArrayOf(0x01, 0x02, 0x03)
        val WALLET_PUBLIC_KEY = byteArrayOf(0x04, 0x05, 0x06)
    }
}