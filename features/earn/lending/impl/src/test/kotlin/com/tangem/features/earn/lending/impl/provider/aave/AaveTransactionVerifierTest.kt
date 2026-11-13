package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory as CallDataFactory
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.FACTORY
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.LATEST
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.MODULE
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.OLD
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.OWNER
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.coin
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.createTx
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.wrapped
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AaveTransactionVerifierTest {

    private val verifier = AaveTransactionVerifier()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN transactions WHEN verify THEN accepted only when they match the expectation`(model: VerifyModel) {
        // Act
        val actual = verifier.verify(
            txs = model.txs,
            expected = model.expected,
            addresses = addresses,
            read = model.read,
        )

        // Assert
        assertThat(actual).isEqualTo(model.result)
    }

    @Test
    fun `GIVEN valid deposit with address in another case WHEN verify THEN accepted`() {
        // Arrange
        val txs = listOf(deposit(destination = MODULE.uppercase().replace("0X", "0x")))

        // Act
        val actual = verifier.verify(txs, depositOf(DEPOSIT), addresses, ready)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
    }

    data class VerifyModel(
        val name: String,
        val txs: List<TransactionData.Uncompiled>,
        val expected: LendingTxExpectation,
        val read: AaveModuleRead,
        val result: Either<LendingTxError, Unit>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels(): List<VerifyModel> = validModels() + depositModels() + preparationModels() +
        upgradeModels() + withdrawModels()

    private fun validModels() = listOf(
        valid("deposit ready", listOf(deposit()), depositOf(DEPOSIT), ready),
        valid("deposit with upgrade", listOf(deposit(callData = wrapped(enter))), depositOf(DEPOSIT), outdated),
        valid("deploy and deposit", listOf(prepare(deploy, FACTORY), deposit()), depositOf(DEPOSIT), notDeployed),
        valid("initToken and deposit", listOf(prepare(initToken), deposit()), depositOf(DEPOSIT), needsInit),
        valid(
            name = "upgraded initToken and deposit",
            txs = listOf(prepare(wrapped(initToken)), deposit()),
            expected = depositOf(DEPOSIT),
            read = needsInitOutdated,
        ),
        valid(
            name = "reactivate and deposit",
            txs = listOf(prepare(reactivate), deposit()),
            expected = depositOf(DEPOSIT),
            read = needsReactivation,
        ),
        valid(
            name = "upgraded reactivate and deposit",
            txs = listOf(prepare(wrapped(reactivate)), deposit()),
            expected = depositOf(DEPOSIT),
            read = needsReactivationOutdated,
        ),
        valid(
            name = "deposit with trailing zeros",
            txs = listOf(deposit(value = BigDecimal("1.00"))),
            expected = depositOf(BigDecimal("1.0")),
            read = ready,
        ),
        valid("withdraw partial", listOf(withdraw(withdrawNative(WITHDRAW))), withdrawOf(isEntire = false), ready),
        valid("withdraw entire", listOf(withdraw(exit)), withdrawOf(isEntire = true), ready),
        valid("withdraw with upgrade", listOf(withdraw(wrapped(exit))), withdrawOf(isEntire = true), outdated),
        valid(
            name = "partial withdraw with upgrade",
            txs = listOf(withdraw(wrapped(withdrawNative(WITHDRAW)))),
            expected = withdrawOf(isEntire = false),
            read = outdated,
        ),
    )

    private fun depositModels() = listOf(
        invalid(
            name = "deposit amount differs",
            txs = listOf(deposit(value = DEPOSIT + BigDecimal.ONE)),
            reason = "Deposit amount differs",
        ),
        invalid(
            name = "deposit to another recipient",
            txs = listOf(deposit(destination = OTHER)),
            reason = "Deposit goes to another address",
        ),
        invalid(
            name = "token enter",
            txs = listOf(deposit(callData = CallDataFactory.getEnterCallData(WETH))),
            reason = "Deposit is not a native supply",
        ),
        invalid(
            name = "erc20 approve",
            txs = listOf(deposit(callData = approve)),
            reason = "Deposit is not a native supply",
        ),
        invalid(
            name = "token typed deposit",
            txs = listOf(deposit().copy(amount = tokenAmount(DEPOSIT))),
            reason = "Amount is not a native coin",
        ),
        invalid(
            name = "wrong decimals deposit",
            txs = listOf(deposit().copy(amount = coin(DEPOSIT).copy(decimals = 6))),
            reason = "Amount is not a native coin",
        ),
        invalid(
            name = "deposit from another sender",
            txs = listOf(deposit().copy(sourceAddress = OTHER)),
            reason = "Transaction is sent by another address",
        ),
        invalid(
            name = "preparation from another sender",
            txs = listOf(prepare(initToken).copy(sourceAddress = OTHER), deposit()),
            read = needsInit,
            reason = "Transaction is sent by another address",
        ),
        invalid(name = "no call data", txs = listOf(deposit(callData = null)), reason = "Transaction has no call data"),
        invalid(name = "no transactions", txs = emptyList(), reason = "Unexpected number of transactions: 0"),
        invalid(
            name = "deposit when the module cannot be upgraded",
            txs = listOf(deposit()),
            read = read(YieldModuleVersionStatus.UpgradeUnavailable(OLD), readyStatus),
            reason = "Module is not ready for a deposit",
        ),
        invalid(
            name = "deposit when the reserve status is unknown",
            txs = listOf(deposit()),
            read = read(YieldModuleVersionStatus.UpToDate, status = null),
            reason = "Module is not ready for a deposit",
        ),
    )

    private fun preparationModels() = listOf(
        invalid(
            name = "preparation carries a value",
            txs = listOf(prepare(initToken, value = BigDecimal.ONE), deposit()),
            read = needsInit,
            reason = "Preparation carries a value",
        ),
        invalid(
            name = "preparation to another recipient",
            txs = listOf(prepare(initToken, OTHER), deposit()),
            read = needsInit,
            reason = "Preparation goes to another address",
        ),
        invalid(
            name = "deploy sent to the module",
            txs = listOf(prepare(deploy, MODULE), deposit()),
            read = notDeployed,
            reason = "Deploy goes to another address",
        ),
        invalid(
            name = "deploy for another owner",
            txs = listOf(prepare(deployFor(OTHER), FACTORY), deposit()),
            read = notDeployed,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "wrapped deploy",
            txs = listOf(prepare(wrapped(deploy), FACTORY), deposit()),
            read = notDeployed,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "initToken of another token",
            txs = listOf(prepare(CallDataFactory.getInitTokenCallData(OTHER, coin())), deposit()),
            read = needsInit,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "approve in the preparation slot",
            txs = listOf(prepare(approve), deposit()),
            read = needsInit,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "three transactions",
            txs = listOf(prepare(initToken), prepare(initToken), deposit()),
            read = needsInit,
            reason = "Unexpected number of transactions: 3",
        ),
        invalid(
            name = "init when the module needs reactivation",
            txs = listOf(prepare(initToken), deposit()),
            read = needsReactivation,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "reactivate when the module needs init",
            txs = listOf(prepare(reactivate), deposit()),
            read = needsInit,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "deploy when the module needs init",
            txs = listOf(prepare(deploy, FACTORY), deposit()),
            read = needsInit,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "init when the module is not deployed",
            txs = listOf(prepare(initToken), deposit()),
            read = notDeployed,
            reason = "Unexpected preparation call",
        ),
        invalid(
            name = "preparation when the module is ready",
            txs = listOf(prepare(initToken), deposit()),
            read = ready,
            reason = "Unexpected number of transactions: 2",
        ),
        invalid(
            name = "deploy when the module is outdated and ready",
            txs = listOf(prepare(deploy, FACTORY), deposit()),
            read = outdated,
            reason = "Unexpected number of transactions: 2",
        ),
        invalid(
            name = "no preparation when the module is not deployed",
            txs = listOf(deposit()),
            read = notDeployed,
            reason = "Unexpected number of transactions: 1",
        ),
    )

    private fun upgradeModels() = listOf(
        invalid(
            name = "upgrade to another implementation",
            txs = listOf(deposit(callData = wrapped(enter, implementation = OTHER))),
            read = outdated,
            reason = "Upgrade to an unexpected implementation",
        ),
        invalid(
            name = "upgrade when up to date",
            txs = listOf(deposit(callData = wrapped(enter))),
            reason = "Upgrade is not expected here",
        ),
        invalid(
            name = "upgrade wrapping an unexpected call",
            txs = listOf(deposit(callData = wrapped(CallDataFactory.getEnterCallData(WETH)))),
            read = outdated,
            reason = "Deposit is not a native supply",
        ),
        invalid(
            name = "approve wrapped inside upgradeToAndCall",
            txs = listOf(deposit(callData = wrapped(approve))),
            read = outdated,
            reason = "Deposit is not a native supply",
        ),
        invalid(
            name = "upgraded deposit after an upgraded preparation",
            txs = listOf(prepare(wrapped(initToken)), deposit(callData = wrapped(enter))),
            read = needsInitOutdated,
            reason = "Upgrade is not expected here",
        ),
        invalid(
            name = "bare deposit when outdated",
            txs = listOf(deposit()),
            read = outdated,
            reason = "Module upgrade is missing",
        ),
        invalid(
            name = "bare initToken when outdated",
            txs = listOf(prepare(initToken), deposit()),
            read = needsInitOutdated,
            reason = "Module upgrade is missing",
        ),
        invalid(
            name = "bare reactivate when outdated",
            txs = listOf(prepare(reactivate), deposit()),
            read = needsReactivationOutdated,
            reason = "Module upgrade is missing",
        ),
    )

    private fun withdrawModels() = listOf(
        invalid(
            name = "wrong decimals withdraw",
            txs = listOf(withdraw(exit).copy(amount = coin().copy(decimals = 6))),
            expected = withdrawOf(isEntire = true),
            reason = "Amount is not a native coin",
        ),
        invalid(
            name = "withdraw entire with partial call",
            txs = listOf(withdraw(withdrawNative(WITHDRAW))),
            expected = withdrawOf(isEntire = true),
            reason = "Unexpected withdrawal call",
        ),
        invalid(
            name = "withdraw partial with another amount",
            txs = listOf(withdraw(withdrawNative(DEPOSIT))),
            expected = withdrawOf(isEntire = false),
            reason = "Unexpected withdrawal call",
        ),
        invalid(
            name = "withdraw to another recipient",
            txs = listOf(withdraw(exit, destination = OTHER)),
            expected = withdrawOf(isEntire = true),
            reason = "Withdrawal goes to another address",
        ),
        invalid(
            name = "withdraw from another sender",
            txs = listOf(withdraw(exit).copy(sourceAddress = OTHER)),
            expected = withdrawOf(isEntire = true),
            reason = "Transaction is sent by another address",
        ),
        invalid(
            name = "withdraw carries a value",
            txs = listOf(withdraw(exit, value = BigDecimal.ONE)),
            expected = withdrawOf(isEntire = true),
            reason = "Withdrawal carries a value",
        ),
        invalid(
            name = "withdraw of two transactions",
            txs = listOf(withdraw(exit), withdraw(exit)),
            expected = withdrawOf(isEntire = true),
            reason = "Unexpected number of transactions: 2",
        ),
        invalid(
            name = "withdraw with a deposit call",
            txs = listOf(withdraw(enter)),
            expected = withdrawOf(isEntire = false),
            reason = "Unexpected withdrawal call",
        ),
        invalid(
            name = "bare withdraw entire when outdated",
            txs = listOf(withdraw(exit)),
            expected = withdrawOf(isEntire = true),
            read = outdated,
            reason = "Module upgrade is missing",
        ),
        invalid(
            name = "bare withdraw partial when outdated",
            txs = listOf(withdraw(withdrawNative(WITHDRAW))),
            expected = withdrawOf(isEntire = false),
            read = outdated,
            reason = "Module upgrade is missing",
        ),
        invalid(
            name = "withdraw when the module cannot be upgraded",
            txs = listOf(withdraw(exit)),
            expected = withdrawOf(isEntire = true),
            read = read(YieldModuleVersionStatus.UpgradeUnavailable(OLD), readyStatus),
            reason = "Module is not ready for a withdrawal",
        ),
        invalid(
            name = "withdraw when the module version is unknown",
            txs = listOf(withdraw(withdrawNative(WITHDRAW))),
            expected = withdrawOf(isEntire = false),
            read = read(YieldModuleVersionStatus.Indeterminate(reason = "rpc"), readyStatus),
            reason = "Module is not ready for a withdrawal",
        ),
    )

    private companion object {
        const val WETH = "0x3333333333333333333333333333333333333333"
        const val OTHER = "0x5555555555555555555555555555555555555555"
        val DEPOSIT = BigDecimal("0.5")
        val WITHDRAW = BigDecimal("0.25")

        val addresses = AaveVerifiedAddresses(MODULE, FACTORY, WETH, Blockchain.Ethereum)

        val outdatedVersion = YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD,
            latestImplementation = LATEST,
        )
        val readyStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH)
        val needsInitStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH, isInitialized = false, isActive = false)
        val inactiveStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH, isActive = false)

        val ready = read(YieldModuleVersionStatus.UpToDate, readyStatus)
        val outdated = read(outdatedVersion, readyStatus)
        val notDeployed = read(YieldModuleVersionStatus.NotDeployed, status = null)
        val needsInit = read(YieldModuleVersionStatus.UpToDate, needsInitStatus)
        val needsInitOutdated = read(outdatedVersion, needsInitStatus)
        val needsReactivation = read(YieldModuleVersionStatus.UpToDate, inactiveStatus)
        val needsReactivationOutdated = read(outdatedVersion, inactiveStatus)

        val enter = CallDataFactory.getEnterWithNativeCallData()
        val exit = CallDataFactory.getExitNativeCallData()
        val deploy = deployFor(OWNER)
        val initToken = CallDataFactory.getInitTokenCallData(WETH, coin())
        val reactivate = CallDataFactory.getReactivateTokenCallData(WETH, coin())
        val approve = ApprovalERC20TokenCallData(spenderAddress = OTHER, amount = null)

        fun read(version: YieldModuleVersionStatus, status: YieldModuleStatus?) =
            AaveModuleRead(status = status, version = version, withdrawLiquidity = null)

        fun tokenAmount(value: BigDecimal) = Amount(
            currencySymbol = "WETH",
            value = value,
            decimals = 18,
            type = AmountType.Token(Token(symbol = "WETH", contractAddress = WETH, decimals = 18)),
        )

        fun deployFor(owner: String) = CallDataFactory.getDeployCallData(owner, WETH, coin())

        fun withdrawNative(value: BigDecimal) = CallDataFactory.getWithdrawNativeCallData(coin(value))

        fun depositOf(amount: BigDecimal = DEPOSIT) = LendingTxExpectation.Deposit(OWNER, amount)

        fun withdrawOf(isEntire: Boolean) = LendingTxExpectation.Withdraw(OWNER, WITHDRAW, isEntire)

        fun deposit(
            callData: SmartContractCallData? = enter,
            destination: String = MODULE,
            value: BigDecimal = DEPOSIT,
        ) = createTx(callData = callData, destination = destination, value = value)

        fun prepare(
            callData: SmartContractCallData,
            destination: String = MODULE,
            value: BigDecimal = BigDecimal.ZERO,
        ) = createTx(callData = callData, destination = destination, value = value)

        fun withdraw(
            callData: SmartContractCallData,
            destination: String = MODULE,
            value: BigDecimal = BigDecimal.ZERO,
        ) = createTx(callData = callData, destination = destination, value = value)

        fun valid(
            name: String,
            txs: List<TransactionData.Uncompiled>,
            expected: LendingTxExpectation,
            read: AaveModuleRead,
        ) = VerifyModel(name, txs, expected, read, result = Unit.right())

        fun invalid(
            name: String,
            txs: List<TransactionData.Uncompiled>,
            reason: String,
            expected: LendingTxExpectation = depositOf(),
            read: AaveModuleRead = ready,
        ) = VerifyModel(name, txs, expected, read, result = LendingTxError.Unverified(reason).left())
    }
}