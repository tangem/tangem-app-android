package com.tangem.domain.mcp.usecase

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.models.McpIntentStatus
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMcpIntentHistoryUseCaseTest {

    private val repository: McpRepository = mockk()
    private val useCase = GetMcpIntentHistoryUseCase(repository)

    @BeforeEach
    fun setUp() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN page with other sessions WHEN invoke THEN only the session intents are returned`() = runTest {
        // Arrange
        coEvery { repository.getIntentHistory(WALLET_ID, 1, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(
                items = listOf(item("mine", SESSION_ID), item("other", "other-session")),
                nextPage = null,
            ),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = 1)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextPage = null)),
        )
    }

    @Test
    fun `GIVEN only other sessions WHEN invoke THEN empty page retains continuation after one read`() = runTest {
        // Arrange
        coEvery { repository.getIntentHistory(WALLET_ID, 1, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(
                items = List(PAGE_SIZE) { item("other-$it", "other-session") },
                nextPage = 2,
            ),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = 1)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = emptyList(), nextPage = 2)),
        )
        coVerify(exactly = 1) { repository.getIntentHistory(any(), any(), any()) }
    }

    @Test
    fun `GIVEN consecutive matching pages WHEN invoked THEN pages do not overfill or skip intents`() = runTest {
        // Arrange
        val firstItems = List(PAGE_SIZE - 1) { item("mine-$it", SESSION_ID) }
        val nextItems = List(PAGE_SIZE) { item("next-$it", SESSION_ID) }
        coEvery { repository.getIntentHistory(WALLET_ID, 1, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(
                items = firstItems + item("other", "other-session"),
                nextPage = 2,
            ),
        )
        coEvery { repository.getIntentHistory(WALLET_ID, 2, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = nextItems, nextPage = 3),
        )

        // Act
        val first = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = 1)
        val next = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = first.getOrNull()!!.nextPage!!)

        // Assert
        assertThat(listOf(first, next)).containsExactly(
            Either.Right(McpIntentHistoryPage(items = firstItems, nextPage = 2)),
            Either.Right(McpIntentHistoryPage(items = nextItems, nextPage = 3)),
        ).inOrder()
        coVerify(exactly = 1) { repository.getIntentHistory(WALLET_ID, 1, PAGE_SIZE) }
        coVerify(exactly = 1) { repository.getIntentHistory(WALLET_ID, 2, PAGE_SIZE) }
    }

    @Test
    fun `GIVEN history ends before the page is filled WHEN invoke THEN reading stops without a next page`() = runTest {
        // Arrange
        coEvery { repository.getIntentHistory(WALLET_ID, 1, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextPage = null),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = 1)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextPage = null)),
        )
    }

    @Test
    fun `GIVEN backend page fails WHEN invoke THEN the failure is returned`() = runTest {
        // Arrange
        val error = IOException()
        coEvery { repository.getIntentHistory(WALLET_ID, 2, PAGE_SIZE) } returns Either.Left(error)

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, page = 2)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
    }

    private fun item(id: String, sessionId: String) = McpIntentHistoryItem(
        intentId = id,
        source = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND),
        status = McpIntentStatus.Created,
        sessionId = sessionId,
        createdAtMillis = 0L,
    )

    private companion object {
        const val SESSION_ID = "session-id"
        const val PAGE_SIZE = 20
        val WALLET_ID = UserWalletId("aabbcc")
    }
}