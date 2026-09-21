package com.tangem.data.jointaccount.repository

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.api.ApiResponseRaise
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.jointaccount.converter.ActivateJointAccountRequestConverter
import com.tangem.data.jointaccount.converter.CreateJointAccountRequestConverter
import com.tangem.data.jointaccount.converter.JoinJointAccountRequestConverter
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.converter.JointAccountInvitePreviewDtoConverter
import com.tangem.data.jointaccount.converter.JointOperationCreateErrorConverter
import com.tangem.data.jointaccount.converter.JointOperationRejectErrorConverter
import com.tangem.data.jointaccount.converter.JointOperationSignErrorConverter
import com.tangem.data.jointaccount.converter.SafeTxConverter
import com.tangem.store.datasource.jointaccount.models.CreateJointOperationRequest
import com.tangem.store.datasource.jointaccount.models.SignJointOperationRequest
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.domain.jointaccount.model.JointAccountJoinResult
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultJointAccountRepository @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val invitesStore: JointAccountInvitesStore,
    private val converter: JointAccountDtoConverter,
    private val previewConverter: JointAccountInvitePreviewDtoConverter,
    private val safeTxConverter: SafeTxConverter,
    private val dispatchers: CoroutineDispatcherProvider,
    @NetworkMoshi private val moshi: Moshi,
) : JointAccountRepository {

    private val logger = TangemLogger.withTag(tag = TAG)
    private val createErrorConverter = JointOperationCreateErrorConverter(moshi)
    private val signErrorConverter = JointOperationSignErrorConverter()
    private val rejectErrorConverter = JointOperationRejectErrorConverter()

    override suspend fun getFreeOwnerDerivationIndex(userWalletId: UserWalletId): Int {
        return withContext(dispatchers.io) {
            walletAccountsFetcher.fetch(userWalletId = userWalletId).wallet.totalJointAccounts ?: NO_JOINT_ACCOUNTS
        }
    }

    override suspend fun create(
        userWalletId: UserWalletId,
        payload: JointAccountCreationPayload,
        signature: String,
    ): JointAccountCreationResult {
        return withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    val response = jointAccountApi.createJointAccount(
                        walletId = userWalletId.stringValue,
                        body = CreateJointAccountRequestConverter.convert(
                            payload = payload,
                            signature = signature,
                        ),
                    ).bind()

                    storeInvites(
                        userWalletId = userWalletId,
                        cryptoAccountId = response.cryptoAccountId,
                        invites = response.invites.orEmpty().map { it.id },
                    )

                    JointAccountCreationResult.Created(account = converter.convert(value = response))
                },
                onError = { error ->
                    // The HTTP code stays in the data layer: the domain branches on the result, not on 409
                    if (error is ApiResponseError.HttpException && error.code == Code.CONFLICT) {
                        JointAccountCreationResult.CreatorAlreadyRegistered
                    } else {
                        throw error
                    }
                },
            )
        }
    }

    override suspend fun join(
        userWalletId: UserWalletId,
        payload: JointAccountJoinPayload,
        signature: String,
    ): JointAccountJoinResult {
        return withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    val response = jointAccountApi.joinJointAccount(
                        walletId = userWalletId.stringValue,
                        body = JoinJointAccountRequestConverter(signature = signature).convert(value = payload),
                    ).bind()

                    JointAccountJoinResult.Joined(account = converter.convert(value = response))
                },
                onError = { error ->
                    // The HTTP code stays in the data layer: the domain branches on the result, not on 409
                    if (error is ApiResponseError.HttpException && error.code == Code.CONFLICT) {
                        JointAccountJoinResult.Conflict
                    } else {
                        throw error
                    }
                },
            )
        }
    }

    override suspend fun activate(
        userWalletId: UserWalletId,
        payload: JointAccountActivationPayload,
        signature: String,
    ): Either<Throwable, Unit> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                activateOnce(userWalletId = userWalletId, payload = payload, signature = signature)
                Unit.right()
            },
            onError = { error ->
                if (error is ApiResponseError.TimeoutException) {
                    logger.e(messageString = "Activation timed out, retrying once — the endpoint is idempotent", error)
                    safeApiCall(
                        call = {
                            activateOnce(userWalletId = userWalletId, payload = payload, signature = signature)
                            Unit.right()
                        },
                        onError = { retryError -> retryError.left() },
                    )
                } else {
                    error.left()
                }
            },
        )
    }

    private suspend fun ApiResponseRaise.activateOnce(
        userWalletId: UserWalletId,
        payload: JointAccountActivationPayload,
        signature: String,
    ) {
        jointAccountApi.activateJointAccount(
            walletId = userWalletId.stringValue,
            body = ActivateJointAccountRequestConverter(signature = signature).convert(value = payload),
        ).bind()
    }

    override suspend fun getInvitePreview(
        walletId: UserWalletId,
        inviteId: String,
    ): Either<JointAccountInvitePreviewError, JointAccountInvitePreview> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val dto = jointAccountApi.getJointAccountInvite(
                    walletId = walletId.stringValue,
                    inviteId = inviteId,
                ).bind()
                previewConverter.convert(dto).right()
            },
            onError = { error ->
                // A link the backend does not know is as dead as one it refuses: retrying it can only fail again
                val isInvalidLink = error is ApiResponseError.HttpException &&
                    error.code in DEAD_INVITE_CODES
                if (isInvalidLink) {
                    JointAccountInvitePreviewError.InvalidLink.left()
                } else {
                    JointAccountInvitePreviewError.LoadFailed(cause = error).left()
                }
            },
        )
    }

    /**
     * An unreadable store must not take the screen down with it: the members of the account are worth showing
     * even when the invite actions cannot be offered. As in [storeInvites], the failure is logged without its
     * content — an invite id is a secret.
     */
    override suspend fun getInvites(userWalletId: UserWalletId, cryptoAccountId: String): List<String> {
        return withContext(dispatchers.io) {
            runSuspendCatching {
                invitesStore.get(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId).orEmpty()
            }.getOrElse { throwable ->
                logger.e(messageString = "Failed to read the stored invites", throwable = throwable)
                emptyList()
            }
        }
    }

    /**
     * A failure here must not fail the creation: the account already exists on the backend, and reporting an
     * error would send the user into a retry that can only end in a conflict. The invites are lost — the
     * backend returns them once — so the fact is logged, never their content: an invite id is a secret.
     */
    private suspend fun storeInvites(userWalletId: UserWalletId, cryptoAccountId: String, invites: List<String>) {
        runSuspendCatching {
            invitesStore.store(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId, invites = invites)
        }.onFailure { throwable ->
            logger.e(
                messageString = "Failed to persist the invites of the created joint account",
                throwable = throwable,
            )
        }
    }

    override suspend fun createOperation(
        userWalletId: UserWalletId,
        safeAddress: String,
        chainId: Long,
        safeTx: SafeTx,
        signature: String,
    ): Either<JointOperationCreateError, Unit> = withContext(dispatchers.io) {
        val safeTxDto = Either
            .catch { safeTxConverter.convertBack(safeTx) }
            .getOrElse { return@withContext JointOperationCreateError.Failed(cause = it).left() }

        safeApiCall(
            call = {
                jointAccountApi.createJointOperation(
                    walletId = userWalletId.stringValue,
                    body = CreateJointOperationRequest(
                        safeAddress = safeAddress,
                        chainId = chainId,
                        safeTx = safeTxDto,
                        signature = signature,
                    ),
                ).bind()

                Unit.right()
            },
            onError = { error -> createErrorConverter.convert(error).left() },
        )
    }

    override suspend fun signOperation(
        userWalletId: UserWalletId,
        operationId: String,
        safeTx: SafeTx,
        signature: String,
    ): Either<JointOperationSignError, Unit> = withContext(dispatchers.io) {
        val safeTxDto = Either
            .catch { safeTxConverter.convertBack(safeTx) }
            .getOrElse { return@withContext JointOperationSignError.Failed(cause = it).left() }

        safeApiCall(
            call = {
                jointAccountApi.signJointOperation(
                    walletId = userWalletId.stringValue,
                    operationId = operationId,
                    body = SignJointOperationRequest(
                        safeTx = safeTxDto,
                        signature = signature,
                    ),
                ).bind()

                Unit.right()
            },
            onError = { error -> signErrorConverter.convert(error).left() },
        )
    }

    override suspend fun rejectOperation(
        userWalletId: UserWalletId,
        operationId: String,
    ): Either<JointOperationRejectError, Unit> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                jointAccountApi.rejectJointOperation(
                    walletId = userWalletId.stringValue,
                    operationId = operationId,
                ).bind()

                Unit.right()
            },
            onError = { error -> rejectErrorConverter.convert(error).left() },
        )
    }

    private companion object {
        const val TAG = "JointAccountRepository"
        const val NO_JOINT_ACCOUNTS = 0
        val DEAD_INVITE_CODES = setOf(Code.CONFLICT, Code.NOT_FOUND)
    }
}