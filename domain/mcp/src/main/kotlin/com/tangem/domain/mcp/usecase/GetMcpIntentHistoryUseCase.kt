package com.tangem.domain.mcp.usecase

import arrow.core.Either
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Reads one backend page and keeps only intents of the requested agent session.
 *
 * The service cannot filter by session, so the result may be partial or empty while still having a next page.
 */
class GetMcpIntentHistoryUseCase @Inject constructor(
    private val mcpRepository: McpRepository,
) {

    suspend operator fun invoke(
        walletId: UserWalletId,
        sessionId: String,
        page: Int,
    ): Either<Throwable, McpIntentHistoryPage> =
        mcpRepository.getIntentHistory(walletId = walletId, page = page, limit = PAGE_SIZE).map { history ->
            history.copy(items = history.items.filter { it.sessionId == sessionId })
        }

    private companion object {
        const val PAGE_SIZE = 20
    }
}