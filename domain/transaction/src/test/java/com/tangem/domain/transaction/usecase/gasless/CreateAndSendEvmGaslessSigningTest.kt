package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.gasless.EthereumGaslessDataProvider
import com.tangem.blockchain.blockchains.ethereum.models.EIP7702AuthorizationData
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateAndSendEvmGaslessSigningTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val gaslessSignerProvider: GaslessSignerProvider = mockk()
    private val signer: TransactionSigner = mockk()
    private val walletManager: WalletManager = mockk(moreInterfaces = arrayOf(EthereumGaslessDataProvider::class)) {
        every { wallet.blockchain } returns Blockchain.Ethereum
        every { wallet.publicKey } returns mockk(relaxed = true)
    }

    private val sut = CreateAndSendEvmGaslessTransactionUseCase(
        walletManagersFacade = walletManagersFacade,
        singleAccountListSupplier = singleAccountListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessSignerProvider = gaslessSignerProvider,
    )

    private val userWalletId = UserWalletId(stringValue = "0123456789")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
    private val usdc = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = USDC_CONTRACT,
        symbol = "USDC",
        decimals = 6,
    )
    private val usdcAmount = Amount(
        token = Token(symbol = "USDC", contractAddress = USDC_CONTRACT, decimals = 6),
        value = BigDecimal("10"),
    )
    private val transactionData = TransactionData.Uncompiled(
        amount = usdcAmount,
        fee = null,
        sourceAddress = SOURCE_ADDRESS,
        destinationAddress = RECIPIENT,
        extras = EthereumTransactionExtras(callData = TransferERC20TokenCallData(RECIPIENT, usdcAmount)),
        contractAddress = USDC_CONTRACT,
    )
    private val tokenFee = Fee.Ethereum.TokenCurrency(
        amount = usdcAmount.copy(value = BigDecimal("0.5")),
        gasLimit = BigInteger("100000"),
        coinPriceInToken = BigInteger("2000000000"),
        feeTransferGasLimit = BigInteger("60000"),
        baseGas = BigInteger("21000"),
    )
    private val fee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = tokenFee),
        feeTokenId = usdc.id,
        gaslessFeePlan = GaslessFeePlan.TokenPay(feeToken = usdc, fee = tokenFee),
        callGasLimits = listOf(BigInteger("60000")),
        gaslessProtocolData = GaslessProtocolData.Evm,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the signer fails WHEN invoke THEN the failure maps to a send error`(model: SigningModel) = runTest {
        // Arrange
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null
        coEvery { nonPortfolioCurrenciesProvider.findCurrency(userWalletId, usdc.id) } returns usdc
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, usdc.network) } returns walletManager
        val gaslessDataProvider = walletManager as EthereumGaslessDataProvider
        coEvery { gaslessDataProvider.getGaslessContractNonce(SOURCE_ADDRESS) } returns Result.Success(BigInteger.ZERO)
        coEvery { gaslessDataProvider.prepareEIP7702AuthorizationData(isV2 = true) } returns Result.Success(
            EIP7702AuthorizationData(
                chainId = 1,
                nonce = BigInteger.ZERO,
                data = ByteArray(32),
                executorAddress = EXECUTOR,
            ),
        )
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns FEE_RECEIVER
        every { gaslessTransactionRepository.getChainIdForNetwork(usdc.network) } returns 1
        every { gaslessSignerProvider(userWallet) } returns signer
        coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns CompletionResult.Failure(model.signerError)

        // Act
        val actual = sut(userWallet = userWallet, transactionData = transactionData, fee = fee)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
        coVerify(exactly = 0) {
            gaslessTransactionRepository.signGaslessTransaction(any(), any(), any(), any(), any())
        }
    }

    private fun provideTestModels() = listOf(
        SigningModel(
            signerError = TangemSdkError.UserCancelled(),
            expected = SendTransactionError.UserCancelledError,
        ),
        SigningModel(
            signerError = TangemSdkError.ExceptionError(IllegalStateException("wallet is locked")),
            expected = SendTransactionError.DataError("Signing failed: 50000"),
        ),
    )

    data class SigningModel(val signerError: TangemSdkError, val expected: SendTransactionError)

    private companion object {
        const val USDC_CONTRACT = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val SOURCE_ADDRESS = "0x7f56aab66955bc02cc6b2870d4cddc12b0221c55"
        const val RECIPIENT = "0xfc9013965447f804042a03ae4b98130a8c300a2f"
        const val EXECUTOR = "0x5b38da6a701c568545dcfcb03fcb875f56beddc4"
        const val FEE_RECEIVER = "0x3a1f7e2c9b4d5e6f80912a3b4c5d6e7f8091a2b3"
    }
}