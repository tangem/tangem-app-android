package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationError
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountParticipant
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

/**
 * Creates a joint account: picks the owner derivation index, signs the payload with the card in one tap,
 * registers the account and leaves the wallet's joint accounts refreshed.
 *
 * The index is read right before the tap and never incremented afterwards — the contract forbids retrying
 * under the next index, so a conflict means the account already exists and is resolved by re-reading the list.
 */
class CreateJointAccountUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointAccountSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        config: JointAccountConfig,
        creatorName: String,
    ): Either<JointAccountCreationError, JointAccount> = either {
        val derivationIndex = Either
            .catch { repository.getFreeOwnerDerivationIndex(userWalletId = userWalletId) }
            .mapLeft(JointAccountCreationError::Failed)
            .bind()

        val signResult = signer
            .sign(
                userWalletId = userWalletId,
                input = JointAccountSignInput(derivationIndex = derivationIndex) { ownerAddress ->
                    createPayload(
                        userWalletId = userWalletId,
                        config = config,
                        creatorName = creatorName,
                        derivationIndex = derivationIndex,
                        ownerAddress = ownerAddress,
                    )
                },
            )
            .mapLeft(::toCreationError)
            .bind()

        val result = Either
            .catch {
                repository.create(
                    userWalletId = userWalletId,
                    payload = createPayload(
                        userWalletId = userWalletId,
                        config = config,
                        creatorName = creatorName,
                        derivationIndex = derivationIndex,
                        ownerAddress = signResult.ownerAddress,
                    ),
                    signature = signResult.signature,
                )
            }
            .mapLeft(JointAccountCreationError::Failed)
            .bind()

        val refresh = refreshAccounts(userWalletId = userWalletId)

        when (result) {
            // The account is registered, so a failed refresh must not fail the creation: the list catches up on
            // the next fetch, and reporting an error here would push the user into a retry that can only conflict
            is JointAccountCreationResult.Created -> result.account
            JointAccountCreationResult.CreatorAlreadyRegistered -> {
                // Here the refresh is the only source of the existing account, so its failure is the real cause —
                // reporting "not found" instead would blame the data for a network problem
                refresh.mapLeft(JointAccountCreationError::Failed).bind()

                findOwnAccount(userWalletId = userWalletId, ownerAddress = signResult.ownerAddress).bind()
            }
        }
    }

    private fun createPayload(
        userWalletId: UserWalletId,
        config: JointAccountConfig,
        creatorName: String,
        derivationIndex: Int,
        ownerAddress: String,
    ): JointAccountCreationPayload {
        return JointAccountCreationPayload(
            config = config,
            creator = JointAccountParticipant(
                walletId = userWalletId.stringValue,
                name = creatorName,
                address = ownerAddress,
                derivation = derivationIndex,
            ),
        )
    }

    private suspend fun refreshAccounts(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId))
    }

    /** The own account is found by the owner address derived from the card, never by trusting an index. */
    private suspend fun findOwnAccount(
        userWalletId: UserWalletId,
        ownerAddress: String,
    ): Either<JointAccountCreationError, JointAccount> {
        val accounts = supplier(userWalletId = userWalletId).firstOrNull().orEmpty()
        val own = accounts.firstOrNull { account ->
            account.members.any { member -> member.address.equals(ownerAddress, ignoreCase = true) }
        }

        return own?.right() ?: JointAccountCreationError.ExistingAccountNotFound.left()
    }

    private fun toCreationError(cause: Throwable): JointAccountCreationError = when (cause) {
        is TangemSdkError.UserCancelled -> JointAccountCreationError.UserCancelled
        else -> JointAccountCreationError.Failed(cause = cause)
    }
}