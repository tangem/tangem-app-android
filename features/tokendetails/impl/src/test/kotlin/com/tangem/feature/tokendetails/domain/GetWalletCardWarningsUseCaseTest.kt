package com.tangem.feature.tokendetails.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.HasSingleWalletSignedHashesUseCase
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetWalletCardWarningsUseCaseTest {

    private val isDemoCardUseCase: IsDemoCardUseCase = mockk()
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase = mockk()
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()
    private val network: Network = mockk(relaxed = true)

    private val useCase = GetWalletCardWarningsUseCase(
        isDemoCardUseCase = isDemoCardUseCase,
        hasSingleWalletSignedHashesUseCase = hasSingleWalletSignedHashesUseCase,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            isDemoCardUseCase,
            hasSingleWalletSignedHashesUseCase,
            isWalletBackupProblematicUseCase,
        )
        mockkStatic(UserWallet.Cold::cardTypesResolver)

        // Default: nothing flagged
        every { isDemoCardUseCase(any()) } returns false
        every { hasSingleWalletSignedHashesUseCase(any(), any()) } returns flowOf(false)
        every { isWalletBackupProblematicUseCase(any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWallet.Cold::cardTypesResolver)
    }

    @Test
    fun `GIVEN multi-currency cold wallet WHEN invoke THEN empty set`() = runTest {
        // Arrange
        val wallet = coldWallet(isMultiCurrency = true)

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN hot wallet WHEN invoke THEN empty set`() = runTest {
        // Arrange
        val wallet = mockk<UserWallet.Hot>(relaxed = true)

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN single-currency cold wallet with clean card WHEN invoke THEN empty set`() = runTest {
        // Arrange
        val wallet = coldWallet(isMultiCurrency = false, resolver = cleanResolver())

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN single-currency dev card WHEN invoke THEN only dev card warning`() = runTest {
        // Arrange
        val resolver = cleanResolver().also { every { it.isReleaseFirmwareType() } returns false }
        val wallet = coldWallet(isMultiCurrency = false, resolver = resolver)

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).containsExactly(WalletCardWarning.DevCard)
    }

    @Test
    fun `GIVEN single-currency release card with every condition WHEN invoke THEN full warning set`() = runTest {
        // Arrange
        val resolver = cleanResolver().also {
            every { it.isReleaseFirmwareType() } returns true
            every { it.isAttestationFailed() } returns true
            every { it.getRemainingSignatures() } returns LOW_SIGNATURES
            every { it.isTestCard() } returns true
        }
        val wallet = coldWallet(isMultiCurrency = false, resolver = resolver)
        every { isWalletBackupProblematicUseCase(any()) } returns true
        every { isDemoCardUseCase(any()) } returns true
        every { hasSingleWalletSignedHashesUseCase(any(), any()) } returns flowOf(true)

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).containsExactly(
            WalletCardWarning.BackupError,
            WalletCardWarning.FailedCardValidation,
            WalletCardWarning.TestnetCard,
            WalletCardWarning.LowSignatures(count = LOW_SIGNATURES),
            WalletCardWarning.DemoCard,
            WalletCardWarning.NumberOfSignedHashesIncorrect,
        )
    }

    @Test
    fun `GIVEN single-currency cold wallet with backup problem WHEN invoke THEN backup error warning`() = runTest {
        // Arrange
        val wallet = coldWallet(isMultiCurrency = false, resolver = cleanResolver())
        every { isWalletBackupProblematicUseCase(any()) } returns true

        // Act
        val result = useCase(userWallet = wallet, network = network).first()

        // Assert
        assertThat(result).containsExactly(WalletCardWarning.BackupError)
    }

    private fun coldWallet(
        isMultiCurrency: Boolean,
        resolver: CardTypesResolver = cleanResolver(),
    ): UserWallet.Cold {
        return mockk<UserWallet.Cold>(relaxed = true) {
            every { this@mockk.isMultiCurrency } returns isMultiCurrency
            every { cardTypesResolver } returns resolver
        }
    }

    private fun cleanResolver(): CardTypesResolver = mockk {
        every { isReleaseFirmwareType() } returns true
        every { isAttestationFailed() } returns false
        every { getRemainingSignatures() } returns null
        every { isTestCard() } returns false
        every { getCardId() } returns "card"
    }

    private companion object {
        const val LOW_SIGNATURES = 5
    }
}