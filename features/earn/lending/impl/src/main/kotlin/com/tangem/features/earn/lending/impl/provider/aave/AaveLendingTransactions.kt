package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.fixFee
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.models.YieldModulePreparation
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingProviderState
import com.tangem.features.earn.lending.impl.provider.LendingTransactions
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.utils.coroutines.runSuspendCatching
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

internal class AaveLendingTransactions @Inject constructor(
    private val repository: YieldSupplyTransactionRepository,
    private val estimateFee: YieldSupplyEstimateEnterFeeUseCase,
    private val feeRepository: FeeRepository,
    private val feeErrorResolver: FeeErrorResolver,
    private val verifier: AaveTransactionVerifier,
) : LendingTransactions {

    override suspend fun deposit(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        amount: BigDecimal,
        state: LendingProviderState,
    ): Either<LendingTxError, List<TransactionData.Uncompiled>> = either {
        requireCoin(asset)
        val preparation = when (requireRead(state).toState()) {
            is AaveModuleState.Ready -> null
            AaveModuleState.NotDeployed -> YieldModulePreparation.Deploy
            is AaveModuleState.NeedsInit -> YieldModulePreparation.InitToken
            is AaveModuleState.NeedsReactivation -> YieldModulePreparation.ReactivateToken
            AaveModuleState.UpgradeUnavailable -> raise(LendingTxError.UpdateRequired)
            AaveModuleState.ReadFailed, is AaveModuleState.StatusUnknown -> raise(LendingTxError.ReadFailed)
        }
        val network = asset.currency.network

        if (preparation == null) {
            val depositTx = build {
                repository.createNativeEnterTransaction(
                    userWalletId = userWalletId,
                    network = network,
                    amount = amount,
                    wrapIntoUpgrade = true,
                )
            }
            return@either listOf(depositTx)
        }

        val wrappedToken = ensureNotNull(asset.wrappedToken) { LendingTxError.ReadFailed }
        val preparationTx = build {
            repository.createNativePreparationTransaction(
                userWalletId = userWalletId,
                network = network,
                wrappedToken = wrappedToken,
                preparation = preparation,
            )
        }
        // The preparation upgrades an outdated module within the same send, so the deposit calls the new implementation
        val depositTx = build {
            repository.createNativeEnterTransaction(
                userWalletId = userWalletId,
                network = network,
                amount = amount,
                wrapIntoUpgrade = false,
            )
        }
        listOf(preparationTx, depositTx)
    }

    override suspend fun withdraw(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        amount: BigDecimal,
        position: BigDecimal,
        state: LendingProviderState,
    ): Either<LendingTxError, TransactionData.Uncompiled> = either {
        requireCoin(asset)
        when (requireRead(state).toState()) {
            AaveModuleState.UpgradeUnavailable -> raise(LendingTxError.UpdateRequired)
            AaveModuleState.ReadFailed -> raise(LendingTxError.ReadFailed)
            else -> Unit
        }
        // Only the entire withdrawal deactivates the coin in the module,
        // so an amount covering the position takes it all
        val withdrawAmount = amount.takeIf { it < position }
        build { repository.createNativeWithdrawTransaction(userWalletId, asset.currency.network, withdrawAmount) }
    }

    override suspend fun estimate(
        userWallet: UserWallet,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, List<TransactionData.Uncompiled>> = estimateFee(userWallet, asset.currency, txs)

    override suspend fun feeLevels(
        userWallet: UserWallet,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, TransactionFee> = either {
        ensure(txs.isNotEmpty()) { GetFeeError.UnknownError }
        // The deposit cannot be priced alone before the module is deployed, so the levels come from the first one
        val levels = runSuspendCatching {
            feeRepository.calculateFee(userWallet, asset.currency, txs.first())
        }.getOrElse { raise(feeErrorResolver.resolve(it)) }
        // A single transaction is estimated by the same fee calculation, so its levels already carry its gas
        val estimated = if (txs.size == 1) {
            listOf(txs.single().copy(fee = levels.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY)))
        } else {
            estimate(userWallet, asset, txs).bind()
        }
        val totalGasLimit = estimated.fold(BigInteger.ZERO) { total, tx ->
            val gasLimit = (tx.fee as? Fee.Ethereum)?.gasLimit
            total + ensureNotNull(gasLimit) { GetFeeError.UnknownError }
        }

        fun Fee.forTotal() = fixFee(asset.currency, totalGasLimit)
        when (levels) {
            is TransactionFee.Choosable -> TransactionFee.Choosable(
                normal = levels.normal.forTotal(),
                minimum = levels.minimum.forTotal(),
                priority = levels.priority.forTotal(),
            )
            is TransactionFee.Single -> TransactionFee.Single(normal = levels.normal.forTotal())
        }
    }

    override suspend fun verify(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
        expected: LendingTxExpectation,
        state: LendingProviderState,
    ): Either<LendingTxError, Unit> = either {
        requireCoin(asset)
        val read = requireRead(state)
        val wrappedToken = ensureNotNull(asset.wrappedToken) { LendingTxError.ReadFailed }
        val network = asset.currency.network
        val moduleAddresses = build { repository.getNativeYieldModuleAddresses(userWalletId, network) }
        val addresses = AaveVerifiedAddresses(
            moduleAddress = moduleAddresses.moduleAddress,
            factoryAddress = moduleAddresses.factoryAddress,
            wrappedTokenAddress = wrappedToken.contractAddress,
            blockchain = network.toBlockchain(),
        )
        verifier.verify(txs = txs, expected = expected, addresses = addresses, read = read).bind()
    }

    private fun Raise<LendingTxError>.requireCoin(asset: LendingAsset) {
        ensure(asset.currency is CryptoCurrency.Coin) { LendingTxError.Unverified("Only a native coin is supported") }
    }

    private fun Raise<LendingTxError>.requireRead(state: LendingProviderState): AaveModuleRead =
        ensureNotNull(state as? AaveModuleRead) { LendingTxError.ReadFailed }

    /** The repository throws instead of returning `Either`, so its failures become `Left` here */
    private suspend fun <T> Raise<LendingTxError>.build(block: suspend () -> T): T =
        runSuspendCatching { block() }.getOrElse { error ->
            raise(
                if (error is YieldModuleUpgradeUnavailableException) {
                    LendingTxError.UpdateRequired
                } else {
                    LendingTxError.ReadFailed
                },
            )
        }
}