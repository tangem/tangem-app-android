package com.tangem.features.txhistory.joint.operation.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.jointaccount.model.JointOperationFetchResult
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@ModelScoped
internal class JointOperationPoller @Inject constructor(
    private val repository: JointOperationHistoryRepository,
) {

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    suspend fun poll(userWalletId: UserWalletId, operationId: String): End {
        var hasLoggedFailureRun = false

        while (!isFinished(userWalletId = userWalletId, operationId = operationId)) {
            refreshRequests.tryReceive()
            val result = repository.fetchOperation(userWalletId = userWalletId, operationId = operationId)
                .onLeft { error ->
                    if (!hasLoggedFailureRun) TangemLogger.e("Unable to fetch the joint operation $operationId", error)
                    hasLoggedFailureRun = true
                }
                .onRight { hasLoggedFailureRun = false }
                .getOrNull()

            if (result == JointOperationFetchResult.Gone) return End.GONE

            withTimeoutOrNull(POLL_INTERVAL) { refreshRequests.receive() }
        }

        return End.FINISHED
    }

    private suspend fun isFinished(userWalletId: UserWalletId, operationId: String): Boolean {
        return repository.observeOperation(userWalletId = userWalletId, operationId = operationId).first() != null
    }

    enum class End {
        FINISHED,
        GONE,
    }

    companion object {
        val POLL_INTERVAL = 5.seconds
    }
}