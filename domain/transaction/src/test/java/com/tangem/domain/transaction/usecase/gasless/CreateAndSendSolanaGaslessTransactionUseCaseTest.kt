package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSignedTransaction
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.transaction.error.SolanaGaslessSendError
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CreateAndSendSolanaGaslessTransactionUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val repository: SolanaGaslessTransactionRepository = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val hotSigner: TransactionSigner = mockk()
    private val gaslessSignerProvider: GaslessSignerProvider = mockk { every { this@mockk(any()) } returns hotSigner }
    private val useCase = CreateAndSendSolanaGaslessTransactionUseCase(
        walletManagersFacade = walletManagersFacade,
        solanaGaslessTransactionRepository = repository,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        gaslessSignerProvider = gaslessSignerProvider,
    )

    private val walletId = UserWalletId("011")
    private val userWallet: UserWallet.Hot = mockk { every { walletId } returns this@CreateAndSendSolanaGaslessTransactionUseCaseTest.walletId }
    private val network: Network = mockk()
    private val txData = TransactionData.Compiled(TransactionData.Compiled.Data.Bytes(ByteArray(10)))
    private val signedBytes = ByteArray(20) { 1 }

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, repository, singleNetworkStatusFetcher)
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN signing and submit succeed WHEN invoke THEN returns backend signature`() = runTest {
        // Arrange
        coEvery {
            walletManagersFacade.signSolanaGaslessTransaction(walletId, network, txData, "FeePayer", hotSigner)
        } returns Result.Success(SolanaGaslessSignedTransaction(signedBytes, lastValidBlockHeight = 9))
        coEvery { repository.submit(walletId, signedBytes, 9) } returns "sig".right()

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo("sig".right())
    }

    @Test
    fun `GIVEN backend rejects WHEN invoke THEN Service error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns
            Result.Success(SolanaGaslessSignedTransaction(signedBytes, lastValidBlockHeight = 9))
        coEvery { repository.submit(any(), any(), any()) } returns SolanaGaslessError.BlockhashExpired.left()

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessSendError.Service(SolanaGaslessError.BlockhashExpired).left())
    }

    @Test
    fun `GIVEN signing fails WHEN invoke THEN Signing error and nothing submitted`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns
            Result.Failure(BlockchainSdkError.Solana.GaslessMessageTooLarge)

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(SolanaGaslessSendError.Signing::class.java)
        coVerify(exactly = 0) { repository.submit(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no Solana wallet manager WHEN invoke THEN Signing UnknownError`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns null

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessSendError.Signing(SendTransactionError.UnknownError()).left())
    }

    @Test
    fun `GIVEN submit succeeds WHEN invoke THEN refreshes network status`() = runTest {
        // Arrange
        givenSignedAndSubmitted(signature = "sig")

        // Act
        useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher(SingleNetworkStatusFetcher.Params(userWalletId = walletId, network = network))
        }
    }

    @Test
    fun `GIVEN network status refresh fails WHEN invoke THEN still returns signature`() = runTest {
        // Arrange
        givenSignedAndSubmitted(signature = "sig")
        coEvery { singleNetworkStatusFetcher(any()) } returns Either.Left(IllegalStateException("refresh failed"))

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo("sig".right())
    }

    @Test
    fun `GIVEN submit fails as not sent WHEN invoke THEN network status not refreshed`() = runTest {
        // Arrange
        givenSignedAndRejected(error = SolanaGaslessError.BlockhashExpired)

        // Act
        useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN submit may have been sent WHEN invoke THEN refreshes network status and returns MaybeSent`() = runTest {
        // Arrange
        givenSignedAndRejected(error = SolanaGaslessError.MaybeSent(signature = "sig"))

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessSendError.Service(SolanaGaslessError.MaybeSent("sig")).left())
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher(SingleNetworkStatusFetcher.Params(userWalletId = walletId, network = network))
        }
    }

    @Test
    fun `GIVEN may have been sent and refresh fails WHEN invoke THEN still returns MaybeSent`() = runTest {
        // Arrange
        givenSignedAndRejected(error = SolanaGaslessError.MaybeSent(signature = null))
        coEvery { singleNetworkStatusFetcher(any()) } returns Either.Left(IllegalStateException("refresh failed"))

        // Act
        val actual = useCase(userWallet, network, txData, feePayer = "FeePayer")

        // Assert
        assertThat(actual).isEqualTo(SolanaGaslessSendError.Service(SolanaGaslessError.MaybeSent(null)).left())
    }

    private fun givenSignedAndRejected(error: SolanaGaslessError) {
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns
            Result.Success(SolanaGaslessSignedTransaction(signedBytes, lastValidBlockHeight = 9))
        coEvery { repository.submit(any(), any(), any()) } returns error.left()
    }

    private fun givenSignedAndSubmitted(signature: String) {
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns
            Result.Success(SolanaGaslessSignedTransaction(signedBytes, lastValidBlockHeight = 9))
        coEvery { repository.submit(any(), any(), any()) } returns signature.right()
    }
}