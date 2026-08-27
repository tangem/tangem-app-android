package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReportMissingWalletCardsBackupUseCaseTest {

    private val walletCardsBackupRepository: WalletCardsBackupRepository = mockk()
    private val reportWalletCardsBackupUseCase: ReportWalletCardsBackupUseCase = mockk()
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()

    private val useCase = ReportMissingWalletCardsBackupUseCase(
        walletCardsBackupRepository = walletCardsBackupRepository,
        reportWalletCardsBackupUseCase = reportWalletCardsBackupUseCase,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletCardsBackupRepository, reportWalletCardsBackupUseCase, isWalletBackupProblematicUseCase)
    }

    @ParameterizedTest(name = "{0}")
    @ProvideTestModels
    fun invoke(model: ReportModel) = runTest {
        // Arrange
        val wallet = model.wallet()
        every { isWalletBackupProblematicUseCase(wallet) } returns model.isLocallyProblematic
        model.backendCards?.let { coEvery { walletCardsBackupRepository.getWalletCards(WALLET_ID) } returns it }
        model.reportResult?.let {
            coEvery { reportWalletCardsBackupUseCase.invoke(any(), any(), any()) } returns it
        }

        // Act
        val actual = useCase(wallet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)

        coVerify(exactly = if (model.backendCards == null) 0 else 1) {
            walletCardsBackupRepository.getWalletCards(WALLET_ID)
        }

        if (model.reportResult == null) {
            coVerify(exactly = 0) { reportWalletCardsBackupUseCase.invoke(any(), any(), any()) }
        } else {
            coVerify(exactly = 1) {
                reportWalletCardsBackupUseCase.invoke(
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
                    usedSeed = model.isImported,
                )
            }
        }
    }

    internal data class ReportModel(
        val name: String,
        val expected: Either<WalletCardsBackupError, Unit>,
        val isCold: Boolean = true,
        val supportsBackup: Boolean = true,
        val isLocallyProblematic: Boolean = true,
        val isImported: Boolean = false,
        /** `null` means the backend must not be asked at all */
        val backendCards: Either<WalletCardsBackupError, List<WalletCardBackup>>? = null,
        /** `null` means nothing must be reported */
        val reportResult: Either<WalletCardsBackupError, Unit>? = null,
    ) {

        fun wallet(): UserWallet = if (isCold) {
            mockk<UserWallet.Cold> {
                every { walletId } returns WALLET_ID
                every { isImported } returns this@ReportModel.isImported
                every { scanResponse } returns mockk {
                    every { card } returns card(hasBackupSupport = supportsBackup)
                }
            }
        } else {
            mockk<UserWallet.Hot>()
        }

        override fun toString() = name
    }

    private fun provideTestModels() = listOf(
        ReportModel(
            name = "hot wallet is not reported",
            isCold = false,
            expected = Unit.right(),
        ),
        ReportModel(
            name = "card without backup support is not reported",
            supportsBackup = false,
            expected = Unit.right(),
        ),
        ReportModel(
            name = "wallet without a local problem is not reported",
            isLocallyProblematic = false,
            expected = Unit.right(),
        ),
        ReportModel(
            name = "unreachable backend reports nothing and fails",
            backendCards = WalletCardsBackupError.NoInternetConnection.left(),
            expected = WalletCardsBackupError.NoInternetConnection.left(),
        ),
        ReportModel(
            name = "backend that already knows the wallet is left alone",
            backendCards = listOf(
                WalletCardBackup(
                    cardId = CARD_ID,
                    cardPublicKey = CARD_PUBLIC_KEY_HEX,
                    role = WalletCardBackup.Role.PRIMARY,
                    backupStatus = CardBackupStatus.ACTIVE,
                    curves = emptyList(),
                ),
            ).right(),
            expected = Unit.right(),
        ),
        ReportModel(
            name = "backend without the wallet gets the scanned card",
            backendCards = emptyList<WalletCardBackup>().right(),
            reportResult = Unit.right(),
            expected = Unit.right(),
        ),
        ReportModel(
            name = "imported wallet is reported as created from a seed",
            isImported = true,
            backendCards = emptyList<WalletCardBackup>().right(),
            reportResult = Unit.right(),
            expected = Unit.right(),
        ),
        ReportModel(
            name = "failed report is propagated",
            backendCards = emptyList<WalletCardBackup>().right(),
            reportResult = WalletCardsBackupError.NoInternetConnection.left(),
            expected = WalletCardsBackupError.NoInternetConnection.left(),
        ),
    )

    private companion object {

        val WALLET_ID = UserWalletId(stringValue = "0102030405060708")
        const val CARD_ID = "AC01000000000000"
        val CARD_PUBLIC_KEY = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        const val CARD_PUBLIC_KEY_HEX = "AABB"

        /** A card whose backup is broken — [com.tangem.domain.card.BackupValidator] rejects exactly this status */
        fun card(hasBackupSupport: Boolean): CardDTO = mockk {
            every { cardId } returns CARD_ID
            every { cardPublicKey } returns CARD_PUBLIC_KEY
            every { backupStatus } returns CardDTO.BackupStatus.CardLinked(cardCount = 2).takeIf { hasBackupSupport }
            every { wallets } returns listOf(mockk { every { curve } returns EllipticCurve.Secp256k1 })
        }
    }
}