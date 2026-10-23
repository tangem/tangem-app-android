package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.models.BuybackError
import com.tangem.domain.collectibles.models.CollectibleBuybackStatus
import com.tangem.domain.collectibles.models.CollectibleBuybackTx
import com.tangem.domain.collectibles.models.GenerateBuybackTxParams
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BuybackCollectibleUseCaseTest {

    private val collectiblesRepository: CollectiblesRepository = mockk()
    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher = mockk()

    private val userWallet: UserWallet = mockk { every { walletId } returns WALLET_ID }

    private val useCase = BuybackCollectibleUseCase(
        collectiblesRepository = collectiblesRepository,
        gachaVaultRepository = gachaVaultRepository,
        ownerDeriver = ownerDeriver,
        accountStatusFetcher = accountStatusFetcher,
        appScope = TestAppCoroutineScope(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(collectiblesRepository, gachaVaultRepository, ownerDeriver, accountStatusFetcher)
        coEvery { accountStatusFetcher(any()) } returns Unit.right()
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns OWNER
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns VAULT.right()
        coEvery { collectiblesRepository.generateBuybackTx(WALLET_ID, any()) } returns BUYBACK_TX
        coEvery { gachaVaultRepository.sendWithOwnerSignature(userWallet, TRANSACTION) } returns TX_HASH.right()
        coEvery { collectiblesRepository.reportBuybackTxSent(WALLET_ID, BUYBACK_ID, TX_HASH) } returns
            CollectibleBuybackStatus.BUYBACK_TX_SENT
        coEvery { collectiblesRepository.getBuybackStatus(WALLET_ID, BUYBACK_ID) } returns
            CollectibleBuybackStatus.CONFIRMED
    }

    @Test
    fun `GIVEN every step succeeds WHEN invoked THEN the payout goes to the vault`() = runTest {
        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            collectiblesRepository.generateBuybackTx(
                WALLET_ID,
                GenerateBuybackTxParams(fromAddress = OWNER, nftAddress = NFT_ADDRESS, toAddress = VAULT),
            )
            collectiblesRepository.reportBuybackTxSent(WALLET_ID, BUYBACK_ID, TX_HASH)
            accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID))
        }
    }

    @Test
    fun `GIVEN confirmation on the third poll WHEN invoked THEN it completes after three polls`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getBuybackStatus(WALLET_ID, BUYBACK_ID) } returnsMany listOf(
            CollectibleBuybackStatus.BUYBACK_TX_SENT,
            CollectibleBuybackStatus.CONFIRMING,
            CollectibleBuybackStatus.CONFIRMED,
        )

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(currentTime).isEqualTo(6_000L)
        coVerify(exactly = 3) { collectiblesRepository.getBuybackStatus(WALLET_ID, BUYBACK_ID) }
    }

    @Test
    fun `GIVEN never confirmed WHEN invoked THEN not confirmed after five polls in ten seconds`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getBuybackStatus(any(), any()) } returns CollectibleBuybackStatus.CONFIRMING

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(BuybackError.NotConfirmed.left())
        assertThat(currentTime).isEqualTo(10_000L)
        coVerify(exactly = 5) { collectiblesRepository.getBuybackStatus(WALLET_ID, BUYBACK_ID) }
    }

    @Test
    fun `GIVEN the buyback expires WHEN invoked THEN polling stops with not confirmed`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getBuybackStatus(any(), any()) } returns CollectibleBuybackStatus.EXPIRED

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(BuybackError.NotConfirmed.left())
        coVerify(exactly = 1) { collectiblesRepository.getBuybackStatus(any(), any()) }
    }

    @Test
    fun `GIVEN a status request fails WHEN invoked THEN it is just another poll`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getBuybackStatus(any(), any()) } throws IllegalStateException() andThen
            CollectibleBuybackStatus.CONFIRMED

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    @Test
    fun `GIVEN the tx-sent report fails WHEN invoked THEN the status poll still decides`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.reportBuybackTxSent(any(), any(), any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    @Test
    fun `GIVEN the vault address cannot be read WHEN invoked THEN it fails before asking the backend`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns
            CollectiblesAccountError.Vault(GachaVaultError.NetworkUnsupported).left()

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(BuybackError.Unexpected.left())
        coVerify(exactly = 0) { collectiblesRepository.generateBuybackTx(any(), any()) }
    }

    @Test
    fun `GIVEN no stored owner WHEN invoked THEN it fails before asking the backend`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(BuybackError.Unexpected.left())
        coVerify(exactly = 0) { collectiblesRepository.generateBuybackTx(any(), any()) }
        coVerify(exactly = 1) { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID)) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a failure WHEN invoked THEN it ends in the matching error and nothing after it runs`(
        model: FailureModel,
    ) = runTest {
        // Arrange
        model.arrange(this@BuybackCollectibleUseCaseTest)

        // Act
        val actual = useCase(userWallet, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
        coVerify(exactly = 0) { collectiblesRepository.getBuybackStatus(any(), any()) }
        coVerify(exactly = 1) { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID)) }
    }

    internal data class FailureModel(
        val description: String,
        val arrange: BuybackCollectibleUseCaseTest.() -> Unit,
        val expected: BuybackError,
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        FailureModel(
            description = "backend fails",
            arrange = { stubGenerateThrows(IllegalStateException()) },
            expected = BuybackError.Unexpected,
        ),
        FailureModel(
            description = "user cancels the card session",
            arrange = { stubSendFails(CollectiblesAccountError.Cancelled) },
            expected = BuybackError.Cancelled,
        ),
        FailureModel(
            description = "card refuses",
            arrange = { stubSendFails(CollectiblesAccountError.CardError(code = 50003, message = "busy")) },
            expected = BuybackError.CardRefused,
        ),
        FailureModel(
            description = "transaction too large for the card",
            arrange = { stubSendFails(CollectiblesAccountError.Vault(GachaVaultError.TransactionTooLarge)) },
            expected = BuybackError.Unexpected,
        ),
        FailureModel(
            description = "network rejects",
            arrange = { stubSendFails(CollectiblesAccountError.Vault(GachaVaultError.NetworkUnsupported)) },
            expected = BuybackError.Unexpected,
        ),
    )

    private fun stubGenerateThrows(cause: Throwable) {
        coEvery { collectiblesRepository.generateBuybackTx(any(), any()) } throws cause
    }

    private fun stubSendFails(error: CollectiblesAccountError) {
        coEvery { gachaVaultRepository.sendWithOwnerSignature(any(), any()) } returns error.left()
    }

    private companion object {

        val WALLET_ID = UserWalletId("aa")
        const val OWNER = "HoRGkYTxNBbufFhzGzaP8Bek6kJHcTdbYMSMPGN9Mk66"
        const val VAULT = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        const val NFT_ADDRESS = "nft"
        const val BUYBACK_ID = "buyback-id"
        const val TRANSACTION = "AQID"
        const val TX_HASH = "hash"
        val AMOUNT = BigDecimal("42.50")
        val BUYBACK_TX = CollectibleBuybackTx(
            buybackId = BUYBACK_ID,
            memo = "cc-memo",
            amount = AMOUNT,
            transaction = TRANSACTION,
        )
    }
}