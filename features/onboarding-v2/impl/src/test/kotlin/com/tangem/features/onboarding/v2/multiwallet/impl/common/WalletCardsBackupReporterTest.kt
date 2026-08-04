package com.tangem.features.onboarding.v2.multiwallet.impl.common

import arrow.core.right
import com.tangem.common.card.EllipticCurve
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
import org.junit.jupiter.api.Assertions
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
    fun `GIVEN toggle enabled WHEN report THEN cards are sent for the wallet of the primary card`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true
        val cards = listOf(primaryCard(), backupCard())

        // Act
        reporter.report(scanResponse = scanResponse(), cards = cards, usedSeed = false)

        // Assert
        coVerify(exactly = 1) {
            reportWalletCardsBackupUseCase(
                userWalletId = UserWalletIdBuilder.walletPublicKey(WALLET_PUBLIC_KEY),
                cards = cards,
                usedSeed = false,
            )
        }
    }

    @Test
    fun `GIVEN wallet created from a seed phrase WHEN report THEN used seed is sent`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

        // Act
        reporter.report(scanResponse = scanResponse(), cards = listOf(primaryCard()), usedSeed = true)

        // Assert
        coVerify(exactly = 1) {
            reportWalletCardsBackupUseCase(userWalletId = any(), cards = any(), usedSeed = true)
        }
    }

    @Test
    fun `GIVEN imported wallet on the card WHEN usedSeedPhrase THEN it is true`() {
        // Act
        val actual = scanResponse(isImported = true).usedSeedPhrase()

        // Assert
        Assertions.assertTrue(actual)
    }

    @Test
    fun `GIVEN wallet not imported WHEN usedSeedPhrase THEN it is false`() {
        // Act
        val actual = scanResponse().usedSeedPhrase()

        // Assert
        Assertions.assertFalse(actual)
    }

    @Test
    fun `GIVEN toggle disabled WHEN report THEN nothing is sent`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns false

        // Act
        reporter.report(scanResponse = scanResponse(), cards = listOf(primaryCard()), usedSeed = false)

        // Assert
        coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN card without wallets WHEN report THEN nothing is sent`() = runTest {
        // Arrange
        every { onboardingV2FeatureToggles.isCardLinkedStatusUpdateEnabled } returns true

        // Act
        reporter.report(
            scanResponse = scanResponse(wallets = emptyList()),
            cards = listOf(primaryCard()),
            usedSeed = false,
        )

        // Assert
        coVerify(exactly = 0) { reportWalletCardsBackupUseCase(any(), any(), any()) }
    }

    private fun scanResponse(
        wallets: List<CardDTO.Wallet> = createdWallets(),
        isImported: Boolean = false,
    ): ScanResponse {
        val card = mockk<CardDTO> {
            every { this@mockk.wallets } returns wallets.map { wallet ->
                mockk {
                    every { publicKey } returns wallet.publicKey
                    every { this@mockk.isImported } returns isImported
                }
            }
        }

        return mockk {
            every { this@mockk.card } returns card
            every { productType } returns ProductType.Wallet2
        }
    }

    private fun createdWallets(): List<CardDTO.Wallet> = listOf(
        mockk { every { publicKey } returns WALLET_PUBLIC_KEY },
    )

    private fun primaryCard() = WalletCardBackup(
        cardId = "AC05000000000001",
        cardPublicKey = "010203",
        role = WalletCardBackup.Role.PRIMARY,
        backupStatus = CardBackupStatus.ACTIVE,
        curves = listOf(EllipticCurve.Secp256k1),
    )

    private fun backupCard() = WalletCardBackup(
        cardId = "AC05000000000002",
        cardPublicKey = "040506",
        role = WalletCardBackup.Role.BACKUP_1,
        backupStatus = CardBackupStatus.NO_BACKUP,
        curves = emptyList(),
    )

    private companion object {
        val WALLET_PUBLIC_KEY = byteArrayOf(0x04, 0x05, 0x06)
    }
}