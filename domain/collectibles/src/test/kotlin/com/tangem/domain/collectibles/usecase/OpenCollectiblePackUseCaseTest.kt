package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
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
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpenCollectiblePackUseCaseTest {

    private val collectiblesRepository: CollectiblesRepository = mockk()
    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val hotDelegate: CollectiblesHotDelegate = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()

    private val userWallet: UserWallet = mockk { every { walletId } returns WALLET_ID }

    private val useCase = OpenCollectiblePackUseCase(
        collectiblesRepository = collectiblesRepository,
        gachaVaultRepository = gachaVaultRepository,
        hotDelegate = hotDelegate,
        ownerDeriver = ownerDeriver,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(collectiblesRepository, gachaVaultRepository, hotDelegate, ownerDeriver)
        coEvery { hotDelegate.getPublicKeyOrNull(any()) } returns DELEGATE
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns OWNER
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns VAULT.right()
        coEvery { collectiblesRepository.generatePurchaseTx(WALLET_ID, any()) } returns PURCHASE
        coEvery { gachaVaultRepository.sendPurchasePack(userWallet, DELEGATE, AMOUNT, FEE_AMOUNT, MEMO) } returns
            TX_HASH.right()
        coEvery { collectiblesRepository.reportPurchaseTxSent(WALLET_ID, TX_ID, TX_HASH) } returns
            CollectibleTxStatus.PURCHASE_TX_HASH
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returns PURCHASED
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID) } returns OPENED
    }

    @Test
    fun `GIVEN every step succeeds WHEN invoked THEN steps go generating confirming opening opened`() = runTest {
        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList()

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
                GeneratePurchaseTxParams(fromAddress = VAULT, machineId = MACHINE_ID, toAddress = OWNER),
            )
        }
    }

    @Test
    fun `GIVEN turbo enabled WHEN invoked THEN auto-sell proceeds go to the owner`() = runTest {
        // Act
        useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = true)).toList()

        // Assert
        coVerify(exactly = 1) {
            collectiblesRepository.generatePurchaseTx(
                WALLET_ID,
                GeneratePurchaseTxParams(
                    fromAddress = VAULT,
                    machineId = MACHINE_ID,
                    toAddress = OWNER,
                    collectibleTurboMode = CollectibleTurboMode(isEnabled = true, buybackAddress = OWNER),
                ),
            )
        }
    }

    @Test
    fun `GIVEN the hot delegate is unreadable WHEN invoked THEN the purchase fails before generating`() = runTest {
        // Arrange
        coEvery { hotDelegate.getPublicKeyOrNull(any()) } returns null

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @Test
    fun `GIVEN the owner key is not stored WHEN invoked THEN the purchase fails before generating`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @Test
    fun `GIVEN the vault address is unknown WHEN invoked THEN the purchase fails before generating`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.getVaultAddress(userWallet) } returns
            CollectiblesAccountError.DelegateNotProvisioned.left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { collectiblesRepository.generatePurchaseTx(any(), any()) }
    }

    @Test
    fun `GIVEN low inventory WHEN invoked THEN it fails as low inventory and pays nothing`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.generatePurchaseTx(any(), any()) } throws CollectibleException.LowInventory()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.LowInventory)
        coVerify(exactly = 0) { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN generating fails WHEN invoked THEN the purchase fails and pays nothing`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.generatePurchaseTx(any(), any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
        coVerify(exactly = 0) { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the send fails WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any(), any()) } returns
            CollectiblesAccountError.DelegateNotProvisioned.left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    @Test
    fun `GIVEN the hot delegate fails to sign WHEN invoked THEN the purchase fails as unpaid`() = runTest {
        // Arrange
        coEvery { gachaVaultRepository.sendPurchasePack(any(), any(), any(), any(), any()) } returns
            CollectiblesAccountError.DelegateSigningFailed(IllegalStateException()).left()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PurchaseFailed)
    }

    @Test
    fun `GIVEN reporting the hash fails WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.reportPurchaseTxSent(any(), any(), any()) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    @Test
    fun `GIVEN the payment is pending then confirmed WHEN invoked THEN it polls until purchased`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returnsMany listOf(
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT),
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT),
            PURCHASED,
        )

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 3) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN a status request fails WHEN invoked THEN polling goes on`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } throws IllegalStateException() andThen
            PURCHASED

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 2) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN an unknown status WHEN invoked THEN polling stops and the opening is tried`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returns
            state(CollectibleTxStatus.UNKNOWN)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Opened(PACK))
        coVerify(exactly = 1) { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) }
    }

    @Test
    fun `GIVEN the payment never lands WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returns
            state(CollectibleTxStatus.WAITING_FOR_PAYMENT)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the purchase is closed WHEN invoked THEN it fails by its status without opening`(
        model: ClosedStatusModel,
    ) = runTest {
        // Arrange
        coEvery { collectiblesRepository.getTxState(WALLET_ID, TX_ID, any()) } returns state(model.status)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        coVerify(exactly = 0) { collectiblesRepository.openPack(any(), any()) }
    }

    @Test
    fun `GIVEN the opening answers refunded WHEN invoked THEN it fails as refunded`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID) } returns state(CollectibleTxStatus.REFUNDED)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.Refunded)
    }

    internal data class ClosedStatusModel(val status: CollectibleTxStatus, val expected: OpenPackStep.Failed)

    private fun provideTestModels() = listOf(
        ClosedStatusModel(CollectibleTxStatus.EXPIRED, OpenPackStep.Failed.Expired),
        ClosedStatusModel(CollectibleTxStatus.REFUNDED, OpenPackStep.Failed.Refunded),
        ClosedStatusModel(CollectibleTxStatus.PAUSED, OpenPackStep.Failed.PackUnopened(TX_ID)),
        ClosedStatusModel(CollectibleTxStatus.AUTO_BUYBACKED, OpenPackStep.Failed.Unexpected),
        ClosedStatusModel(CollectibleTxStatus.MANUALLY_BUYBACKED, OpenPackStep.Failed.Unexpected),
    )

    @Test
    fun `GIVEN opening fails WHEN invoked THEN it is not retried and the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
        coVerify(exactly = 1) { collectiblesRepository.openPack(WALLET_ID, TX_ID) }
    }

    @Test
    fun `GIVEN the pack opens without the card WHEN invoked THEN the pack is left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID) } returns state(CollectibleTxStatus.PACK_OPENED)

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Buy(MACHINE_ID, isTurboEnabled = false)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    @Test
    fun `GIVEN a pack left unopened WHEN reopen requested THEN it waits for the payment and opens without paying`() = runTest {
        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID)).toList()

        // Assert
        assertThat(actual).containsExactly(
            OpenPackStep.Confirming,
            OpenPackStep.Opening,
            OpenPackStep.Opened(PACK),
        ).inOrder()
        coVerify(exactly = 0) {
            collectiblesRepository.generatePurchaseTx(any(), any())
            gachaVaultRepository.sendPurchasePack(any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { collectiblesRepository.openPack(WALLET_ID, TX_ID) }
    }

    @Test
    fun `GIVEN opening fails again WHEN reopen requested THEN the pack is still left unopened`() = runTest {
        // Arrange
        coEvery { collectiblesRepository.openPack(WALLET_ID, TX_ID) } throws IllegalStateException()

        // Act
        val actual = useCase(userWallet, OpenPackRequest.Reopen(TX_ID)).toList().last()

        // Assert
        assertThat(actual).isEqualTo(OpenPackStep.Failed.PackUnopened(TX_ID))
    }

    private companion object {

        val WALLET_ID = UserWalletId("aa")
        val DELEGATE = HotDelegatePublicKey("3hTCy6rJu8iXNWnFMB741HrVnbS5bViyfguEAMF5iT2P")
        const val OWNER = "HoRGkYTxNBbufFhzGzaP8Bek6kJHcTdbYMSMPGN9Mk66"
        const val VAULT = "AsE1WN52nUmrsGGDFtW4AA5YKV1thpj7jEfWV2Rhx28d"
        const val MACHINE_ID = "pokemon_50"
        const val TX_ID = "tx-id"
        const val MEMO = "cc-uuid"
        const val AMOUNT = 50_000_000L
        const val FEE_AMOUNT = 1_000_000L
        const val TX_HASH = "hash"

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
            grade = null,
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