package com.tangem.domain.jointaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.model.JointAccountJoinError
import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.domain.jointaccount.model.JointAccountJoinResult
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

class JoinJointAccountUseCase(
    private val repository: JointAccountRepository,
    private val signer: JointAccountSigner,
    private val fetcher: SingleJointAccountListFetcher,
    private val supplier: SingleJointAccountListSupplier,
    private val accountListFetcher: SingleAccountListFetcher,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        inviteId: String,
        config: JointAccountConfig,
        memberName: String,
    ): Either<JointAccountJoinError, JointAccount> = either {
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
                        inviteId = inviteId,
                        config = config,
                        memberName = memberName,
                        derivationIndex = derivationIndex,
                        ownerAddress = ownerAddress,
                    )
                },
            )
            .mapLeft(::toJoinError)
            .bind()

        val result = Either
            .catch {
                repository.join(
                    userWalletId = userWalletId,
                    payload = createPayload(
                        userWalletId = userWalletId,
                        inviteId = inviteId,
                        config = config,
                        memberName = memberName,
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
            is JointAccountJoinResult.Joined -> result.account
            JointAccountJoinResult.Conflict -> {
                refresh.mapLeft(JointAccountFlowError::Failed).bind()

                val own = supplier.findByMember(userWalletId = userWalletId, ownerAddress = signResult.ownerAddress)

                ensureNotNull(own) { JointAccountJoinError.InvalidInvite }
            }
        }
    }

    @Suppress("LongParameterList")
    private fun createPayload(
        userWalletId: UserWalletId,
        inviteId: String,
        config: JointAccountConfig,
        memberName: String,
        derivationIndex: DerivationIndex,
        ownerAddress: String,
    ): JointAccountJoinPayload {
        return JointAccountJoinPayload(
            inviteId = inviteId,
            config = config,
            member = JointAccountParticipant(
                walletId = userWalletId.stringValue,
                name = memberName,
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
            TangemLogger.e("Failed to refetch the account list of $userWalletId after joint account joining", it)
        }
    }

    private fun toJoinError(cause: Throwable): JointAccountJoinError = when (cause) {
        is TangemSdkError.UserCancelled -> JointAccountFlowError.UserCancelled
        else -> JointAccountFlowError.Failed(cause = cause)
    }
}