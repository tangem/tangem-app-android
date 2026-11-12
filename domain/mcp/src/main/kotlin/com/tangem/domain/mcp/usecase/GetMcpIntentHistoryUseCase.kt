package com.tangem.domain.mcp.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * Reads the intents of one agent session.
 *
 * The service cannot filter by session, so a page is filled by reading the service's pages until [PAGE_SIZE] items of
 * the session are found or the history ends.
 */
class GetMcpIntentHistoryUseCase @Inject constructor(
    private val mcpRepository: McpRepository,
) {

    suspend operator fun invoke(
        walletId: UserWalletId,
        sessionId: String,
        offset: Int,
    ): Either<Throwable, McpIntentHistoryPage> = either {
        val items = mutableListOf<McpIntentHistoryItem>()
        var nextOffset: Int? = offset

        while (items.size < PAGE_SIZE && nextOffset != null) {
            val page = mcpRepository.getIntentHistory(walletId = walletId, offset = nextOffset, limit = PAGE_SIZE)
                .bind()
            items += page.items.filter { it.sessionId == sessionId }
            nextOffset = page.nextOffset
        }

        McpIntentHistoryPage(items = items, nextOffset = nextOffset)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}