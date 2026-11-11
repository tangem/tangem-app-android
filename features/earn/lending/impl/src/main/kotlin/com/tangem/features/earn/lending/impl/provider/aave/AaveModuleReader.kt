package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class AaveModuleReader @Inject constructor(
    private val yieldTransactions: YieldSupplyTransactionRepository,
) {

    /** The repository throws instead of returning `Either`, so a failed version read becomes `Left` here */
    suspend fun read(userWalletId: UserWalletId, asset: LendingAsset): Either<Throwable, AaveOnChainRead> =
        runSuspendCatching {
            coroutineScope {
                val version =
                    async { yieldTransactions.getYieldModuleVersionStatus(userWalletId, asset.currency.network) }
                val liquidity = async { readLiquidity(userWalletId, asset) }
                val versionStatus = version.await()
                // The SDK reports a failed RPC read as Indeterminate instead of throwing; it is a failure here, so the
                // previous successful read survives
                if (versionStatus is YieldModuleVersionStatus.Indeterminate) {
                    error("Yield Module version is indeterminate: ${versionStatus.reason}")
                }
                AaveOnChainRead(version = versionStatus, withdrawLiquidity = liquidity.await())
            }
        }
            .onFailure { TangemLogger.e("Aave module read failed", it) }
            .fold(onSuccess = { Either.Right(it) }, onFailure = { Either.Left(it) })

    /** Without the pool liquidity a withdrawal is capped by the position only, so its failure is not fatal */
    private suspend fun readLiquidity(userWalletId: UserWalletId, asset: LendingAsset) = runSuspendCatching {
        yieldTransactions.getWithdrawLiquidity(userWalletId, asset.reserveToken)
    }.getOrNull()
}