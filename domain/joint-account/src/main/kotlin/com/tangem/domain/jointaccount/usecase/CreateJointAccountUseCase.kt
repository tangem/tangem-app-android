package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationError
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountParticipant
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.supplier.findByMember
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    private val accountListFetcher: SingleAccountListFetcher,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        config: JointAccountConfig,
        creatorName: String,
    ): Either<JointAccountCreationError, JointAccount> = either {
        val rawIndex = Either
            .catch { repository.getFreeOwnerDerivationIndex(userWalletId = userWalletId) }
            .mapLeft(JointAccountFlowError::Failed)
            .bind()

        val derivationIndex = DerivationIndex(value = rawIndex)
            .mapLeft { JointAccountFlowError.InvalidDerivationIndex(cause = it) }
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
            .mapLeft(JointAccountFlowError::Failed)
            .bind()

        val refresh = refreshAccounts(userWalletId = userWalletId)

        when (result) {
            // The account is registered, so a failed refresh must not fail the creation: the list catches up on
            // the next fetch, and reporting an error here would push the user into a retry that can only conflict
            is JointAccountCreationResult.Created -> result.account
            JointAccountCreationResult.CreatorAlreadyRegistered -> {
                // Here the refresh is the only source of the existing account, so its failure is the real cause —
                // reporting "not found" instead would blame the data for a network problem
                refresh.mapLeft(JointAccountFlowError::Failed).bind()

                val own = supplier.findByMember(userWalletId = userWalletId, ownerAddress = signResult.ownerAddress)

                ensureNotNull(own) { JointAccountCreationError.ExistingAccountNotFound }
            }
        }
    }

    private fun createPayload(
        userWalletId: UserWalletId,
        config: JointAccountConfig,
        creatorName: String,
        derivationIndex: DerivationIndex,
        ownerAddress: String,
    ): JointAccountCreationPayload {
        return JointAccountCreationPayload(
            config = config,
            creator = JointAccountParticipant(
                walletId = userWalletId.stringValue,
                name = creatorName,
                address = ownerAddress,
                derivation = derivationIndex.value,
            ),
        )
    }

    private suspend fun refreshAccounts(userWalletId: UserWalletId): Either<Throwable, Unit> = coroutineScope {
        launch { refreshAccountList(userWalletId = userWalletId) }

        fetcher(params = SingleJointAccountListFetcher.Params(userWalletId = userWalletId))
    }

    /**
     * The wallet screen takes `Account.Joint` from the accounts document, not from the joint accounts store, so
     * without this refetch the new row shows up only after a pull-to-refresh. Best-effort: the account is already
     * registered, and the document catches up on the next fetch.
     */
    private suspend fun refreshAccountList(userWalletId: UserWalletId) {
        accountListFetcher(params = SingleAccountListFetcher.Params(userWalletId = userWalletId)).onLeft {
            TangemLogger.e("Failed to refetch the account list of $userWalletId after joint account creation", it)
        }
    }

    private fun toCreationError(cause: Throwable): JointAccountCreationError = when (cause) {
        is TangemSdkError.UserCancelled -> JointAccountFlowError.UserCancelled
        else -> JointAccountFlowError.Failed(cause = cause)
    }
}