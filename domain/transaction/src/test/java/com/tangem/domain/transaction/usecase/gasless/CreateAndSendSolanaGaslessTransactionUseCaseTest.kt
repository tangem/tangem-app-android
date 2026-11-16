package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSignedTransaction
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
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
    private val sol = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val network = sol.network
    private val fee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(amount = Amount(blockchain = Blockchain.Solana, value = 0.toBigDecimal())),
        ),
        feeTokenId = sol.id,
        gaslessProtocolData = GaslessProtocolData.Solana(feePayer = "FeePayer", network = network),
    )
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
        val actual = useCase(userWallet, txData, fee)

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
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual).isEqualTo(SendTransactionError.SolanaGasless(SolanaGaslessError.BlockhashExpired).left())
    }

    @Test
    fun `GIVEN signing fails WHEN invoke THEN signing error and nothing submitted`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns
            Result.Failure(BlockchainSdkError.Solana.GaslessMessageTooLarge)

        // Act
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(SendTransactionError.BlockchainSdkError::class.java)
        coVerify(exactly = 0) { repository.submit(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no Solana wallet manager WHEN invoke THEN UnknownError`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) } returns null

        // Act
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual).isEqualTo(SendTransactionError.UnknownError().left())
    }

    @Test
    fun `GIVEN submit succeeds WHEN invoke THEN refreshes network status`() = runTest {
        // Arrange
        givenSignedAndSubmitted(signature = "sig")

        // Act
        useCase(userWallet, txData, fee)

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
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual).isEqualTo("sig".right())
    }

    @Test
    fun `GIVEN submit fails as not sent WHEN invoke THEN network status not refreshed`() = runTest {
        // Arrange
        givenSignedAndRejected(error = SolanaGaslessError.BlockhashExpired)

        // Act
        useCase(userWallet, txData, fee)

        // Assert
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN submit may have been sent WHEN invoke THEN refreshes network status and returns MaybeSent`() = runTest {
        // Arrange
        givenSignedAndRejected(error = SolanaGaslessError.MaybeSent(signature = "sig"))

        // Act
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual).isEqualTo(SendTransactionError.SolanaGasless(SolanaGaslessError.MaybeSent("sig")).left())
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
        val actual = useCase(userWallet, txData, fee)

        // Assert
        assertThat(actual).isEqualTo(SendTransactionError.SolanaGasless(SolanaGaslessError.MaybeSent(null)).left())
    }

    @Test
    fun `GIVEN fee without a Solana fee payer WHEN invoke THEN DataError and nothing signed`() = runTest {
        // Arrange
        val evmFee = fee.copy(gaslessProtocolData = GaslessProtocolData.Evm)

        // Act
        val actual = useCase(userWallet, txData, evmFee)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(SendTransactionError.DataError::class.java)
        coVerify(exactly = 0) { walletManagersFacade.signSolanaGaslessTransaction(any(), any(), any(), any(), any()) }
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