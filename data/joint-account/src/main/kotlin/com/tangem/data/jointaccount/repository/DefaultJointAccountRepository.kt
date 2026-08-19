package com.tangem.data.jointaccount.repository

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.jointaccount.converter.CreateJointAccountRequestConverter
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.datasource.api.jointaccount.JointAccountApi
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.repository.JointAccountRepository
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultJointAccountRepository @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val invitesStore: JointAccountInvitesStore,
    private val converter: JointAccountDtoConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointAccountRepository {

    private val logger = TangemLogger.withTag(tag = TAG)

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

                    JointAccountCreationResult.Created(account = converter.convert(dto = response))
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

    /**
     * A failure here must not fail the creation: the account already exists on the backend, and reporting an
     * error would send the user into a retry that can only end in a conflict. The invites are lost — the
     * backend returns them once — so the fact is logged, never their content: an invite id is a secret.
     */
    private suspend fun storeInvites(userWalletId: UserWalletId, cryptoAccountId: String, invites: List<String>) {
        runSuspendCatching {
            invitesStore.store(userWalletId = userWalletId, cryptoAccountId = cryptoAccountId, invites = invites)
        }.onFailure {
            logger.e(messageString = "Failed to persist the invites of the created joint account")
        }
    }

    private companion object {
        const val TAG = "JointAccountRepository"
        const val NO_JOINT_ACCOUNTS = 0
    }
}