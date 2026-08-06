package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.CardBackupStatus
import com.tangem.domain.wallets.models.backup.WalletBackupIntegrity
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
internal class GetWalletBackupIntegrityUseCaseTest {

    private val walletCardsBackupRepository: WalletCardsBackupRepository = mockk()
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()

    private val useCase = GetWalletBackupIntegrityUseCase(
        walletCardsBackupRepository = walletCardsBackupRepository,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletCardsBackupRepository, isWalletBackupProblematicUseCase)
    }

    @ParameterizedTest(name = "{0}")
    @ProvideTestModels
    fun invoke(model: IntegrityModel) = runTest {
        // Arrange
        val wallet = model.wallet()
        every { isWalletBackupProblematicUseCase(wallet) } returns model.isLocallyProblematic
        model.backendResponse?.let { coEvery { walletCardsBackupRepository.getWalletCards(WALLET_ID) } returns it }

        // Act
        val actual = useCase(wallet)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        if (model.backendResponse == null) {
            coVerify(exactly = 0) { walletCardsBackupRepository.getWalletCards(any()) }
        } else {
            coVerify(exactly = 1) { walletCardsBackupRepository.getWalletCards(WALLET_ID) }
        }
    }

    internal data class IntegrityModel(
        val name: String,
        val expected: WalletBackupIntegrity,
        val isCold: Boolean = true,
        val supportsBackup: Boolean = true,
        val isLocallyProblematic: Boolean = false,
        /** `null` means the backend must not be asked at all */
        val backendResponse: Either<WalletCardsBackupError, List<WalletCardBackup>>? = null,
    ) {

        fun wallet(): UserWallet = if (isCold) {
            mockk<UserWallet.Cold> {
                every { walletId } returns WALLET_ID
                every { scanResponse } returns mockk {
                    every { card } returns mockk {
                        every { backupStatus } returns if (supportsBackup) CardDTO.BackupStatus.NoBackup else null
                    }
                }
            }
        } else {
            mockk<UserWallet.Hot>()
        }

        override fun toString() = name
    }

    private fun provideTestModels() = listOf(
        IntegrityModel(
            name = "hot wallet is not applicable",
            isCold = false,
            expected = WalletBackupIntegrity.NotApplicable,
        ),
        IntegrityModel(
            name = "card without backup support is not applicable",
            supportsBackup = false,
            expected = WalletBackupIntegrity.NotApplicable,
        ),
        IntegrityModel(
            name = "local problem wins over the backend",
            isLocallyProblematic = true,
            expected = WalletBackupIntegrity.LocallyDetectedProblem,
        ),
        IntegrityModel(
            name = "unreachable backend is undetermined",
            backendResponse = WalletCardsBackupError.NoInternetConnection.left(),
            expected = WalletBackupIntegrity.Undetermined,
        ),
        IntegrityModel(
            name = "no data on the backend recommends a rescan",
            backendResponse = emptyList<WalletCardBackup>().right(),
            expected = WalletBackupIntegrity.RecommendedRescan,
        ),
        IntegrityModel(
            name = "all cards active is fully activated",
            backendResponse = listOf(
                card(CardBackupStatus.ACTIVE),
                card(CardBackupStatus.ACTIVE),
            ).right(),
            expected = WalletBackupIntegrity.FullyActivated,
        ),
        IntegrityModel(
            name = "one active and one not active requires a rescan",
            backendResponse = listOf(
                card(CardBackupStatus.ACTIVE),
                card(CardBackupStatus.NO_BACKUP),
            ).right(),
            expected = WalletBackupIntegrity.MandatoryRescan,
        ),
        IntegrityModel(
            name = "linked card requires a rescan even without an active one",
            backendResponse = listOf(
                card(CardBackupStatus.CARD_LINKED),
                card(CardBackupStatus.NO_BACKUP),
            ).right(),
            expected = WalletBackupIntegrity.MandatoryRescan,
        ),
        IntegrityModel(
            name = "linked card requires a rescan alongside an active one",
            backendResponse = listOf(
                card(CardBackupStatus.ACTIVE),
                card(CardBackupStatus.CARD_LINKED),
            ).right(),
            expected = WalletBackupIntegrity.MandatoryRescan,
        ),
        IntegrityModel(
            name = "no card ever backed up only recommends a rescan",
            backendResponse = listOf(
                card(CardBackupStatus.NO_BACKUP),
                card(CardBackupStatus.NO_BACKUP),
            ).right(),
            expected = WalletBackupIntegrity.RecommendedRescan,
        ),
        IntegrityModel(
            name = "single card without backup only recommends a rescan",
            backendResponse = listOf(card(CardBackupStatus.NO_BACKUP)).right(),
            expected = WalletBackupIntegrity.RecommendedRescan,
        ),
    )

    private companion object {

        val WALLET_ID = UserWalletId(stringValue = "0102030405060708")

        fun card(backupStatus: CardBackupStatus) = WalletCardBackup(
            cardId = "AC01000000000000",
            cardPublicKey = "AABB",
            role = WalletCardBackup.Role.PRIMARY,
            backupStatus = backupStatus,
            curves = emptyList(),
        )
    }
}