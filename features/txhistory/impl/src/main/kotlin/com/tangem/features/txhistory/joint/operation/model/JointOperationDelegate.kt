package com.tangem.features.txhistory.joint.operation.model

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
import com.tangem.features.txhistory.joint.ownJointAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Only an operation in [JointOperation.Status.AWAITING_SIGNATURES] is exposed: one past the threshold is already an
 * ordinary unconfirmed transaction with nothing left to sign.
 */
internal class JointOperationDelegate @Inject constructor(
    private val accountListSupplier: SingleAccountListSupplier,
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val transferResolver: JointOperationTransferResolver,
    private val signersResolver: JointOperationSignersResolver,
    private val ownerAddressResolver: JointOwnerAddressResolver,
    private val signJointOperationUseCase: SignJointOperationUseCase,
) {

    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency, operationId: String): Flow<JointOperationState?> {
        if (currency.network.derivationPath !is Network.DerivationPath.Joint) return flowOf(null)

        return combine(
            flow = accountListSupplier.ownJointAccount(userWalletId = userWalletId, currency = currency),
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

    suspend fun sign(
        state: JointOperationState,
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
    ): JointOperationState? {
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

        return JointOperationState(
            account = account,
            displayed = transfer.intent,
            feeWei = operation.safeTx.gasPrice * (operation.safeTx.safeTxGas + operation.safeTx.baseGas),
            feeCoin = account.cryptoCurrencies
                .filterIsInstance<CryptoCurrency.Coin>()
                .firstOrNull { it.network == currency.network },
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
}