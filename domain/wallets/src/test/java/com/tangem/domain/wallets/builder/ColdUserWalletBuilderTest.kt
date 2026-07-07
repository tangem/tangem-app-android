package com.tangem.domain.wallets.builder

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlinx.coroutines.test.runTest

/**
 * Tests for [ColdUserWalletBuilder].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ColdUserWalletBuilderTest {

    private val generateWalletNameUseCase: GenerateWalletNameUseCase = mockk()
    private val cardRepository: CardRepository = mockk()

    private val scanResponse = MockScanResponseFactory.create(
        cardConfig = MultiWalletCardConfig,
        derivedKeys = emptyMap(),
    )

    private val primaryCardId = scanResponse.card.cardId
    private val expectedWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

    @BeforeEach
    fun setup() {
        clearMocks(generateWalletNameUseCase, cardRepository)
        every { generateWalletNameUseCase(any(), any(), any()) } returns WALLET_NAME
        coEvery { cardRepository.hasBackupError(any()) } returns false
    }

    @Test
     fun `GIVEN valid scan response WHEN build THEN returns cold wallet with expected fields`() = runTest {
        // Act
        val result = createBuilder().build()

        // Assert
        assertThat(result).isEqualTo(
            UserWallet.Cold(
                name = WALLET_NAME,
                walletId = requireNotNull(expectedWalletId),
                cardsInWallet = setOf(primaryCardId),
                isMultiCurrency = scanResponse.cardTypesResolver.isMultiwalletAllowed(),
                hasBackupError = false,
                scanResponse = scanResponse,
            ),
        )
    }

    @Test
    fun `GIVEN backup card ids WHEN build THEN cards in wallet include primary and backup`() = runTest {
        // Act
        val result = createBuilder()
            .backupCardsIds(setOf("backup-1", "backup-2"))
            .build()

        // Assert
        assertThat(result?.cardsInWallet).containsExactly(primaryCardId, "backup-1", "backup-2")
    }

    @Test
    fun `GIVEN null backup card ids WHEN build THEN cards in wallet contain only primary`() = runTest {
        // Act
        val result = createBuilder()
            .backupCardsIds(null)
            .build()

        // Assert
        assertThat(result?.cardsInWallet).containsExactly(primaryCardId)
    }

    @Test
    fun `GIVEN scan response without wallets WHEN build THEN returns null`() = runTest {
        // Arrange
        val scanResponseWithoutWallets = scanResponse.copy(
            card = scanResponse.card.copy(wallets = emptyList()),
        )

        // Act
        val result = createBuilder(scanResponseWithoutWallets).build()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN scan response WHEN build THEN wallet name generated from resolver data`() = runTest {
        // Act
        createBuilder().build()

        // Assert
        verify(exactly = 1) {
            generateWalletNameUseCase(
                scanResponse.productType,
                scanResponse.card,
                scanResponse.cardTypesResolver.isStart2Coin(),
            )
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun hasBackupError(model: HasBackupErrorModel) = runTest {
        // Arrange
        coEvery { cardRepository.hasBackupError(primaryCardId) } returns model.repositoryReturns

        // Act
        val result = createBuilder()
            .hasBackupError(model.builderFlag)
            .build()

        // Assert
        assertThat(result?.hasBackupError).isEqualTo(model.expected)
        // The externally set flag short-circuits `||`: the repository is queried only when the flag is not set.
        coVerify(exactly = model.expectedRepositoryCalls) { cardRepository.hasBackupError(primaryCardId) }
    }

    private fun createBuilder(scanResponse: ScanResponse = this.scanResponse) = ColdUserWalletBuilder(
        scanResponse = scanResponse,
        generateWalletNameUseCase = generateWalletNameUseCase,
        cardRepository = cardRepository,
    )

    internal data class HasBackupErrorModel(
        val builderFlag: Boolean,
        val repositoryReturns: Boolean,
        val expected: Boolean,
        val expectedRepositoryCalls: Int,
    )

    private fun provideTestModels() = listOf(
        // Flag not set externally -> repository is queried and is the source of truth.
        HasBackupErrorModel(
            builderFlag = false,
            repositoryReturns = false,
            expected = false,
            expectedRepositoryCalls = 1,
        ),
        HasBackupErrorModel(
            builderFlag = false,
            repositoryReturns = true,
            expected = true,
            expectedRepositoryCalls = 1,
        ),
        // Flag set externally -> `||` short-circuits, repository is NOT queried regardless of its value.
        HasBackupErrorModel(
            builderFlag = true,
            repositoryReturns = false,
            expected = true,
            expectedRepositoryCalls = 0,
        ),
        HasBackupErrorModel(builderFlag = true, repositoryReturns = true, expected = true, expectedRepositoryCalls = 0),
    )

    private companion object {
        const val WALLET_NAME = "Wallet"
    }
}