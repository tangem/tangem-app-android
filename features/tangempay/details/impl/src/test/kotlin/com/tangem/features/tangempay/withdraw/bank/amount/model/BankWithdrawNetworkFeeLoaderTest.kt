package com.tangem.features.tangempay.withdraw.bank.amount.model

import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawNetworkFeeLoaderTest {

    private val userWalletId = UserWalletId("011")
    private val moneyAccount = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1")
    private val token = paymentToken()
    private val userWallet: UserWallet = mockk()
    private val transaction: TransactionData.Uncompiled = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase = mockk()
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk()
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk()

    private val loader = BankWithdrawNetworkFeeLoader(
        getUserWalletUseCase = getUserWalletUseCase,
        getMoneyAccountCryptoCurrencyStatusUseCase = getMoneyAccountCryptoCurrencyStatusUseCase,
        createTransferTransactionUseCase = createTransferTransactionUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getUserWalletUseCase,
            getMoneyAccountCryptoCurrencyStatusUseCase,
            createTransferTransactionUseCase,
            getFeeForTokenUseCase,
        )
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        coEvery {
            getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token)
        } returns (Account.Money(userWalletId) to accountTokenStatus()).some()
        coEvery {
            createTransferTransactionUseCase(
                amount = any(),
                memo = null,
                destination = ACCOUNT_ADDRESS,
                userWalletId = userWalletId,
                network = token.network,
            )
        } returns transaction.right()
        coEvery {
            getFeeForTokenUseCase(
                userWallet = userWallet,
                token = token,
                transactionData = transaction,
                sentAmount = BALANCE,
            )
        } returns feeOf(BigDecimal("0.0123")).right()
    }

    @Test
    fun `GIVEN payment account WHEN load THEN no network fee and nothing requested`() = runTest {
        // Act
        val actual = loader.load(BankWithdrawSourceAccount.Payment(userWalletId), token)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal.ZERO.right())
        coVerify(exactly = 0) { getFeeForTokenUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN money account WHEN load THEN fee of sending its balance to itself in fiat`() = runTest {
        // Arrange
        coEvery {
            getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token)
        } returns (Account.Money(userWalletId) to accountTokenStatus(fiatRate = BigDecimal("0.9"))).some()

        // Act
        val actual = loader.load(moneyAccount, token)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("0.01107").right())
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
    fun `GIVEN money account WHEN a step fails THEN network fee unavailable`(model: FailureModel) = runTest {
        // Arrange
        model.arrange()

        // Act
        val actual = loader.load(moneyAccount, token)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    private fun provideTestModels() = listOf(
        FailureModel(
            name = "wallet not found",
            arrange = {
                every { getUserWalletUseCase(userWalletId) } returns GetUserWalletError.UserWalletNotFound.left()
            },
            expected = GetFeeError.UnknownError,
        ),
        FailureModel(
            name = "account token status missing",
            arrange = {
                coEvery { getMoneyAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, token) } returns None
            },
            expected = GetFeeError.UnknownError,
        ),
        FailureModel(
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
        FailureModel(
            name = "gasless fee refused",
            arrange = {
                coEvery {
                    getFeeForTokenUseCase(
                        userWallet = userWallet,
                        token = token,
                        transactionData = transaction,
                        sentAmount = BALANCE,
                    )
                } returns GetFeeError.GaslessError.NotEnoughFunds.left()
            },
            expected = GetFeeError.GaslessError.NotEnoughFunds,
        ),
    )

    internal data class FailureModel(
        val name: String,
        val arrange: () -> Unit,
        val expected: GetFeeError,
    ) {
        override fun toString(): String = name
    }

    private fun accountTokenStatus(fiatRate: BigDecimal = BigDecimal.ONE): CryptoCurrencyStatus = requireNotNull(
        activeMoneyAccount(
            token = token,
            balance = moneyAccountBalance(cryptoAmount = BALANCE, fiatRate = fiatRate),
        ).cryptoCurrencyStatus,
    )

    private fun feeOf(value: BigDecimal): TransactionFeeExtended = mockk {
        every { transactionFee.normal.amount.value } returns value
    }

    private companion object {
        const val ACCOUNT_ADDRESS = "0xaccount"
        val BALANCE = BigDecimal("2325.55")
        val TRANSFER_ERROR = IllegalStateException("No wallet manager")
    }
}