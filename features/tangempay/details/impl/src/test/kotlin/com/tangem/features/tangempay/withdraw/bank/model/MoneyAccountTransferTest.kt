package com.tangem.features.tangempay.withdraw.bank.model

import arrow.core.Either
import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.withdraw.bank.activeMoneyAccount
import com.tangem.features.tangempay.withdraw.bank.moneyAccountBalance
import com.tangem.features.tangempay.withdraw.bank.paymentToken
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

internal class MoneyAccountTransferTest {

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk()
    private val token = paymentToken()
    private val transaction: TransactionData.Uncompiled = mockk()
    private val feeTransaction: TransactionData.Uncompiled = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase = mockk()
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk()
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk()
    private val transfer = MoneyAccountTransfer(
        getUserWalletUseCase = getUserWalletUseCase,
        getMoneyAccountCryptoCurrencyStatusUseCase = getMoneyAccountCryptoCurrencyStatusUseCase,
        createTransferTransactionUseCase = createTransferTransactionUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
        createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getUserWalletUseCase,
            getMoneyAccountCryptoCurrencyStatusUseCase,
            createTransferTransactionUseCase,
            getFeeForTokenUseCase,
            createAndSendGaslessTransactionUseCase,
        )
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        accountTokenStatus()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EstimateFee {

        @BeforeEach
        fun stubBalanceTransfer() {
            transferBuiltTo(destination = ACCOUNT_ADDRESS, amount = BALANCE)
            coEvery { getFeeForTokenUseCase(userWallet, token, transaction, BALANCE) } returns
                feeOf(BigDecimal("0.0123")).right()
        }

        @ParameterizedTest
        @MethodSource("provideEstimateModels")
        fun `GIVEN account balance WHEN estimateFee THEN USDC fee of sending it to the account rounded up to the cent`(
            model: EstimateModel,
        ) = runTest {
            // Arrange
            accountTokenStatus(fiatRate = model.fiatRate)
            coEvery { getFeeForTokenUseCase(userWallet, token, transaction, BALANCE) } returns
                feeOf(model.fee).right()

            // Act
            val actual = transfer.estimateFee(userWalletId, token)

            // Assert
            assertThat(actual).isEqualTo(model.expected.right())
            coVerify(exactly = 1) {
                createTransferTransactionUseCase(
                    amount = match { it.value == BALANCE },
                    memo = null,
                    destination = ACCOUNT_ADDRESS,
                    userWalletId = userWalletId,
                    network = token.network,
                )
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a step fails WHEN estimateFee THEN network fee unavailable`(model: EstimateFailureModel) = runTest {
            // Arrange
            model.arrange()

            // Act
            val actual = transfer.estimateFee(userWalletId, token)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
        }

        private fun provideEstimateModels() = listOf(
            EstimateModel(fee = BigDecimal("0.0123"), fiatRate = BigDecimal("0.9"), expected = BigDecimal("0.02")),
            EstimateModel(fee = BigDecimal("0.03"), fiatRate = BigDecimal.ONE, expected = BigDecimal("0.03")),
            EstimateModel(fee = BigDecimal("0.030001"), fiatRate = BigDecimal.ONE, expected = BigDecimal("0.04")),
            EstimateModel(fee = BigDecimal("0.0123"), fiatRate = null, expected = BigDecimal("0.02")),
        )

        private fun provideTestModels() = listOf(
            EstimateFailureModel(
                name = "wallet not found",
                arrange = {
                    every { getUserWalletUseCase(userWalletId) } returns GetUserWalletError.UserWalletNotFound.left()
                },
                expected = GetFeeError.UnknownError,
            ),
            EstimateFailureModel(
                name = "account token status missing",
                arrange = {
                    coEvery { getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token) } returns None
                },
                expected = GetFeeError.UnknownError,
            ),
            EstimateFailureModel(
                name = "transfer not built",
                arrange = {
                    coEvery {
                        createTransferTransactionUseCase(
                            amount = any(),
                            memo = null,
                            destination = ACCOUNT_ADDRESS,
                            userWalletId = userWalletId,
                            network = token.network,
                        )
                    } returns TRANSFER_ERROR.left()
                },
                expected = GetFeeError.DataError(TRANSFER_ERROR),
            ),
            EstimateFailureModel(
                name = "gasless fee refused",
                arrange = {
                    coEvery { getFeeForTokenUseCase(userWallet, token, transaction, BALANCE) } returns
                        GetFeeError.GaslessError.NotEnoughFunds.left()
                },
                expected = GetFeeError.GaslessError.NotEnoughFunds,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Send {

        private val fee = feeOf(BigDecimal("0.02"))

        @BeforeEach
        fun stubTransfer() {
            transferBuiltTo(destination = DEPOSIT_ADDRESS, amount = AMOUNT)
            coEvery {
                createTransferTransactionUseCase(
                    amount = match { it.value == AMOUNT },
                    fee = fee.transactionFee.normal,
                    memo = null,
                    destination = DEPOSIT_ADDRESS,
                    userWalletId = userWalletId,
                    network = token.network,
                )
            } returns feeTransaction.right()
            coEvery { getFeeForTokenUseCase(userWallet, token, transaction, AMOUNT) } returns fee.right()
            coEvery { createAndSendGaslessTransactionUseCase(userWallet, feeTransaction, fee) } returns TX_HASH.right()
        }

        @Test
        fun `GIVEN balance covers amount and network fee WHEN send THEN that amount signed and sent gasless`() =
            runTest {
                // Act
                val actual = transfer.send(userWalletId, token, AMOUNT, DEPOSIT_ADDRESS, SHOWN_NETWORK_FEE)

                // Assert
                assertThat(actual).isEqualTo(Unit.right())
                coVerify(exactly = 1) { getFeeForTokenUseCase(userWallet, token, transaction, AMOUNT) }
                coVerify(exactly = 1) {
                    createTransferTransactionUseCase(
                        amount = match { it.value == AMOUNT },
                        fee = fee.transactionFee.normal,
                        memo = null,
                        destination = DEPOSIT_ADDRESS,
                        userWalletId = userWalletId,
                        network = token.network,
                    )
                }
                coVerify(exactly = 1) { createAndSendGaslessTransactionUseCase(userWallet, feeTransaction, fee) }
            }

        @ParameterizedTest
        @MethodSource("provideBalanceModels")
        fun `GIVEN balance WHEN send THEN sent only when it covers the amount and the network fee`(
            model: BalanceModel,
        ) = runTest {
            // Arrange
            accountTokenStatus(balance = model.balance)

            // Act
            val actual = transfer.send(userWalletId, token, AMOUNT, DEPOSIT_ADDRESS, SHOWN_NETWORK_FEE)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @ParameterizedTest
        @MethodSource("provideNetworkFeeModels")
        fun `GIVEN shown network fee WHEN send THEN sent only when the quoted fee is not above it`(
            model: NetworkFeeModel,
        ) = runTest {
            // Act
            val actual = transfer.send(userWalletId, token, AMOUNT, DEPOSIT_ADDRESS, model.shownNetworkFee)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            coVerify(exactly = model.expectedSignedCount) {
                createAndSendGaslessTransactionUseCase(any(), any(), any())
            }
        }

        @ParameterizedTest
        @MethodSource("providePreSignFailureModels")
        fun `GIVEN a step before signing fails WHEN send THEN not sent and nothing signed`(
            model: PreSignFailureModel,
        ) = runTest {
            // Arrange
            model.arrange()

            // Act
            val actual = transfer.send(userWalletId, token, AMOUNT, DEPOSIT_ADDRESS, SHOWN_NETWORK_FEE)

            // Assert
            assertThat(actual).isEqualTo(MoneyAccountTransferError.NotSent.left())
            coVerify(exactly = 0) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN gasless send error WHEN send THEN classified by whether the transfer may have left`(
            model: SendErrorModel,
        ) = runTest {
            // Arrange
            coEvery { createAndSendGaslessTransactionUseCase(userWallet, feeTransaction, fee) } returns
                model.error.left()

            // Act
            val actual = transfer.send(userWalletId, token, AMOUNT, DEPOSIT_ADDRESS, SHOWN_NETWORK_FEE)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
        }

        private fun provideNetworkFeeModels() = listOf(
            NetworkFeeModel(shownNetworkFee = BigDecimal("0.03"), expected = Unit.right(), expectedSignedCount = 1),
            NetworkFeeModel(shownNetworkFee = BigDecimal("0.02"), expected = Unit.right(), expectedSignedCount = 1),
            NetworkFeeModel(
                shownNetworkFee = BigDecimal("0.01"),
                expected = MoneyAccountTransferError.NetworkFeeIncreased(BigDecimal("0.02")).left(),
                expectedSignedCount = 0,
            ),
        )

        private fun provideBalanceModels() = listOf(
            BalanceModel(balance = BALANCE, expected = Unit.right()),
            BalanceModel(balance = BigDecimal("501.02"), expected = Unit.right()),
            BalanceModel(balance = BigDecimal("501.01"), expected = MoneyAccountTransferError.NotSent.left()),
        )

        private fun providePreSignFailureModels() = listOf(
            PreSignFailureModel(
                name = "network fee unavailable",
                arrange = {
                    coEvery { getFeeForTokenUseCase(userWallet, token, transaction, AMOUNT) } returns
                        GetFeeError.GaslessError.NoSupportedTokensFound.left()
                },
            ),
            PreSignFailureModel(
                name = "account token not found",
                arrange = {
                    coEvery { getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token) } returns None
                },
            ),
            PreSignFailureModel(
                name = "balance unknown",
                arrange = {
                    val value: CryptoCurrencyStatus.Value = mockk {
                        every { amount } returns null
                        every { yieldSupplyStatus } returns null
                    }
                    coEvery { getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token) } returns
                        (Account.Money(userWalletId) to CryptoCurrencyStatus(currency = token, value = value)).some()
                },
            ),
        )

        private fun provideTestModels() = listOf(
            SendErrorModel(SendTransactionError.UserCancelledError, MoneyAccountTransferError.Cancelled),
            SendErrorModel(SendTransactionError.DataError("Signing failed"), MoneyAccountTransferError.NotSent),
            SendErrorModel(
                SendTransactionError.NetworkError("Bad request", "400"),
                MoneyAccountTransferError.OutcomeUnknown,
            ),
            SendErrorModel(
                SendTransactionError.NetworkError("Server error", "500"),
                MoneyAccountTransferError.OutcomeUnknown,
            ),
            SendErrorModel(
                SendTransactionError.NetworkError("Gasless service request timed out", null),
                MoneyAccountTransferError.OutcomeUnknown,
            ),
        )
    }

