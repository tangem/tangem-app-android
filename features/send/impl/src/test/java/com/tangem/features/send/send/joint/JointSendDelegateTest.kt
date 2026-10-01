package com.tangem.features.send.send.joint

import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.blockchain.common.transaction.Fee
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.jointaccount.gas.JointGasTokenResolver
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.safe.JointTransferPayloadCodec
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.CreateJointOperationUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class JointSendDelegateTest {

    private val jointAccountListSupplier: SingleJointAccountListSupplier = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    private val gasBuilder: SafeTxGasBuilder = mockk()
    private val createJointOperationUseCase: CreateJointOperationUseCase = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val gasTokenResolver: JointGasTokenResolver = mockk()

    private val payloadCodec: JointTransferPayloadCodec = mockk {
        every { encode(any()) } returns SafeInnerCall(to = FIXTURE_RECIPIENT, value = BigInteger.ONE, data = "0x")
    }

    private val delegate = JointSendDelegate(
        jointAccountListSupplier = jointAccountListSupplier,
        getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        gasBuilder = gasBuilder,
        createJointOperationUseCase = createJointOperationUseCase,
        payloadCodec = payloadCodec,
        networkReader = networkReader,
        gasTokenResolver = gasTokenResolver,
    )

    private val userWalletId = UserWalletId("0011223344556677")

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            jointAccountListSupplier,
            getAccountCurrencyStatusUseCase,
            gasBuilder,
            createJointOperationUseCase,
            gasTokenResolver,
        )
    }

    @Test
    fun `GIVEN a complete form WHEN the operation is created THEN the intent goes to the use case`() = runTest {
        // Arrange
        val account: Account.Joint = mockk()
        val expectedIntent = JointTransferIntent.Native(
            recipient = FIXTURE_RECIPIENT,
            amountWei = BigInteger("1000000000000000000"),
        )
        coEvery {
            createJointOperationUseCase(account, fixtureCoin.network, expectedIntent, SafeGasToken.Coin)
        } returns Unit.right()

        // Act
        val actual = delegate.createOperation(
            account = account,
            currency = fixtureCoin,
            amount = BigDecimal.ONE,
            destinationUM = createDestinationContent(),
            feeToken = null,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            createJointOperationUseCase(account, fixtureCoin.network, expectedIntent, SafeGasToken.Coin)
        }
    }

    @Test
    fun `GIVEN an incomplete form WHEN the operation is created THEN the payload is rejected without a use case call`() =
        runTest {
            // Act
            val actual = delegate.createOperation(
                account = mockk(),
                currency = fixtureCoin,
                amount = BigDecimal.ONE,
                destinationUM = DestinationUM.Empty(),
                feeToken = null,
            )

            // Assert
            assertThat(actual).isEqualTo(JointOperationCreateError.InvalidPayload.left())
            coVerify(exactly = 0) { createJointOperationUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN a complete form WHEN the fee is loaded THEN the safe tx is mapped to a single fee`() = runTest {
        // Arrange
        val safeTx = createSafeTx()
        coEvery { gasBuilder.build(userWalletId, fixtureCoin.network, any(), SafeGasToken.Coin) } returns safeTx.right()

        // Act
        val actual = delegate.loadFee(
            userWalletId = userWalletId,
            currency = fixtureCoin,
            amountUM = createAmountData(crypto = BigDecimal.ONE),
            destinationUM = createDestinationContent(),
        )

        // Assert
        assertThat(actual).isEqualTo(JointSendFeeConverter(network = fixtureCoin.network).convert(safeTx).right())
    }

    @Test
    fun `GIVEN an incomplete form WHEN the fee is loaded THEN a data error is returned without a build`() = runTest {
        // Act
        val actual = delegate.loadFee(
            userWalletId = userWalletId,
            currency = fixtureCoin,
            amountUM = AmountState.Empty,
            destinationUM = createDestinationContent(),
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(GetFeeError.DataError::class.java)
    }

    @Test
    fun `GIVEN the build fails WHEN the fee is loaded THEN the cause is wrapped into a data error`() = runTest {
        // Arrange
        val cause = IllegalStateException("node is down")
        coEvery { gasBuilder.build(userWalletId, fixtureCoin.network, any(), SafeGasToken.Coin) } returns cause.left()

        // Act
        val actual = delegate.loadFee(
            userWalletId = userWalletId,
            currency = fixtureCoin,
            amountUM = createAmountData(crypto = BigDecimal.ONE),
            destinationUM = createDestinationContent(),
        )

        // Assert
        assertThat(actual).isEqualTo(GetFeeError.DataError(cause).left())
    }

    @Test
    fun `GIVEN no fee token picked WHEN the extended fee is loaded THEN the coin pays it`() = runTest {
        // Arrange
        val safeTx = createSafeTx()
        coEvery { gasBuilder.build(userWalletId, fixtureUsdc.network, any(), SafeGasToken.Coin) } returns safeTx.right()

        // Act
        val actual = delegate.loadFeeExtended(
            userWalletId = userWalletId,
            currency = fixtureUsdc,
            feeCoin = fixtureCoin,
            feeCurrency = null,
            amountUM = createAmountData(crypto = BigDecimal.ONE),
            destinationUM = createDestinationContent(),
        )

        // Assert
        val expected = TransactionFeeExtended(
            transactionFee = JointSendFeeConverter(network = fixtureUsdc.network).convert(safeTx),
            feeTokenId = fixtureCoin.id,
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN a smart gas token picked WHEN the extended fee is loaded THEN the refund is quoted in it`() = runTest {
        // Arrange
        val gasToken = SafeGasToken.Token(
            address = fixtureUsdc.contractAddress,
            decimals = fixtureUsdc.decimals,
            coinDecimals = 18,
            nativeToTokenRate = BigDecimal("2000"),
        )
        val safeTx = createSafeTx(gasPrice = BigInteger.valueOf(40))
        coEvery {
            gasTokenResolver.resolve(fixtureUsdc.network, fixtureUsdc.contractAddress, fixtureUsdc.id.rawCurrencyId)
        } returns gasToken.right()
        coEvery { gasBuilder.build(userWalletId, fixtureUsdc.network, any(), gasToken) } returns safeTx.right()

        // Act
        val actual = delegate.loadFeeExtended(
            userWalletId = userWalletId,
            currency = fixtureUsdc,
            feeCoin = fixtureCoin,
            feeCurrency = fixtureUsdc,
            amountUM = createAmountData(crypto = BigDecimal.ONE),
            destinationUM = createDestinationContent(),
        )

        // Assert
        val fee = actual.getOrNull()!!
        assertThat(fee.feeTokenId).isEqualTo(fixtureUsdc.id)
        val tokenFee = fee.transactionFee.normal as Fee.Ethereum.TokenCurrency
        // (150 000 + 110 000) gas × 40 base units = 10.4 USDC
        assertThat(tokenFee.amount.value).isEqualTo(BigDecimal("10.400000"))
        assertThat(tokenFee.amount.currencySymbol).isEqualTo(fixtureUsdc.symbol)
    }

    @Test
    fun `GIVEN a token outside smart gas WHEN the extended fee is loaded THEN a gasless error without a build`() =
        runTest {
            // Arrange
            coEvery { gasTokenResolver.resolve(any(), any(), any()) } returns null.right()

            // Act
            val actual = delegate.loadFeeExtended(
                userWalletId = userWalletId,
                currency = fixtureUsdc,
                feeCoin = fixtureCoin,
                feeCurrency = fixtureUsdc,
                amountUM = createAmountData(crypto = BigDecimal.ONE),
                destinationUM = createDestinationContent(),
            )

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.DataError::class.java)
            coVerify(exactly = 0) { gasBuilder.build(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN a smart gas token picked WHEN the operation is created THEN the use case gets it`() = runTest {
        // Arrange
        val account: Account.Joint = mockk()
        val gasToken = SafeGasToken.Token(
            address = fixtureUsdc.contractAddress,
            decimals = fixtureUsdc.decimals,
            coinDecimals = 18,
            nativeToTokenRate = BigDecimal("2000"),
        )
        coEvery {
            gasTokenResolver.resolve(fixtureUsdc.network, fixtureUsdc.contractAddress, fixtureUsdc.id.rawCurrencyId)
        } returns gasToken.right()
        coEvery { createJointOperationUseCase(account, fixtureUsdc.network, any(), gasToken) } returns Unit.right()

        // Act
        val actual = delegate.createOperation(
            account = account,
            currency = fixtureUsdc,
            amount = BigDecimal.ONE,
            destinationUM = createDestinationContent(),
            feeToken = fixtureUsdc,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { createJointOperationUseCase(account, fixtureUsdc.network, any(), gasToken) }
    }

    @Test
    fun `GIVEN the store holds the account WHEN it is looked up THEN the match ignores the id case`() = runTest {
        // Arrange
        val account: Account.Joint = mockk()
        val accountId: AccountId = mockk()
        every { account.accountId } returns accountId
        every { accountId.value } returns "ABCDEF"
        val stored: JointAccount = mockk()
        every { stored.cryptoAccountId } returns "abcdef"
        every { jointAccountListSupplier(userWalletId) } returns flowOf(listOf(stored))

        // Act
        val actual = delegate.findJointAccount(userWalletId = userWalletId, account = account)

        // Assert
        assertThat(actual).isSameInstanceAs(stored)
    }
}