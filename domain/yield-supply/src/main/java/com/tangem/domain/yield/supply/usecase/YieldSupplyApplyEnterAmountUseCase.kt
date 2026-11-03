package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Makes a Yield Mode enter batch leave the gasless fee on the wallet.
 *
 * The plain `enterProtocolByOwner(token)` moves the whole liquid balance into the module, so a fee paid in the same
 * token could no longer be collected. This replaces that call with `enterProtocolByOwner(token, liquid − feeAmount)`.
 *
 * The enter call is rewritten in place rather than rebuilt: its destination, amount, fee and gas stay as they are, and
 * an `upgradeToAndCall` wrapper keeps its implementation around the new inner call, so the upgrade decision made when
 * the batch was built is not re-derived.
 *
 * `enterProtocolByOwner(token, amount)` exists only in recent module implementations, so the batch must end up on one:
 * it deploys a new module, upgrades the existing one in one of its calls, or the module is already up to date.
 */
class YieldSupplyApplyEnterAmountUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    /**
     * @param calls the enter batch; its last call must be the enter call for [cryptoCurrencyStatus]'s token.
     * @param feeAmount the gasless `maxTokenFee`, in token units, that must stay on the wallet.
     * @return [calls] with the enter call replaced. Left means this batch cannot use Smart Gas paid in the same token:
     * the enter call is missing or not last, the module may lack the amount-taking enter, the module balance cannot
     * be read, or nothing would be left to enter after the fee. The caller must not fall back to the plain enter with
     * a token fee — it would move the fee into the module and the batch would fail.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        calls: List<TransactionData.Uncompiled>,
        feeAmount: BigDecimal,
    ): Either<Throwable, List<TransactionData.Uncompiled>> = either {
        val token = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
        ensureNotNull(token) { IllegalArgumentException("Yield Mode is entered with a token only") }

        val notEnterError = { IllegalStateException("The last call of the batch is not the ${token.symbol} enter") }
        val enterTransaction = ensureNotNull(calls.lastOrNull(), notEnterError)
        val enterExtras = ensureNotNull(enterTransaction.extras as? EthereumTransactionExtras, notEnterError)
        val enterCallData = ensureNotNull(enterExtras.callData, notEnterError)
        val existingEnter = ensureNotNull(enterCallData.findEnterCallData(), notEnterError)
        ensure(existingEnter.tokenContractAddress.equals(token.contractAddress, ignoreCase = true), notEnterError)

        ensureModuleSupportsEnterAmount(userWalletId, token, calls).bind()

        val moduleBalance = resolveModuleBalance(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            token = token,
            calls = calls,
        ).bind()
        val liquidBalance = (cryptoCurrencyStatus.value.amount.orZero() - moduleBalance).coerceAtLeast(BigDecimal.ZERO)
        val enterAmount = (liquidBalance - feeAmount)
            .movePointRight(token.decimals)
            .setScale(0, RoundingMode.FLOOR)
            .toBigInteger()
        ensure(enterAmount > BigInteger.ZERO) {
            IllegalStateException("Nothing left to enter after the fee of $feeAmount ${token.symbol}")
        }

        val newEnterCallData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(
            tokenContractAddress = token.contractAddress,
            amount = enterAmount,
        )
        val newEnterTransaction = enterTransaction.copy(
            extras = enterExtras.copy(callData = enterCallData.replaceEnterCallData(newEnterCallData)),
        )

        calls.dropLast(1) + newEnterTransaction
    }

    private suspend fun ensureModuleSupportsEnterAmount(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
        calls: List<TransactionData.Uncompiled>,
    ): Either<Throwable, Unit> {
        val batchCallData = calls.mapNotNull { (it.extras as? EthereumTransactionExtras)?.callData }
        val isModuleDeployedOrUpgraded = batchCallData.any {
            it is EthereumYieldSupplyDeployCallData || it is EthereumYieldSupplyUpgradeToAndCallCallData
        }
        if (isModuleDeployedOrUpgraded) return Either.Right(Unit)

        return Either.catch {
            yieldSupplyTransactionRepository.getYieldModuleVersionStatus(userWalletId, token.network)
        }.flatMap { versionStatus ->
            if (versionStatus is YieldModuleVersionStatus.UpToDate) {
                Either.Right(Unit)
            } else {
                Either.Left(IllegalStateException("The ${token.symbol} yield module may lack the enter with amount"))
            }
        }
    }

    private suspend fun resolveModuleBalance(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        token: CryptoCurrency.Token,
        calls: List<TransactionData.Uncompiled>,
    ): Either<Throwable, BigDecimal> {
        // The module can't read the balance of a token it hasn't registered yet (the call reverts), and a token
        // registered by this very batch holds nothing in it.
        if (calls.registersToken(token)) return Either.Right(BigDecimal.ZERO)

        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus

        yieldSupplyStatus?.effectiveProtocolBalance?.let { return Either.Right(it) }
        if (yieldSupplyStatus?.isInitialized == false) return Either.Right(BigDecimal.ZERO)

        // Unlike the fee plan, an unreadable module balance is not assumed empty here: that would count the module's
        // funds as liquid and enter more than the wallet holds, reverting the batch.
        return Either.catch {
            yieldSupplyTransactionRepository.getEffectiveProtocolBalance(userWalletId, token)
                ?: error("The ${token.symbol} yield module balance is unavailable")
        }
    }

    private fun List<TransactionData.Uncompiled>.registersToken(token: CryptoCurrency.Token): Boolean = any { call ->
        when (val callData = (call.extras as? EthereumTransactionExtras)?.callData?.unwrapUpgrade()) {
            is EthereumYieldSupplyDeployCallData -> true
            is EthereumYieldSupplyInitTokenCallData ->
                callData.tokenContractAddress.equals(token.contractAddress, ignoreCase = true)
            else -> false
        }
    }

    private fun SmartContractCallData.unwrapUpgrade(): SmartContractCallData =
        (this as? EthereumYieldSupplyUpgradeToAndCallCallData)?.innerCallData ?: this

    private fun SmartContractCallData.findEnterCallData(): EthereumYieldSupplyEnterCallData? = when (this) {
        is EthereumYieldSupplyEnterCallData -> this
        is EthereumYieldSupplyUpgradeToAndCallCallData -> innerCallData as? EthereumYieldSupplyEnterCallData
        else -> null
    }

    private fun SmartContractCallData.replaceEnterCallData(newEnter: SmartContractCallData): SmartContractCallData =
        when (this) {
            is EthereumYieldSupplyUpgradeToAndCallCallData -> EthereumYieldSupplyUpgradeToAndCallCallData(
                newImplementation = newImplementation,
                innerCallData = newEnter,
            )
            else -> newEnter
        }
}