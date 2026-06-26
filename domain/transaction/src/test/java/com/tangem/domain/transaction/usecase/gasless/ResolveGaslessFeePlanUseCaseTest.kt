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
import java.math.RoundingMode

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

    // ─── Case 2: yield-active with no liquid → the whole fee is withdrawn from the module ──

    @Test
    fun `yield active with no liquid withdraws the whole fee`() = runTest {
        val decimals = 6
        // value.amount is effectiveBalance = liquid(EOA) + effectiveProtocolBalance. Here total == module
        // balance (20), so liquid is 0 and the entire fee must be withdrawn from the module — the plan must
        // not short-circuit to TokenPay.
        // withdraw == feeAmount, CEILING-rounded: 10000000.5 → 10000001 (floor would give 10000000).
        val feeAmount = BigDecimal("10.0000005")
        val moduleBalance = BigDecimal("20")
        val expectedWithdrawAmount = feeAmount
            .movePointRight(decimals)
            .setScale(0, RoundingMode.CEILING)
            .toBigInteger()
        val floorAmount = feeAmount.movePointRight(decimals).toBigInteger() // 10000000
        assertThat(expectedWithdrawAmount).isGreaterThan(floorAmount)

        // value.amount == module balance → liquid is 0, so the fee cannot be paid from the EOA (no TokenPay).
        val tokenStatus = tokenStatus(plainBalance = moduleBalance, decimals = decimals)
        val tokenFee = tokenFee(feeAmount = feeAmount, decimals = decimals)
        val mockCallData = mockk<SmartContractCallData>(relaxed = true)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns moduleBalance

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
        // Must be 10000001 (CEILING of the fee), not the module balance and not floor.
        assertThat(plan!!.withdrawAmount).isEqualTo(expectedWithdrawAmount)
        assertThat(plan.withdrawAmount).isEqualTo(BigInteger.valueOf(10_000_001))
        assertThat(plan.yieldModuleAddress).isEqualTo("0xmodule")
        assertThat(plan.withdrawCallData).isEqualTo(mockCallData)
    }

    // ─── Case 2b: send amount counts toward sufficiency but NOT toward the withdraw ────────────

    @Test
    fun `yield active withdraw covers only the fee not the send amount`() = runTest {
        val decimals = 6
        // The main module.send tx moves the send amount from the module itself, so the fee-withdraw must
        // cover ONLY the fee. Including the send amount would withdraw it twice and overdraw the module.
        val feeAmount = BigDecimal("3.0")
        val sendAmountInFeeToken = BigDecimal("1.5")
        val moduleBalance = BigDecimal("5.0") // covers required = fee(3.0) + send(1.5) = 4.5 ✓
        val expectedWithdrawAmount = feeAmount
            .movePointRight(decimals)
            .setScale(0, RoundingMode.CEILING)
            .toBigInteger() // 3000000 — the FEE only, NOT 4.5

        val tokenStatus = tokenStatus(plainBalance = moduleBalance, decimals = decimals)
        val tokenFee = tokenFee(feeAmount = feeAmount, decimals = decimals)
        val mockCallData = mockk<SmartContractCallData>(relaxed = true)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns moduleBalance

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
            sendAmountInFeeToken = sendAmountInFeeToken,
        )

        assertThat(result.isRight()).isTrue()
        val plan = result.getOrNull() as? GaslessFeePlan.TokenPayWithYieldWithdraw
        assertThat(plan).isNotNull()
        assertThat(plan!!.withdrawAmount).isEqualTo(expectedWithdrawAmount)
        assertThat(plan.withdrawAmount).isEqualTo(BigInteger.valueOf(3_000_000))
        assertThat(plan.yieldModuleAddress).isEqualTo("0xmodule")
        assertThat(plan.withdrawCallData).isEqualTo(mockCallData)
    }

    // ─── Case 2c: module cannot cover send + fee → NotEnoughFunds ──────────────

    @Test
    fun `yield active module cannot cover send plus fee returns NotEnoughFunds`() = runTest {
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("4"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("3"), decimals = 6)

        // required = fee(3) + send(1.5) = 4.5, but the module holds only 4.0
        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("4.0")

        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal("1.5"),
        )

        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.NotEnoughFunds::class.java)
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
        // total(10) covers the fee(5) and liquid(0) does not, so the flow reaches the module withdraw.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6)
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
        // total(6) = liquid(1) + module(5) < fee(10) → not enough funds anywhere.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("6"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("10"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("5") // liquid 1 + module 5 = 6 < 10

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
        // total(10) covers the fee(5) and liquid(0) does not, so the flow reaches the module withdraw.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6)
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

    // ─── Case 7: liquid EOA balance covers most of send+fee, protocol alone does not ──────────────

    @Test
    fun `GIVEN liquid covers send but protocol alone does not WHEN yield active THEN TokenPayWithYieldWithdraw`() =
        runTest {
            // value.amount is effectiveBalance (liquid EOA + effectiveProtocolBalance). The user sends 3.00 of
            // 3.585624 total. The yield module (effectiveProtocolBalance) holds only 0.6, the rest (2.985624)
            // is liquid on the EOA. required = send(3.00) + fee(0.05) = 3.05 < total(3.585624), so funds ARE
            // sufficient. The old check compared the module balance (0.6) against required and wrongly raised
            // NotEnoughFunds.
            val decimals = 6
            val totalBalance = BigDecimal("3.585624")
            val moduleBalance = BigDecimal("0.6")
            val feeAmount = BigDecimal("0.05")
            val sendAmount = BigDecimal("3.00")
            // module.send consumes EOA liquid first, leaving 0 for the fee, so the whole fee must be withdrawn.
            val expectedWithdrawAmount = feeAmount
                .movePointRight(decimals)
                .setScale(0, RoundingMode.CEILING)
                .toBigInteger()

            val tokenStatus = tokenStatus(plainBalance = totalBalance, decimals = decimals)
            val tokenFee = tokenFee(feeAmount = feeAmount, decimals = decimals)
            val mockCallData = mockk<SmartContractCallData>(relaxed = true)

            coEvery {
                gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
            } returns moduleBalance
            coEvery {
                gaslessYieldRepository.createPartialWithdrawCallData(mockUserWalletId, any(), any())
            } returns mockCallData
            coEvery {
                gaslessYieldRepository.getYieldContractAddress(mockUserWalletId, any())
            } returns "0xmodule"

            // Act
            val result = useCase(
                userWallet = mockUserWallet,
                tokenStatus = tokenStatus,
                tokenFee = tokenFee,
                isYieldActive = true,
                sendAmountInFeeToken = sendAmount,
            )

            // Assert
            assertThat(result.isRight()).isTrue()
            val plan = result.getOrNull() as? GaslessFeePlan.TokenPayWithYieldWithdraw
            assertThat(plan).isNotNull()
            assertThat(plan!!.withdrawAmount).isEqualTo(expectedWithdrawAmount)
        }

    // ─── Case 8: liquid EOA balance alone covers send + fee → no withdraw needed ───────────────────

    @Test
    fun `GIVEN liquid covers send plus fee WHEN yield active THEN TokenPay without withdraw`() = runTest {
        // Arrange — liquid = total(10) - module(2) = 8, which already covers required = send(3) + fee(1) = 4.
        // The EOA holds enough after the main send to settle the fee, so no yield withdraw is needed.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("1"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("2")

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal("3"),
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
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