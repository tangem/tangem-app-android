package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory as CallDataFactory
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterWithNativeCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitNativeCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.yield.supply.models.YieldModulePreparation
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.features.earn.lending.impl.provider.callData
import com.tangem.features.earn.lending.impl.provider.isSameAddressAs
import com.tangem.features.earn.lending.impl.provider.isSameCallAs
import com.tangem.features.earn.lending.impl.provider.isSentTo
import java.math.BigDecimal
import javax.inject.Inject

/** The contracts the transactions may touch; the caller resolves them, the verifier trusts none of its own */
internal data class AaveVerifiedAddresses(
    val moduleAddress: String,
    val factoryAddress: String,
    val wrappedTokenAddress: String,
    val blockchain: Blockchain,
) {
    val networkId: String
        get() = blockchain.toNetworkId()
}

internal class AaveTransactionVerifier @Inject constructor() {

    private val AaveModuleRead.isUpgradeRequired: Boolean
        get() = latestImplementation != null

    /** @param read the module state the transactions must have been built for */
    fun verify(
        txs: List<TransactionData.Uncompiled>,
        expected: LendingTxExpectation,
        addresses: AaveVerifiedAddresses,
        read: AaveModuleRead,
    ): Either<LendingTxError, Unit> = either {
        val areNativeCoins = txs.all { tx -> tx.amount.isNativeCoinOf(addresses.blockchain) }
        ensure(areNativeCoins) { unverified("Amount is not a native coin") }
        val areSentByOwner = txs.all { tx -> tx.sourceAddress.isSameAddressAs(expected.owner, addresses.networkId) }
        ensure(areSentByOwner) { unverified("Transaction is sent by another address") }
        when (expected) {
            is LendingTxExpectation.Deposit -> verifyDeposit(
                txs = txs,
                expected = expected,
                addresses = addresses,
                read = read,
            )
            is LendingTxExpectation.Withdraw -> verifyWithdraw(
                txs = txs,
                expected = expected,
                addresses = addresses,
                read = read,
            )
        }
    }

    private fun Raise<LendingTxError>.verifyDeposit(
        txs: List<TransactionData.Uncompiled>,
        expected: LendingTxExpectation.Deposit,
        addresses: AaveVerifiedAddresses,
        read: AaveModuleRead,
    ) {
        val preparation = when (read.toState()) {
            AaveModuleState.NotDeployed -> YieldModulePreparation.Deploy
            is AaveModuleState.NeedsInit -> YieldModulePreparation.InitToken
            is AaveModuleState.NeedsReactivation -> YieldModulePreparation.ReactivateToken
            is AaveModuleState.Ready -> null
            AaveModuleState.ReadFailed,
            AaveModuleState.UpgradeUnavailable,
            is AaveModuleState.StatusUnknown,
            -> raise(unverified("Module is not ready for a deposit"))
        }
        val expectedCount = if (preparation == null) 1 else 2
        ensure(txs.size == expectedCount) { unverified("Unexpected number of transactions: ${txs.size}") }

        if (preparation != null) {
            verifyPreparation(
                tx = txs.first(),
                preparation = preparation,
                owner = expected.owner,
                addresses = addresses,
                read = read,
            )
        }

        val deposit = txs.last()
        ensure(deposit.isSentTo(addresses.moduleAddress, addresses.networkId)) {
            unverified("Deposit goes to another address")
        }
        ensure(deposit.amount.value?.compareTo(expected.amount) == 0) { unverified("Deposit amount differs") }
        val isUpgradeExpected = preparation == null && read.isUpgradeRequired
        val call =
            unwrap(tx = deposit, read = read, networkId = addresses.networkId, isUpgradeExpected = isUpgradeExpected)
        ensure(call === EthereumYieldSupplyEnterWithNativeCallData) { unverified("Deposit is not a native supply") }
    }

