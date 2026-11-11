package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.models.CollectibleException
import com.tangem.domain.collectibles.models.CollectiblePurchaseTx
import com.tangem.domain.collectibles.models.CollectibleTurboMode
import com.tangem.domain.collectibles.models.CollectibleTxState
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.models.GeneratePurchaseTxParams
import com.tangem.domain.collectibles.models.OpenPackRequest
import com.tangem.domain.collectibles.models.OpenPackStep
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpenCollectiblePackUseCaseTest {

    private val collectiblesRepository: CollectiblesRepository = mockk()
    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher = mockk()
    private val activeTxsFetcher: CollectiblesActiveTxsFetcher = mockk()
    private val accountRepository: CollectiblesAccountRepository = mockk()
    private val sessionKey: CollectiblesSessionKey = mockk()

    private val userWallet: UserWallet = mockk { every { walletId } returns WALLET_ID }

    private val useCase = OpenCollectiblePackUseCase(
        collectiblesRepository = collectiblesRepository,
        gachaVaultRepository = gachaVaultRepository,
        ownerDeriver = ownerDeriver,
        accountStatusFetcher = accountStatusFetcher,
        activeTxsFetcher = activeTxsFetcher,
        accountRepository = accountRepository,
        sessionKey = sessionKey,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            collectiblesRepository,
            gachaVaultRepository,
            ownerDeriver,
            accountStatusFetcher,
            activeTxsFetcher,
            accountRepository,
            sessionKey,
        )
        coEvery { gachaVaultRepository.getVaultSessionKey(userWallet) } returns SESSION_KEY.base58.right()
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns VAULT.right()
        coEvery { sessionKey.getPublicKeyOrNull(ACCOUNT_ID) } returns SESSION_KEY
        coEvery { accountRepository.getStatusSync(WALLET_ID) } returns ONBOARDED
        coEvery { accountRepository.saveAccount(any(), any()) } just runs
        every { accountRepository.isRelinkPending(WALLET_ID) } returns false
        coEvery { accountStatusFetcher(any()) } returns Unit.right()
        coEvery { activeTxsFetcher(any()) } returns Unit.right()
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns OWNER
        coEvery { collectiblesRepository.generatePurchaseTx(WALLET_ID, any()) } returns PURCHASE
        coEvery { gachaVaultRepository.sendPurchasePack(userWallet, AMOUNT, FEE_AMOUNT, MEMO) } returns
            TX_HASH.right()
        coEvery { collectiblesRepository.reportPurchaseTxSent(WALLET_ID, TX_ID, TX_HASH) } returns
            CollectibleTxStatus.PURCHASE_TX_SENT
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns PURCHASED
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } returns OPENED
    }

    @Test
    fun `GIVEN the pack opens WHEN invoked THEN the vault balance is refreshed`() = runTest {
        // Act
        useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList()

        // Assert
        coVerify(exactly = 1) { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN the purchase fails WHEN invoked THEN the vault balance is refreshed anyway`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.SessionKeyNotProvisioned.left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 1) { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN the pack opens WHEN invoked THEN the active purchases are re-read`() = runTest {
        // Act
        useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = false).toList()

        // Assert
        coVerify(exactly = 1) { activeTxsFetcher(CollectiblesActiveTxsFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN the opening fails WHEN invoked THEN the active purchases are re-read anyway`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns
            state(CollectibleTxStatus.PACK_OPENING_ERROR)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.OpenFailed)
        coVerify(exactly = 1) { activeTxsFetcher(CollectiblesActiveTxsFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN the active purchases read fails WHEN invoked THEN the pack still opens`() = runTest {
        // Arrange
        coEvery { activeTxsFetcher(any()) } returns IllegalStateException().left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
    }

    @Test
    fun `GIVEN the balance refresh fails WHEN invoked THEN the pack still opens`() = runTest {
        // Arrange
        coEvery { accountStatusFetcher(any()) } returns IllegalStateException().left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
    }

    @Test
    fun `GIVEN every step succeeds WHEN invoked THEN steps go generating confirming opening opened`() = runTest {
        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList()

        // Assert
        assertThat(actual).containsExactly(
            OpenPackStep.Generating,
            OpenPackStep.Confirming,
            OpenPackStep.Opening,
            OpenPackStep.Opened(PACK),
        ).inOrder()
        coVerify(exactly = 1) {
            collectiblesRepository.generatePurchaseTx(
                WALLET_ID,
                GeneratePurchaseTxParams(fromAddress = OWNER, machineId = MACHINE_ID, toAddress = OWNER),
            )
        }
    }

    @Test
    fun `GIVEN turbo enabled WHEN invoked THEN auto-sell proceeds go to the vault`() = runTest {
        // Act
        useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = true).toList()

        // Assert
        coVerify(exactly = 1) {
            collectiblesRepository.generatePurchaseTx(
                WALLET_ID,
                GeneratePurchaseTxParams(
                    fromAddress = OWNER,
                    machineId = MACHINE_ID,
                    toAddress = OWNER,
                    collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = VAULT),
                ),
            )
        }
    }

    @Test
    fun `GIVEN turbo enabled and the vault unread WHEN invoked THEN the purchase fails before generating`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns
            CollectiblesAccountError.SessionKeyNotProvisioned.left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = true).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN turbo mode WHEN invoked THEN the status and the opening ask for the payout only in turbo`(
        isTurboEnabled: Boolean,
    ) = runTest {
        // Act
        useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled).toList()

        // Assert
        coVerify(exactly = 1) {
            collectiblesRepository.getTxState(WALLET_ID, TX_ID, includeAutoBuyback = isTurboEnabled, any())
            collectiblesRepository.openPack(WALLET_ID, TX_ID, includeAutoBuyback = isTurboEnabled)
        }
    }

    @Test
    fun `GIVEN the owner key is not stored WHEN invoked THEN the purchase fails before generating`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @Test
    fun `GIVEN low inventory WHEN invoked THEN it fails as low inventory and pays nothing`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.generatePurchaseTx(any(), any()) } throws CollectibleException.LowInventory()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.LowInventory)
        coVerify(exactly = 0) { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN generating fails WHEN invoked THEN the purchase fails and pays nothing`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.generatePurchaseTx(any(), any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the send fails WHEN invoked THEN the purchase fails`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
    }

    @Test
    fun `GIVEN the vault answers to another session key WHEN invoked THEN a relink is required and nothing is paid`() =
        runTest {
            // Arrange
            coEvery { gachaVaultRepository.getVaultSessionKey(userWallet) } returns OTHER_SESSION_KEY.right()

            // Act
            val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

            // Assert
            assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
            coVerify(exactly = 1) { accountRepository.saveAccount(WALLET_ID, RELINK_REQUIRED) }
            coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
        }

    @Test
    fun `GIVEN a relink still landing WHEN invoked THEN nothing is paid and no new relink is asked`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultSessionKey(userWallet) } returns OTHER_SESSION_KEY.right()
        every { accountRepository.isRelinkPending(WALLET_ID) } returns true

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) {
            accountRepository.saveAccount(any(), any())
            collectiblesRepository.generatePurchaseTx(any(), any())
        }
    }

    @Test
    fun `GIVEN the vault session key cannot be read WHEN invoked THEN the purchase fails and pays nothing`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultSessionKey(userWallet) } returns
            CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { accountRepository.saveAccount(any(), any()) }
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @Test
    fun `GIVEN the vault is re-pointed while paying WHEN the send fails THEN a relink is required`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultSessionKey(userWallet) } returnsMany
            listOf(SESSION_KEY.base58.right(), OTHER_SESSION_KEY.right())
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 1) { accountRepository.saveAccount(WALLET_ID, RELINK_REQUIRED) }
    }

    @Test
    fun `GIVEN the session key still matches WHEN the send fails THEN the account is left as it is`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.Vault(GachaVaultError.WalletUnavailable).left()

        // Act
        useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList()

        // Assert
        coVerify(exactly = 2) { gachaVaultRepository.getVaultSessionKey(userWallet) }
        coVerify(exactly = 0) { accountRepository.saveAccount(any(), any()) }
    }

    @Test
    fun `GIVEN the session key is not provisioned WHEN invoked THEN the purchase fails as unpaid`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.SessionKeyNotProvisioned.left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
    }

    @Test
    fun `GIVEN the session key fails to sign WHEN invoked THEN the purchase fails as unpaid`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any()) } returns
            CollectiblesAccountError.SessionKeySigningFailed(IllegalStateException()).left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
    }

    @Test
    fun `GIVEN reporting the hash fails WHEN invoked THEN the payment is still awaited and the pack opens`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.reportPurchaseTxSent(any(), any(), any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 1) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN the payment is pending then confirmed WHEN invoked THEN it polls until purchased`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returnsMany listOf(
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT),
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT),
            PURCHASED,
        )

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 3) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) }
    }

    @Test
    fun `GIVEN a status request fails WHEN invoked THEN polling goes on`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } throws
            IllegalStateException() andThen PURCHASED

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 2) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) }
    }

    @Test
    fun `GIVEN an unknown status WHEN invoked THEN polling stops and the opening is tried`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns
            state(CollectibleTxStatus.UNKNOWN)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 1) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) }
    }

    @Test
    fun `GIVEN the payment never lands WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
        coVerify(exactly = 0) { collectiblesRepository.openPack(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the payment never lands WHEN reopen requested THEN the pack is still left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returns
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
        coVerify(exactly = 0) { collectiblesRepository.openPack(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the gacha refuses the pack WHEN invoked THEN the opening fails without a retry`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } throws CollectibleException.PackOpeningRefused()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.OpenFailed)
    }

    @Test
    fun `GIVEN the refund is on its way WHEN invoked THEN it fails as refunding`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } throws CollectibleException.RefundInProgress()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.Refunding)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the purchase is closed WHEN invoked THEN it fails by its status without opening`(
        model: ClosedStatusModel,
    ) = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns state(model.status)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        coVerify(exactly = 0) { collectiblesRepository.openPack(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the opening answers refunded WHEN invoked THEN it fails as refunded`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } returns state(CollectibleTxStatus.REFUNDED)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.Refunded)
    }

    internal data class ClosedStatusModel(val status: CollectibleTxStatus, val expected: OpenPackStep.Failed)

    private fun provideTestModels() = listOf(
        ClosedStatusModel(CollectibleTxStatus.EXPIRED, OpenPackStep.Failed.OpenFailed),
        ClosedStatusModel(CollectibleTxStatus.REFUNDED, OpenPackStep.Failed.Refunded),
        ClosedStatusModel(CollectibleTxStatus.PACK_OPENING_ERROR, OpenPackStep.Failed.OpenFailed),
        ClosedStatusModel(CollectibleTxStatus.REFUNDING, OpenPackStep.Failed.Refunding),
        ClosedStatusModel(CollectibleTxStatus.PAUSED, OpenPackStep.Failed.OpenFailed),
        ClosedStatusModel(CollectibleTxStatus.AUTO_BUYBACKED, OpenPackStep.Failed.Unexpected),
        ClosedStatusModel(CollectibleTxStatus.MANUALLY_BUYBACKED, OpenPackStep.Failed.Unexpected),
    )

    @Test
    fun `GIVEN opening fails WHEN invoked THEN it is not retried and the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
        coVerify(exactly = 1) { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN the opening answers auto-buybacked with a payout WHEN invoked THEN it ends auto-buybacked`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } returns CollectibleTxState(
            status = CollectibleTxStatus.AUTO_BUYBACKED,
            pack = PACK,
            autoBuybackAmount = BigDecimal("42.5"),
        )

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = true).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.AutoBuybacked(amount = BigDecimal("42.5"), pack = PACK))
    }

    @Test
    fun `GIVEN the opening answers auto-buybacked with the card but no payout WHEN invoked THEN it fails as unexpected`() =
        runTest {
            // Arrange
            coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } returns
                state(CollectibleTxStatus.AUTO_BUYBACKED, pack = PACK)

            // Act
            val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = true).toList().last()

            // Assert
            assertThat(actual).isEqualTo(OpenPackStep.Failed.Unexpected)
        }

    @Test
    fun `GIVEN the pack opens without the card WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } returns
            state(CollectibleTxStatus.PACK_OPENED)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    @Test
    fun `GIVEN a pack left unopened WHEN reopen requested THEN it waits for the payment and opens without paying`() = runTest {
        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = false).toList()

        // Assert
        assertThat(actual).containsExactly(
            OpenPackStep.Confirming,
            OpenPackStep.Opening,
            OpenPackStep.Opened(PACK),
        ).inOrder()
        coVerify(exactly = 0) {
            collectiblesRepository.generatePurchaseTx(any(), any())
            gachaVaultRepository.sendPurchasePack(any(), any(), any(), any())
        }
        coVerify(exactly = 1) { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN the status answers auto-buybacked with a payout WHEN reopen requested THEN it ends auto-buybacked without opening`() =
        runTest {
            // Arrange
            coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any(), any()) } returns CollectibleTxState(
                status = CollectibleTxStatus.AUTO_BUYBACKED,
                pack = null,
                autoBuybackAmount = BigDecimal("42.5"),
            )

            // Act
            val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = true).toList()

            // Assert
            assertThat(actual).containsExactly(
                OpenPackStep.Confirming,
                OpenPackStep.AutoBuybacked(amount = BigDecimal("42.5"), pack = null),
            ).inOrder()
            coVerify(exactly = 0) { collectiblesRepository.openPack(any(), any(), any()) }
        }

    @Test
    fun `GIVEN opening fails again WHEN reopen requested THEN the pack is still left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID, any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID), isTurboEnabled = false).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    private companion object {

        val WALLET_ID = UserWalletId("aa")
        const val OWNER = "HoRGkYTxNBbufFhzGzaP8Bek6kJHcTdbYMSMPGN9Mk66"
        const val VAULT = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        const val MACHINE_ID = "pokemon_50"
        const val TX_ID = "tx-id"
        const val MEMO = "cc-uuid"
        const val AMOUNT = 50_000_000L
        const val FEE_AMOUNT = 1_000_000L
        const val TX_HASH = "hash"
        const val OTHER_SESSION_KEY = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"

        val ACCOUNT_ID = AccountId.forCollectiblesAccount(WALLET_ID)
        val SESSION_KEY = SessionKeyPublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        val BALANCE: CollectiblesVaultBalance = mockk()
        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.CACHE, vaultBalance = BALANCE)
        val RELINK_REQUIRED = CollectiblesAccountStatusValue.UpdateRequired(
            source = StatusSource.ACTUAL,
            vaultBalance = BALANCE,
        )

        val PURCHASED = state(CollectibleTxStatus.PACK_PURCHASED)

        val PURCHASE = CollectiblePurchaseTx(
            txId = TX_ID,
            memo = MEMO,
            amount = AMOUNT,
            feeAmount = FEE_AMOUNT,
        )

        val PACK = OpenedCollectiblePack(
            txId = TX_ID,
            nftAddress = "nft",
            imageFrontUrl = null,
            imageBackUrl = null,
            rarity = null,
            gradingCompany = null,
            gradingId = "123",
            year = null,
            set = null,
            serialNumber = null,
            insuredValue = null,
        )

        val OPENED = state(CollectibleTxStatus.PACK_OPENED, pack = PACK)

        fun state(status: CollectibleTxStatus, pack: OpenedCollectiblePack? = null) = CollectibleTxState(status, pack)
    }
}