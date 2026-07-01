package com.tangem.domain.transaction.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SignAndBroadcastPsbtUseCase] — the orchestration that derives the wallet's inputs from a
 * provider PSBT, signs them, and broadcasts the finalized transaction (Bitcoin swap flow).
 */
internal class SignAndBroadcastPsbtUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk(relaxed = true)
    private val signer: TransactionSigner = mockk(relaxed = true)
    private val walletManager: WalletManager = mockk()

    private val useCase = SignAndBroadcastPsbtUseCase(
        cardSdkConfigRepository = cardSdkConfigRepository,
        walletManagersFacade = walletManagersFacade,
        getHotTransactionSigner = { signer },
    )

    private val userWalletId = UserWalletId(stringValue = "deadbeef")
    private val network: Network = mockk(relaxed = true)
    private val userWallet: UserWallet = mockk<UserWallet.Hot>(relaxed = true) {
        every { walletId } returns userWalletId
    }

    private val psbt = "psbt-base64"
    private val signInputs = listOf(SignInput(address = "addr", index = 0, sighashTypes = listOf(1)))

    @BeforeEach
    fun setup() {
        // The use case refreshes the wallet manager (fresh UTXO set) before signing the PSBT.
        coEvery { walletManagersFacade.update(userWalletId, network, emptySet()) } returns mockk(relaxed = true)
    }

    @Test
    fun `GIVEN derive sign broadcast succeed WHEN invoke THEN returns tx hash`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns walletManager
        every { walletManager.deriveSignInputs(psbt) } returns Result.Success(signInputs)
        coEvery { walletManager.signPsbt(psbt, signInputs, signer) } returns Result.Success("signed-psbt")
        coEvery { walletManager.broadcastPsbt("signed-psbt") } returns Result.Success("tx-hash")

        // Act
        val actual = useCase(psbtBase64 = psbt, userWallet = userWallet, network = network)

        // Assert
        assertThat(actual).isEqualTo("tx-hash".right())
        coVerify(exactly = 1) { walletManagersFacade.update(userWalletId, network, emptySet()) }
        coVerify(exactly = 1) { walletManager.broadcastPsbt("signed-psbt") }
    }

    @Test
    fun `GIVEN deriveSignInputs fails WHEN invoke THEN returns error and does not sign`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns walletManager
        every {
            walletManager.deriveSignInputs(psbt)
        } returns Result.Failure(BlockchainSdkError.CustomError("no inputs"))

        // Act
        val actual = useCase(psbtBase64 = psbt, userWallet = userWallet, network = network)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { walletManager.signPsbt(any(), any(), any()) }
    }

    @Test
    fun `GIVEN signPsbt fails WHEN invoke THEN returns error and does not broadcast`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns walletManager
        every { walletManager.deriveSignInputs(psbt) } returns Result.Success(signInputs)
        coEvery {
            walletManager.signPsbt(psbt, signInputs, signer)
        } returns Result.Failure(BlockchainSdkError.CustomError("sign fail"))

        // Act
        val actual = useCase(psbtBase64 = psbt, userWallet = userWallet, network = network)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        coVerify(exactly = 0) { walletManager.broadcastPsbt(any()) }
    }

    @Test
    fun `GIVEN wallet manager missing WHEN invoke THEN returns error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns null

        // Act
        val actual = useCase(psbtBase64 = psbt, userWallet = userWallet, network = network)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }
}