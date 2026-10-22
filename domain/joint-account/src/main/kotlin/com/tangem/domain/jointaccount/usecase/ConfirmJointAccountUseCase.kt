package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountConfirmationError
import com.tangem.domain.jointaccount.model.JointAccountConfirmationPayload
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.find
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.utils.logging.TangemLogger

class ConfirmJointAccountUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointAccountSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
    private val safeAddressCalculator: SafeAddressCalculator,
) {

    suspend operator fun invoke(account: Account.Joint): Either<JointAccountConfirmationError, JointAccount> = either {
        val userWalletId = account.accountId.userWalletId

        val jointAccount = ensureNotNull(supplier.find(accountId = account.accountId)) {
            refused(
                error = JointAccountConfirmationError.AccountNotFound,
                reason = "none of the wallet's joint accounts is the tapped one",
            )
        }
        ensure(jointAccount.status == JointAccount.Status.CONFIRMING) {
            refused(
                error = JointAccountConfirmationError.NotReadyForConfirmation,
                reason = "the account is ${jointAccount.status}, not ${JointAccount.Status.CONFIRMING}",
            )
        }
        ensure(jointAccount.members.size == jointAccount.membersCount) {
            refused(
                error = JointAccountConfirmationError.NotReadyForConfirmation,
                reason = "${jointAccount.members.size} of ${jointAccount.membersCount} members have joined",
            )
        }

        val payload = createPayload(
            account = account,
            jointAccount = jointAccount,
            safeAddress = confirmedSafeAddress(jointAccount = jointAccount),
        )

        val derivationIndex = DerivationIndex(value = account.ownerKeyIndex.value)
            .mapLeft { indexError ->
                refused(
                    error = JointAccountFlowError.Failed(cause = IllegalStateException(indexError.toString())),
                    reason = "the owner key index ${account.ownerKeyIndex.value} is not derivable: $indexError",
                )
            }
            .bind()

        val signResult = signer
            .sign(
                userWalletId = userWalletId,
                input = JointAccountSignInput(derivationIndex = derivationIndex) { payload },
            )
            .mapLeft(::toConfirmationError)
            .bind()

        // The owner address is known only after the tap, so the membership check is post-sign: the backend would
        // reject a stranger's signature with 403 anyway, this only fails fast with a domain error instead
        val isMember = jointAccount.members.any { it.address.equals(signResult.ownerAddress, ignoreCase = true) }
        ensure(isMember) {
            refused(
                error = JointAccountConfirmationError.NotMember,
                reason = "the tapped wallet's owner key belongs to none of the members",
            )
        }

        val confirmed = repository
            .confirm(userWalletId = userWalletId, payload = payload, signature = signResult.signature)
            .mapLeft { cause ->
                refused(
                    error = JointAccountFlowError.Failed(cause = cause),
                    reason = "the confirmation call failed",
                    cause = cause,
                )
            }
            .bind()

        // The signature is recorded on the backend, so a failed refresh must not fail the confirmation: the list
        // catches up on the next fetch, and reporting an error here would push the user into a pointless retry
        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId)).getOrElse {}

        confirmed
    }

    /**
     * The address the composition resolves to, recomputed by the app — the backend's value alone is never
     * trusted. Which Safe singleton the backend deploys is undecided (alignment №21), so the address is
     * computed under every supported singleton and the backend's value picks the matching one; no match means
     * the composition does not resolve to the announced address, and confirmation is blocked.
     *
     * Once confirmed, the backend's lower-case spelling is what gets signed: the payload is compared with the
     * stored account byte by byte, and a checksummed address would not match it.
     */
    private fun Raise<JointAccountConfirmationError>.confirmedSafeAddress(jointAccount: JointAccount): String {
        val backendAddress = jointAccount.safeAddress
        ensureNotNull(backendAddress) {
            refused(
                error = JointAccountConfirmationError.NotReadyForConfirmation,
                reason = "the backend has not returned the Safe address yet",
            )
        }

        val owners = jointAccount.members.map(JointAccount.Member::address)
        val computed = SafeSingleton.entries
            .map { singleton ->
                // A composition no Safe can be deployed from is a distinct failure, not a mismatch
                safeAddressCalculator.compute(
                    owners = owners,
                    threshold = jointAccount.threshold,
                    singleton = singleton,
                )
                    .mapLeft { addressError ->
                        refused(
                            error = JointAccountFlowError.Failed(
                                cause = IllegalStateException(addressError.toString()),
                            ),
                            reason = "the Safe address does not compute under $singleton: $addressError",
                        )
                    }
                    .bind()
            }

        val isConfirmed = computed.any { it.equals(backendAddress, ignoreCase = true) }

        ensure(isConfirmed) {
            refused(
                error = JointAccountConfirmationError.AddressMismatch,
                reason = "the announced Safe address $backendAddress is none of ${computed.joinToString()}, " +
                    "computed from ${owners.size} owners and threshold ${jointAccount.threshold}",
                // Both addresses are the whole diagnostic, and the sanitizer replaces any hex run of 8 or more
                // characters in the exported log file — the only build a user can send us logs from
                shouldSanitize = false,
            )
        }

        return backendAddress.lowercase()
    }

    private fun Raise<JointAccountConfirmationError>.createPayload(
        account: Account.Joint,
        jointAccount: JointAccount,
        safeAddress: String,
    ): JointAccountConfirmationPayload {
        val name = when (val accountName = account.accountName) {
            is AccountName.Custom -> accountName.value
            is AccountName.DefaultMain -> raise(
                refused(
                    error = JointAccountFlowError.Failed(
                        cause = IllegalStateException("A joint account cannot carry the default main account name"),
                    ),
                    reason = "the account carries the default main account name",
                ),
            )
        }

        return JointAccountConfirmationPayload(
            config = JointAccountConfig(
                name = name,
                icon = account.icon.value.name,
                iconColor = account.icon.color.name,
                membersCount = jointAccount.membersCount,
                threshold = jointAccount.threshold,
            ),
            safeAddress = safeAddress,
        )
    }

    private fun toConfirmationError(cause: Throwable): JointAccountConfirmationError = when (cause) {
        // Dismissing the card session is not a failure and has nothing to diagnose later
        is TangemSdkError.UserCancelled -> JointAccountFlowError.UserCancelled
        else -> refused(
            error = JointAccountFlowError.Failed(cause = cause),
            reason = "signing the confirmation payload failed",
            cause = cause,
        )
    }

    /**
     * Every refusal reaches the user as the same generic dialog, so the true reason survives only here. The
     * level is `e` deliberately: only `e` and `i` reach the exported log file, and a refused confirmation is
     * exactly what a support request arrives with.
     */
    private fun <E : JointAccountConfirmationError> refused(
        error: E,
        reason: String,
        cause: Throwable? = null,
        shouldSanitize: Boolean = true,
    ): E {
        TangemLogger.e(
            messageString = "Joint account confirmation refused: $reason",
            throwable = cause,
            shouldSanitize = shouldSanitize,
        )
        return error
    }
}