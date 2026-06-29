package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class CreateAndSendTronGaslessTransactionUseCaseTest {

    private val transactionRepository: TransactionRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val signer: TransactionSigner = mockk()

    private val useCase = CreateAndSendTronGaslessTransactionUseCase(
        transactionRepository = transactionRepository,
        walletManagersFacade = walletManagersFacade,
        tronGaslessTransactionRepository = tronGaslessTransactionRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        getHotWalletSigner = { signer },
    )

    private val userWallet = MockUserWalletFactory.create()
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtCurrency = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Tron,
        contractAddress = usdtContract,
    )
    private val sentToken = Token(symbol = "USDT", contractAddress = usdtContract, decimals = 6)

    private val originalTx = TransactionData.Uncompiled(
        amount = Amount(token = sentToken, value = BigDecimal("50")),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TTo",
    )
    private val compensationTx = TransactionData.Uncompiled(
        amount = Amount(token = sentToken, value = BigDecimal("2.75")),
        fee = null,
        sourceAddress = "TFrom",
        destinationAddress = "TFee",
    )

    private val quote = TronGaslessQuote(
        quoteId = "q_1",
        feeRecipient = "TFee",
        compensationToken = usdtContract,
        compensationAmountRaw = BigInteger("2750000"),
        compensationAmountDecimal = BigDecimal("2.75"),
        energy = 1,
        bandwidth = 1,
        trxCostSun = BigInteger("1"),
        expiresAtEpochMs = 1,
    )
    private val fee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(amount = Amount(token = sentToken, value = BigDecimal("2.75"))),
        ),
        feeTokenId = usdtCurrency.id,
        tronGaslessQuote = quote,
    )

    @BeforeEach
    fun setUp() {
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) } returns signer
        coEvery {
            transactionRepository.createTransferTransaction(any(), any(), any(), any(), any(), any(), any())
        } returns compensationTx
        coEvery {
            walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any())
        } returns listOf("signedComp", "signedOrig")
        coEvery {
            tronGaslessTransactionRepository.submit("q_1", "signedComp", "signedOrig")
        } returns TronGaslessSubmitResult(compensationTxHash = "hComp", originalTxHash = "hOrig", status = "BROADCAST")
    }

    @Test
    fun `GIVEN signing and submit succeed WHEN invoke THEN returns original hash, compensation signed first`() =
        runTest {
            // Act
            val result = useCase(userWallet, usdtCurrency.network, originalTx, fee)

            // Assert
            assertThat(result.getOrNull()).isEqualTo("hOrig")
            coVerify {
                walletManagersFacade.signTronGaslessTransactions(
                    userWalletId = any(),
                    network = any(),
                    transactionDataList = match { it.size == 2 && it[0] === compensationTx && it[1] === originalTx },
                    signer = any(),
                )
            }
            coVerify { tronGaslessTransactionRepository.submit("q_1", "signedComp", "signedOrig") }
        }

    @Test
    fun `GIVEN signing returns null WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.signTronGaslessTransactions(any(), any(), any(), any()) } returns null

        // Act
        val result = useCase(userWallet, usdtCurrency.network, originalTx, fee)

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN backend reports partial failure WHEN invoke THEN error`() = runTest {
        // Arrange
        coEvery {
            tronGaslessTransactionRepository.submit("q_1", "signedComp", "signedOrig")
        } returns TronGaslessSubmitResult(compensationTxHash = "hComp", originalTxHash = "hOrig", status = "PARTIAL_FAILED")

        // Act
        val result = useCase(userWallet, usdtCurrency.network, originalTx, fee)

        // Assert
        assertThat(result.isLeft()).isTrue()
    }
}