    private fun accountTokenStatus(balance: BigDecimal = BALANCE, fiatRate: BigDecimal? = BigDecimal.ONE) {
        val status: CryptoCurrencyStatus = requireNotNull(
            activeMoneyAccount(
                token = token,
                balance = moneyAccountBalance(cryptoAmount = balance, fiatRate = fiatRate),
            ).cryptoCurrencyStatus,
        )
        coEvery { getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token) } returns
            (Account.Money(userWalletId) to status).some()
    }

    private fun transferBuiltTo(destination: String, amount: BigDecimal) {
        coEvery {
            createTransferTransactionUseCase(
                amount = match { it.value == amount },
                memo = null,
                destination = destination,
                userWalletId = userWalletId,
                network = token.network,
            )
        } returns transaction.right()
    }

    private fun feeOf(value: BigDecimal) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(Amount(currencySymbol = "USDC", value = value, decimals = 6)),
        ),
        feeTokenId = token.id,
        gaslessProtocolData = GaslessProtocolData.Evm,
    )

    internal data class EstimateModel(val fee: BigDecimal, val fiatRate: BigDecimal?, val expected: BigDecimal)

    internal data class NetworkFeeModel(
        val shownNetworkFee: BigDecimal,
        val expected: Either<MoneyAccountTransferError, Unit>,
        val expectedSignedCount: Int,
    )

    internal data class EstimateFailureModel(
        val name: String,
        val arrange: () -> Unit,
        val expected: GetFeeError,
    ) {
        override fun toString(): String = name
    }

    internal data class SendErrorModel(val error: SendTransactionError, val expected: MoneyAccountTransferError)

    internal data class BalanceModel(val balance: BigDecimal, val expected: Either<MoneyAccountTransferError, Unit>)

    internal data class PreSignFailureModel(val name: String, val arrange: () -> Unit) {
        override fun toString(): String = name
    }

    private companion object {
        const val ACCOUNT_ADDRESS = "0xaccount"
        const val DEPOSIT_ADDRESS = "0x0000000000000000000000000000000000000003"
        const val TX_HASH = "0xhash"
        val BALANCE = BigDecimal("2325.55")
        val AMOUNT = BigDecimal("501.00")
        val SHOWN_NETWORK_FEE = BigDecimal("0.03")
        val TRANSFER_ERROR = IllegalStateException("No wallet manager")
    }
}