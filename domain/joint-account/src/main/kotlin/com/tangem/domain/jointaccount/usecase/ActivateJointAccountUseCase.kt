package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountActivationError
import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.SafeAddress
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

class ActivateJointAccountUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointAccountSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
) {

    suspend operator fun invoke(account: Account.Joint): Either<JointAccountActivationError, Unit> = either {
        val userWalletId = account.accountId.userWalletId

        val jointAccount = findJointAccount(userWalletId = userWalletId, accountId = account.accountId).bind()
        ensure(jointAccount.status == JointAccount.Status.CONFIRMING) {
            JointAccountActivationError.NotReadyForActivation
        }
        ensure(jointAccount.members.size == jointAccount.membersCount) {
            JointAccountActivationError.NotReadyForActivation
        }

        val payload = createPayload(
            account = account,
            jointAccount = jointAccount,
            safeAddress = confirmedSafeAddress(jointAccount = jointAccount),
        )

        val derivationIndex = DerivationIndex(value = account.ownerKeyIndex.value)
            .mapLeft { JointAccountFlowError.Failed(cause = IllegalStateException(it.toString())) }
            .bind()

        val signResult = signer
            .sign(
                userWalletId = userWalletId,
                input = JointAccountSignInput(derivationIndex = derivationIndex) { payload },
            )
            .mapLeft(::toActivationError)
            .bind()

        // The owner address is known only after the tap, so the creator check is post-sign: the backend would
        // reject a non-creator signature with 403 anyway, this only fails fast with a domain error instead
        val creatorAddress = jointAccount.members
            .firstOrNull { it.role == JointAccount.Role.CREATOR }
            ?.address
        ensure(signResult.ownerAddress.equals(creatorAddress, ignoreCase = true)) {
            JointAccountActivationError.NotCreator
        }

        repository
            .activate(userWalletId = userWalletId, payload = payload, signature = signResult.signature)
            .mapLeft(JointAccountFlowError::Failed)
            .bind()

        // The account is active on the backend, so a failed refresh must not fail the activation: the list
        // catches up on the next fetch, and reporting an error here would push the user into a pointless retry
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}
    }

    private suspend fun findJointAccount(
        userWalletId: UserWalletId,
        accountId: AccountId,
    ): Either<JointAccountActivationError, JointAccount> {
        val accounts = supplier(userWalletId = userWalletId).firstOrNull().orEmpty()
        val own = accounts.firstOrNull { it.cryptoAccountId.equals(accountId.value, ignoreCase = true) }

        return own?.right() ?: JointAccountActivationError.AccountNotFound.left()
    }

    /**
     * The address the composition resolves to, recomputed by the app — the backend's value alone is never
     * trusted. Which Safe singleton the backend deploys is undecided (alignment №21), so the address is
     * computed under every supported singleton and the backend's value picks the matching one; no match means
     * the composition does not resolve to the announced address, and activation is blocked.
     */
    private fun Raise<JointAccountActivationError>.confirmedSafeAddress(jointAccount: JointAccount): String {
        val backendAddress = jointAccount.safeAddress
        ensureNotNull(backendAddress) { JointAccountActivationError.NotReadyForActivation }

        val owners = jointAccount.members.map(JointAccount.Member::address)
        val match = SafeSingleton.entries
            .map { singleton ->
                // A composition no Safe can be deployed from is a distinct failure, not a mismatch
                SafeAddress.compute(owners = owners, threshold = jointAccount.threshold, singleton = singleton)
                    .mapLeft { JointAccountFlowError.Failed(cause = IllegalStateException(it.toString())) }
                    .bind()
            }
            .firstOrNull { computed -> computed.equals(backendAddress, ignoreCase = true) }

        return ensureNotNull(match) { JointAccountActivationError.AddressMismatch }
    }

    private fun Raise<JointAccountActivationError>.createPayload(
        account: Account.Joint,
        jointAccount: JointAccount,
        safeAddress: String,
    ): JointAccountActivationPayload {
        val name = when (val accountName = account.accountName) {
            is AccountName.Custom -> accountName.value
            is AccountName.DefaultMain -> raise(
                JointAccountFlowError.Failed(
                    cause = IllegalStateException("A joint account cannot carry the default main account name"),
                ),
            )
        }

        return JointAccountActivationPayload(
            walletId = account.accountId.userWalletId.stringValue,
            cryptoAccountId = jointAccount.cryptoAccountId,
            config = JointAccountActivationPayload.ConfirmedConfig(
                base = JointAccountConfig(
                    name = name,
                    icon = account.icon.value.name,
                    iconColor = account.icon.color.name,
                    membersCount = jointAccount.membersCount,
                    threshold = jointAccount.threshold,
                ),
                safeAddress = safeAddress,
            ),
        )
    }

    private fun toActivationError(cause: Throwable): JointAccountActivationError = when (cause) {
        is TangemSdkError.UserCancelled -> JointAccountFlowError.UserCancelled
        else -> JointAccountFlowError.Failed(cause = cause)
    }
}