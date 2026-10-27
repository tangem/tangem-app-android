package com.tangem.features.txhistory.joint.operation.signers

import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.features.txhistory.joint.ownJointAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/** The answers to a finished operation, read against the account's current roster and threshold */
internal class FinishedJointOperationSigners @Inject constructor(
    private val accountListSupplier: SingleAccountListSupplier,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val signersResolver: JointOperationSignersResolver,
) {

    private val signersConverter = JointOperationSignersUMConverter()

    /** `null` while the viewed transaction is not a joint operation or its account is not known yet */
    fun observe(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        txHistoryInfo: Flow<TxHistoryInfo>,
    ): Flow<Signatures?> {
        if (!currency.isJoint) return flowOf(null)

        return txHistoryInfo
            .map { (it as? JointOperationTx)?.operation }
            .distinctUntilChanged()
            .flatMapLatest { operation ->
                if (operation == null) {
                    flowOf(null)
                } else {
                    observe(userWalletId = userWalletId, currency = currency, operation = operation)
                }
            }
            .onStart { emit(null) }
            .distinctUntilChanged()
    }

    private fun observe(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        operation: FinishedJointOperation,
    ): Flow<Signatures?> {
        return combine(
            flow = accountListSupplier.ownJointAccount(userWalletId = userWalletId, currency = currency),
            flow2 = jointAccountListSupplier(userWalletId = userWalletId),
        ) { account, jointAccounts ->
            val jointAccount = jointAccounts.firstOrNull { jointAccount ->
                account != null && jointAccount.cryptoAccountId.equals(account.accountId.value, ignoreCase = true)
            } ?: return@combine null
            val signers = signersResolver.signers(jointAccount = jointAccount, operation = operation)

            Signatures(
                collected = signers.count { it.answer == Answer.CREATED || it.answer == Answer.SIGNED },
                threshold = jointAccount.threshold,
                signers = signersConverter.convert(
                    value = JointOperationSignersUMConverter.Input(
                        membersCount = jointAccount.membersCount,
                        signers = signers,
                        isFinished = true,
                    ),
                ),
            )
        }
            .distinctUntilChanged()
    }

    data class Signatures(
        val collected: Int,
        val threshold: Int,
        val signers: JointOperationSignersUM,
    )
}