    private fun Raise<LendingTxError>.verifyPreparation(
        tx: TransactionData.Uncompiled,
        preparation: YieldModulePreparation,
        owner: String,
        addresses: AaveVerifiedAddresses,
        read: AaveModuleRead,
    ) {
        ensure(tx.amount.value?.signum() == 0) { unverified("Preparation carries a value") }
        val zeroFee = Amount(value = BigDecimal.ZERO, blockchain = addresses.blockchain)
        val token = addresses.wrappedTokenAddress

        if (preparation == YieldModulePreparation.Deploy) {
            val deploy = CallDataFactory.getDeployCallData(owner, token, zeroFee)
            ensure(tx.callData.isSameCallAs(deploy)) { unverified("Unexpected preparation call") }
            ensure(tx.isSentTo(addresses.factoryAddress, addresses.networkId)) {
                unverified("Deploy goes to another address")
            }
            return
        }

        val expectedCall = if (preparation == YieldModulePreparation.InitToken) {
            CallDataFactory.getInitTokenCallData(token, zeroFee)
        } else {
            CallDataFactory.getReactivateTokenCallData(token, zeroFee)
        }
        val call =
            unwrap(tx = tx, read = read, networkId = addresses.networkId, isUpgradeExpected = read.isUpgradeRequired)
        ensure(call.isSameCallAs(expectedCall)) { unverified("Unexpected preparation call") }
        ensure(tx.isSentTo(addresses.moduleAddress, addresses.networkId)) {
            unverified("Preparation goes to another address")
        }
    }

    private fun Raise<LendingTxError>.verifyWithdraw(
        txs: List<TransactionData.Uncompiled>,
        expected: LendingTxExpectation.Withdraw,
        addresses: AaveVerifiedAddresses,
        read: AaveModuleRead,
    ) {
        when (read.toState()) {
            AaveModuleState.ReadFailed,
            AaveModuleState.UpgradeUnavailable,
            -> raise(unverified("Module is not ready for a withdrawal"))
            AaveModuleState.NotDeployed,
            is AaveModuleState.StatusUnknown,
            is AaveModuleState.NeedsInit,
            is AaveModuleState.NeedsReactivation,
            is AaveModuleState.Ready,
            -> Unit
        }
        ensure(txs.size == 1) { unverified("Unexpected number of transactions: ${txs.size}") }
        val tx = txs.single()
        ensure(tx.isSentTo(addresses.moduleAddress, addresses.networkId)) {
            unverified("Withdrawal goes to another address")
        }
        ensure(tx.amount.value?.signum() == 0) { unverified("Withdrawal carries a value") }

        val call =
            unwrap(tx = tx, read = read, networkId = addresses.networkId, isUpgradeExpected = read.isUpgradeRequired)
        val isExpectedCall = if (expected.isEntire) {
            call === EthereumYieldSupplyExitNativeCallData
        } else {
            val amount = Amount(value = expected.amount, blockchain = addresses.blockchain)
            call.isSameCallAs(CallDataFactory.getWithdrawNativeCallData(amount))
        }
        ensure(isExpectedCall) { unverified("Unexpected withdrawal call") }
    }

    /**
     * An outdated module must be upgraded by the first call to it, since its old implementation may not know the call
     *
     * @return the call the transaction performs, with the module upgrade envelope checked and removed
     */
    private fun Raise<LendingTxError>.unwrap(
        tx: TransactionData.Uncompiled,
        read: AaveModuleRead,
        networkId: String,
        isUpgradeExpected: Boolean,
    ): SmartContractCallData {
        val call = ensureNotNull(tx.callData) { unverified("Transaction has no call data") }
        if (call !is EthereumYieldSupplyUpgradeToAndCallCallData) {
            ensure(!isUpgradeExpected) { unverified("Module upgrade is missing") }
            return call
        }

        ensure(isUpgradeExpected) { unverified("Upgrade is not expected here") }
        val latest = read.latestImplementation
        ensure(latest != null && call.newImplementation.isSameAddressAs(latest, networkId)) {
            unverified("Upgrade to an unexpected implementation")
        }
        return call.innerCallData
    }

    private fun Amount.isNativeCoinOf(blockchain: Blockchain): Boolean =
        type == AmountType.Coin && decimals == blockchain.decimals() && currencySymbol == blockchain.currency

    private fun unverified(reason: String) = LendingTxError.Unverified(reason)
}