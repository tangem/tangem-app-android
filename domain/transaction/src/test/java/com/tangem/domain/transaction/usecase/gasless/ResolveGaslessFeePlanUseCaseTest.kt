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
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessFeePlan
import io.mockk.coEvery
import io.mockk.coVerify
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
 *
 * The fee must be charged against the liquid balance on the address, never against the part
 * of the effective balance that sits inside the yield module.
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
        // Default: the chain knows of no module either. Cases that care override this.
        coEvery { gaslessYieldRepository.getEffectiveProtocolBalance(any(), any()) } returns null
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
    fun `createPartialWithdrawCallData throws VersionIndeterminateException returns ModuleUpdateUnavailable`() =
        runTest {
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

    // ─── Case 9: yield flag lost while the funds sit in the module → NotEnoughFunds ────────────────

    @Test
    fun `GIVEN funds are in the module WHEN yield flag is lost THEN NotEnoughFunds`() = runTest {
        // Arrange — reproduces the failed on-chain Send: the EOA holds nothing, 30.485747 USDC sit in the
        // module, yet isYieldActive arrived false (status not Loaded / toggle off / statuses missing). The
        // old check compared the effective balance (30.485747) against required (1.039923), answered
        // "enough" and produced a TokenPay plan — a single transaction whose fee transfer reverted with
        // "available 0, required 16058".
        val moduleBalance = BigDecimal("30.485747")
        val tokenStatus = tokenStatus(
            plainBalance = moduleBalance,
            decimals = 6,
            statusModuleBalance = moduleBalance,
        )
        val tokenFee = tokenFee(feeAmount = BigDecimal("0.039923"), decimals = 6)

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal("1.0"),
        )

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.NotEnoughFunds::class.java)
    }

    @Test
    fun `GIVEN status has no yield info WHEN yield inactive THEN the module is read from chain`() = runTest {
        // Arrange — the status carries no yieldSupplyStatus at all (non-Loaded status / toggle off), which is
        // exactly what makes the caller pass isYieldActive = false. The status cannot rule a module out, so
        // the balance must be established from the chain: it holds everything, leaving nothing liquid.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("30"), decimals = 6, statusModuleBalance = null)
        val tokenFee = tokenFee(feeAmount = BigDecimal("1"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("30")

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal("3"),
        )

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isInstanceOf(GetFeeError.GaslessError.NotEnoughFunds::class.java)
    }

    @Test
    fun `GIVEN module was never initialized WHEN yield inactive THEN whole balance counts as liquid`() = runTest {
        // Arrange — a plain gasless token with no module: the status says so, so no chain call is needed and
        // the pre-existing behaviour must be preserved.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6, isInitialized = false)
        val tokenFee = tokenFee(feeAmount = BigDecimal("1"), decimals = 6)

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal("3"),
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
        coVerify(exactly = 0) { gaslessYieldRepository.getEffectiveProtocolBalance(any(), any()) }
    }

    @Test
    fun `GIVEN module unreadable WHEN yield inactive THEN falls back to the whole balance`() = runTest {
        // Arrange — a flaky call must not make plain gasless tokens unusable, so a null answer keeps the
        // historical "no module" assumption.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6, statusModuleBalance = null)
        val tokenFee = tokenFee(feeAmount = BigDecimal("1"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns null

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal("3"),
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
    }

    @Test
    fun `GIVEN module read throws WHEN yield inactive THEN falls back to the whole balance`() = runTest {
        // Arrange — the repository throws instead of returning null on an RPC failure, and every plain
        // gasless token reaches this call. An escaping exception would be turned into a DataError with no
        // native-fee fallback, i.e. no fee at all for a token that has nothing to do with yield.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = 6, statusModuleBalance = null)
        val tokenFee = tokenFee(feeAmount = BigDecimal("1"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } throws IllegalStateException("Wallet manager not found")

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = false,
            sendAmountInFeeToken = BigDecimal("3"),
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
    }

    // ─── Case 10: module balance unreadable → no token-fee plan, callers fall back to the native fee ─

    @Test
    fun `GIVEN module balance unavailable WHEN yield active THEN YieldBalanceUnavailable`() = runTest {
        // Arrange — a null balance means "could not read the module", not "the module is empty". Treating it
        // as zero makes the whole effective balance look liquid and yields a TokenPay plan that cannot be
        // settled on chain.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("30"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("0.05"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns null

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal("1.0"),
        )

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.YieldBalanceUnavailable)
    }

    @Test
    fun `GIVEN module balance read throws WHEN yield active THEN YieldBalanceUnavailable`() = runTest {
        // Arrange — the repository declares the balance nullable but signals failure by throwing, so this is
        // the shape the yield path actually meets. It must not be mistaken for an empty module.
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("30"), decimals = 6)
        val tokenFee = tokenFee(feeAmount = BigDecimal("0.05"), decimals = 6)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } throws IllegalStateException("Wallet manager not found")

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal("1.0"),
        )

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.YieldBalanceUnavailable)
    }

    // ─── Case 11: change left on the address must not shrink the withdraw ──────────────────────────

    @Test
    fun `GIVEN change left on the address WHEN yield active THEN withdraws the whole fee`() = runTest {
        // Arrange — reproduces the failed on-chain batch Send. liquid = 1.00725 covers the 1.0 being sent
        // with 0.00725 of change to spare, so the old formula netted that change off the withdraw:
        // 0.012696 - 0.00725 = 0.005446. But the module's send sub-call spends the EOA's liquid balance
        // first, so by the time the fee transfer ran only the withdrawn 5446 were left against 6793 needed.
        val decimals = 6
        val totalBalance = BigDecimal("30.00000")
        val moduleBalance = BigDecimal("28.99275") // liquid = 1.00725
        val feeAmount = BigDecimal("0.012696")
        val sendAmount = BigDecimal("1.0")
        val shrunkWithdraw = BigInteger.valueOf(5_446) // what the old formula produced

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
        assertThat(plan!!.withdrawAmount).isEqualTo(BigInteger.valueOf(12_696))
        assertThat(plan.withdrawAmount).isGreaterThan(shrunkWithdraw)
    }

    @Test
    fun `GIVEN a different token is sent WHEN yield active THEN the liquid balance counts toward the fee`() = runTest {
        // Arrange — sendAmountInFeeToken is zero, so the fee token is not the one being sent: the module's
        // send sub-call never touches it and the liquid balance survives to pay part of the fee.
        // liquid = total(10) - module(9.7) = 0.3 of the 0.5 fee, so only 0.2 needs withdrawing.
        val decimals = 6
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("10"), decimals = decimals)
        val tokenFee = tokenFee(feeAmount = BigDecimal("0.5"), decimals = decimals)
        val mockCallData = mockk<SmartContractCallData>(relaxed = true)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("9.7")
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
            sendAmountInFeeToken = BigDecimal.ZERO,
        )

        // Assert
        val plan = result.getOrNull() as? GaslessFeePlan.TokenPayWithYieldWithdraw
        assertThat(plan).isNotNull()
        assertThat(plan!!.withdrawAmount).isEqualTo(BigInteger.valueOf(200_000))
    }

    @Test
    fun `GIVEN module holds less than the fee WHEN yield active THEN NotEnoughFunds`() = runTest {
        // Arrange — liquid = total(1.9) - module(0.4) = 1.5 falls short of required = send(1.4) +
        // fee(0.5) = 1.9, so the module path is taken. The same token is being sent, so the fee must come
        // out of the module in full, but the module holds only 0.4 of the 0.5 fee. Capping the withdraw
        // at 0.4 would produce a plan whose fee transfer is already known to revert.
        val decimals = 6
        val tokenStatus = tokenStatus(plainBalance = BigDecimal("1.9"), decimals = decimals)
        val tokenFee = tokenFee(feeAmount = BigDecimal("0.5"), decimals = decimals)

        coEvery {
            gaslessYieldRepository.getEffectiveProtocolBalance(mockUserWalletId, any())
        } returns BigDecimal("0.4")

        // Act
        val result = useCase(
            userWallet = mockUserWallet,
            tokenStatus = tokenStatus,
            tokenFee = tokenFee,
            isYieldActive = true,
            sendAmountInFeeToken = BigDecimal("1.4"),
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.NotEnoughFunds)
        coVerify(exactly = 0) {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * @param plainBalance the *effective* balance exposed by the status: liquid EOA + module.
     * @param statusModuleBalance what `yieldSupplyStatus.effectiveProtocolBalance` reports, or null when the
     * status carries no yield info at all (non-Loaded status, toggle off, network did not return it).
     */
    private fun tokenStatus(
        plainBalance: BigDecimal = BigDecimal("100"),
        decimals: Int = 6,
        statusModuleBalance: BigDecimal? = null,
        isInitialized: Boolean? = null,
    ): CryptoCurrencyStatus {
        val token = mockk<CryptoCurrency.Token>(relaxed = true)
        every { token.symbol } returns "USDC"
        every { token.contractAddress } returns "0xUSDC"
        every { token.decimals } returns decimals

        val yieldSupplyStatus = if (statusModuleBalance != null || isInitialized != null) {
            YieldSupplyStatus(
                isActive = statusModuleBalance != null,
                isInitialized = isInitialized != false,
                isAllowedToSpend = true,
                effectiveProtocolBalance = statusModuleBalance,
            )
        } else {
            null
        }

        val status = mockk<CryptoCurrencyStatus>()
        every { status.currency } returns token
        every { status.value.amount } returns plainBalance
        every { status.value.yieldSupplyStatus } returns yieldSupplyStatus

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