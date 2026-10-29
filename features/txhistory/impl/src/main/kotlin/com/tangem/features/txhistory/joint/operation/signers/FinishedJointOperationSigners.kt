package com.tangem.features.txhistory.joint.operation.signers

import com.tangem.common.ui.cosign.CosignRequestRowUM
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.features.txhistory.joint.ownJointAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

            signatures(jointAccount = jointAccount, operation = operation)
        }
            .distinctUntilChanged()
    }

    fun signatures(jointAccount: JointAccount, operation: FinishedJointOperation): Signatures {
        val signers = signersResolver.signers(jointAccount = jointAccount, operation = operation)

        return Signatures(
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

    data class Signatures(
        val collected: Int,
        val threshold: Int,
        val signers: JointOperationSignersUM,
    )
}

internal fun TxHistoryDetailsUM.SingleAsset.withCosign(
    signatures: FinishedJointOperationSigners.Signatures,
    onSignersClick: () -> Unit,
): TxHistoryDetailsUM.SingleAsset = copy(
    cosign = CosignRequestRowUM(
        title = resourceReference(
            id = R.string.joint_operation_signatures_progress,
            formatArgs = wrappedList(signatures.collected, signatures.threshold),
        ),
        progress = SignatureProgressUM(collected = signatures.collected, total = signatures.threshold),
        onInfoClick = onSignersClick,
    ),
)