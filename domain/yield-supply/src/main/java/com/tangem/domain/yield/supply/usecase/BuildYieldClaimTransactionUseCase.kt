package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata

/**
 * Turns a validated claim payload into the transaction the owner signs.
 *
 * Only a payload that passed [ValidateYieldClaimCalldataUseCase] belongs here: the `to` and `data` it carries are
 * taken as given, which is safe exactly because the gate has already matched them against the active context.
 */
class BuildYieldClaimTransactionUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldSupplyErrorResolver: YieldSupplyErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        calldata: YieldClaimCalldata,
        fee: Fee?,
    ): Either<YieldSupplyError, TransactionData.Uncompiled> = Either.catch {
        val destinationAddress = requireNotNull(calldata.to) { "Validated claim calldata has no destination" }
        val payload = requireNotNull(calldata.data) { "Validated claim calldata has no payload" }

        yieldSupplyTransactionRepository.createClaimRewardsTransaction(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            destinationAddress = destinationAddress,
            callData = MerklClaimCallData.fromHex(payload),
            fee = fee,
        )
    }.mapLeft(yieldSupplyErrorResolver::resolve)
}