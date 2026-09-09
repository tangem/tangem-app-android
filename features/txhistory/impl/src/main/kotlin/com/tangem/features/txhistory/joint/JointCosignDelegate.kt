package com.tangem.features.txhistory.joint

import arrow.core.Either
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.usecase.SignJointOperationUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * What the transaction details screen needs from the joint account domain. A transaction of a joint account that is
 * still collecting signatures has no hash — it is shown from the operation itself, keyed by the operation id.
 *
 * Only an operation in [JointOperation.Status.AWAITING_SIGNATURES] is exposed: the store also keeps the ones that
 * already collected the threshold and are executing, and those are ordinary unconfirmed transactions with nothing
 * left to sign.
 */
internal class JointCosignDelegate @Inject constructor(
    private val accountListSupplier: SingleAccountListSupplier,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val transferResolver: JointOperationTransferResolver,
    private val signersResolver: JointOperationSignersResolver,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val signJointOperationUseCase: SignJointOperationUseCase,
) {

    /** The operation behind [operationId], or `null` while the viewed row is an ordinary on-chain transaction */
    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency, operationId: String): Flow<JointCosignState?> {
        if (currency.network.derivationPath !is Network.DerivationPath.Joint) return flowOf(null)

        return combine(
            flow = ownAccountFlow(userWalletId = userWalletId, currency = currency),
            flow2 = jointAccountListSupplier(userWalletId = userWalletId),
        ) { account, accounts ->
            resolve(
                account = account,
                currency = currency,
                operationId = operationId,
                accounts = accounts,
            )
        }
            .distinctUntilChanged()
    }

    /**
     * Ids of the operations still collecting signatures in the account this currency belongs to — the rows to badge
     * as awaiting the other members. Another account's operations are left out: a change in this set reloads the
     * viewed history, and they can neither appear in it nor be signed from it.
     */
    fun observePendingOperationIds(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<Set<String>> {
        if (currency.network.derivationPath !is Network.DerivationPath.Joint) return flowOf(emptySet())

        return combine(
            flow = ownAccountFlow(userWalletId = userWalletId, currency = currency),
            flow2 = jointAccountListSupplier(userWalletId = userWalletId),
        ) { account, accounts -> pendingOperationIds(account = account, accounts = accounts) }
            .distinctUntilChanged()
    }

    private fun pendingOperationIds(account: Account.Joint?, accounts: List<JointAccount>): Set<String> {
        if (account == null) return emptySet()
        val jointAccount = accounts
            .firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
            ?: return emptySet()

        return jointAccount.inFlightOperations.values
            .asSequence()
            .filter { it.status == JointOperation.Status.AWAITING_SIGNATURES }
            .map(JointOperation::operationId)
            .toSet()
    }

    suspend fun sign(
        state: JointCosignState,
        network: Network,
        operationId: String,
    ): Either<JointOperationSignError, Unit> {
        return signJointOperationUseCase(
            account = state.account,
            network = network,
            operationId = operationId,
            displayed = state.displayed,
        )
    }

    private suspend fun resolve(
        account: Account.Joint?,
        currency: CryptoCurrency,
        operationId: String,
        accounts: List<JointAccount>,
    ): JointCosignState? {
        if (account == null) return null
        val jointAccount = accounts
            .firstOrNull { it.cryptoAccountId.equals(account.accountId.value, ignoreCase = true) }
            ?: return null

        val operation = jointAccount.inFlightOperations.values
            .firstOrNull { it.operationId == operationId }
            ?.takeIf { it.status == JointOperation.Status.AWAITING_SIGNATURES }
            ?: return null

        val safeAddress = jointAccount.safeAddress ?: return null
        val transfer = transferResolver.resolve(
            safeTx = operation.safeTx,
            currencies = account.cryptoCurrencies,
            network = currency.network,
            safeAddress = safeAddress,
        ) ?: return null

        val ownerAddress = ownerAddressResolver
            .resolve(userWalletId = account.accountId.userWalletId, ownerKeyIndex = account.ownerKeyIndex)
            .getOrNull()
        val signers = signersResolver.signers(jointAccount = jointAccount, operation = operation)

        return JointCosignState(
            account = account,
            displayed = transfer.intent,
            threshold = jointAccount.threshold,
            membersCount = jointAccount.membersCount,
            signers = signers,
            canSign = signersResolver.canSign(
                jointAccount = jointAccount,
                operation = operation,
                ownerAddress = ownerAddress,
            ),
            ownAnswer = ownerAddress?.let { own ->
                signers.firstOrNull { it.member.address.equals(own, ignoreCase = true) }?.answer
            },
        )
    }

    /**
     * The joint account this currency belongs to, as a flow: collecting the account list inside the joint-list
     * collector would re-subscribe to it on every joint emission.
     */
    private fun ownAccountFlow(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<Account.Joint?> {
        return accountListSupplier(userWalletId = userWalletId)
            .map { accountList ->
                accountList.accounts
                    .filterIsInstance<Account.Joint>()
                    .firstOrNull { account -> account.cryptoCurrencies.any { it.network == currency.network } }
            }
            .distinctUntilChanged()
    }
}