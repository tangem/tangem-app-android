package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessFeePlan
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [ResolveGaslessFeePlanUseCase].
 * Covers every branch of the gasless fee decision tree.
 */
internal class ResolveGaslessFeePlanUseCaseTest {

    private lateinit var gaslessYieldRepository: GaslessYieldRepository
    private lateinit var useCase: ResolveGaslessFeePlanUseCase

    private val mockUserWalletId: UserWalletId = mockk(relaxed = true)
    private val mockUserWallet: UserWallet = mockk<UserWallet.Hot>().also {
        every { it.walletId } returns mockUserWalletId
    }

    @BeforeEach
    fun setup() {
        gaslessYieldRepository = mockk()
        useCase = ResolveGaslessFeePlanUseCase(gaslessYieldRepository)
    }

    // ─── Case 1: plain balance >= required → TokenPay ──────────────────────────

    @Test
    fun `plain balance covers fee returns TokenPay`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("5"), decimals = 6)

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isRight()).isTrue()
        val plan = result.getOrNull()
        assertThat(plan).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
        assertThat((plan as GaslessFeePlan.TokenPay).fee).isEqualTo(tokenFee)
    }

    @Test
    fun `plain balance equals required returns TokenPay`() = runTest {
        val amount = BigDecimal("5")
        val tokenStatus = tokenStatus(plainBalance = amount, decimals = 6)
        val tokenFee = tokenFee(feeAmount = amount, decimals = 6)

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
    }

    // ─── Case 2: yield covers shortfall → TokenPayWithYieldWithdraw ────────────

    @Test
    fun `yield covers shortfall returns TokenPayWithYieldWithdraw with correct withdrawAmount`() = runTest {
        val decimals = 6
        val feeAmount = BigDecimal("10")
        val plainBalance = BigDecimal("3")
        // required = 10, plainBalance = 3, shortfall = 7
        val yieldBalance = BigDecimal("8") // 3 + 8 >= 10 ✓
        val expectedWithdrawDecimal = feeAmount - plainBalance // 7
        val expectedWithdrawAmount = expectedWithdrawDecimal.movePointRight(decimals).toBigInteger()

        val tokenStatus = tokenStatus(plainBalance = plainBalance, decimals = decimals)
        val tokenFee = tokenFee(feeAmount = feeAmount, decimals = decimals)
        val mockCallData = mockk<SmartContractCallData>(relaxed = true)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns yieldBalance

        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(
                userWalletId = mockUserWalletId,
                cryptoCurrency = any(),
                amount = any(),
            )
        } returns mockCallData

        coEvery {
            gaslessYieldRepository.getYieldContractAddress(mockUserWalletId, any())
        } returns "0xmodule"

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isRight()).isTrue()
        val plan = result.getOrNull() as? GaslessFeePlan.TokenPayWithYieldWithdraw
        assertThat(plan).isNotNull()
        assertThat(plan!!.withdrawAmount).isEqualTo(expectedWithdrawAmount)
        assertThat(plan.yieldModuleAddress).isEqualTo("0xmodule")
        assertThat(plan.withdrawCallData).isEqualTo(mockCallData)
    }

    // ─── Case 3: plain insufficient, isYieldActive=false → NotEnoughFunds ──────

    @Test
    fun `plain insufficient yield inactive returns NotEnoughFunds`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("1"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("5"), decimals = 6)

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.NotEnoughFunds::class.java)
    }

    // ─── Case 4: YieldModuleUpgradeUnavailableException → ModuleUpdateUnavailable

    @Test
    fun `createPartialWithdrawCallData throws UpgradeUnavailableException returns ModuleUpdateUnavailable`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("1"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("5"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("10")

        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        } throws YieldModuleUpgradeUnavailableException("0xold")

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.ModuleUpdateUnavailable::class.java)
    }

    // ─── Case 5: plain + yield < required → NotEnoughFunds ─────────────────────

    @Test
    fun `plain plus yield insufficient returns NotEnoughFunds`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("1"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("10"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("5") // 1 + 5 = 6 < 10

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.NotEnoughFunds::class.java)
    }

    // ─── Case 6: YieldModuleVersionIndeterminateException → ModuleUpdateUnavailable

    @Test
    fun `createPartialWithdrawCallData throws VersionIndeterminateException returns ModuleUpdateUnavailable`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("1"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("5"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("10")

        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        } throws YieldModuleVersionIndeterminateException("rpc error")

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.ModuleUpdateUnavailable::class.java)
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private fun tokenStatus(
        plainBalance: BigDecimal = BigDecimal("100"),
        decimals: Int = 6,
    ): CryptoCurrencyStatus {
        val token = mockk<CryptoCurrency.Token>(relaxed = true)
        every { token.symbol } returns "USDC"
        every { token.contractAddress } returns "0xUSDC"
        every { token.decimals } returns decimals

        val status = mockk<CryptoCurrencyStatus>()
        every { status.currency } returns token
        every { status.value.amount } returns plainBalance

        return status
    }

    private fun tokenFee(feeAmount: BigDecimal, decimals: Int = 6): Fee.Ethereum.TokenCurrency {
        val blockchainToken = Token(symbol = "USDC", contractAddress = "0xUSDC", decimals = decimals)
        val amount = Amount(token = blockchainToken, value = feeAmount)
        return Fee.Ethereum.TokenCurrency(
            amount = amount,
            gasLimit = BigInteger("100000"),
            coinPriceInToken = BigInteger("2000000000"),
            feeTransferGasLimit = BigInteger("60000"),
            baseGas = BigInteger("21000"),
        )
    }
}