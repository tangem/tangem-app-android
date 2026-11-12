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
        coEvery { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(
                items = listOf(item("mine", SESSION_ID), item("other", "other-session")),
                nextOffset = null,
            ),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, offset = 0)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextOffset = null)),
        )
    }

    @Test
    fun `GIVEN session intents are sparse WHEN invoke THEN pages are read until the page is filled`() = runTest {
        // Arrange
        coEvery { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = listOf(item("other", "other-session")), nextOffset = 20),
        )
        coEvery { repository.getIntentHistory(WALLET_ID, 20, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = List(PAGE_SIZE) { item("mine-$it", SESSION_ID) }, nextOffset = 40),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, offset = 0)

        // Assert
        assertThat(actual.getOrNull()!!.nextOffset).isEqualTo(40)
        coVerify(exactly = 1) { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) }
        coVerify(exactly = 1) { repository.getIntentHistory(WALLET_ID, 20, PAGE_SIZE) }
    }

    @Test
    fun `GIVEN a partially filled page WHEN invoke THEN only remaining capacity is requested`() = runTest {
        // Arrange
        val firstItems = List(PAGE_SIZE - 1) { item("mine-$it", SESSION_ID) }
        val lastItem = item("last", SESSION_ID)
        coEvery { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = firstItems, nextOffset = PAGE_SIZE),
        )
        coEvery { repository.getIntentHistory(WALLET_ID, PAGE_SIZE, 1) } returns Either.Right(
            McpIntentHistoryPage(items = listOf(lastItem), nextOffset = PAGE_SIZE + 1),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, offset = 0)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = firstItems + lastItem, nextOffset = PAGE_SIZE + 1)),
        )
        coVerify(exactly = 1) { repository.getIntentHistory(WALLET_ID, PAGE_SIZE, 1) }
    }

    @Test
    fun `GIVEN history ends before the page is filled WHEN invoke THEN reading stops without a next page`() = runTest {
        // Arrange
        coEvery { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextOffset = null),
        )

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, offset = 0)

        // Assert
        assertThat(actual).isEqualTo(
            Either.Right(McpIntentHistoryPage(items = listOf(item("mine", SESSION_ID)), nextOffset = null)),
        )
    }

    @Test
    fun `GIVEN a later page fails WHEN invoke THEN the failure is returned`() = runTest {
        // Arrange
        val error = IOException()
        coEvery { repository.getIntentHistory(WALLET_ID, 0, PAGE_SIZE) } returns Either.Right(
            McpIntentHistoryPage(items = emptyList(), nextOffset = 20),
        )
        coEvery { repository.getIntentHistory(WALLET_ID, 20, PAGE_SIZE) } returns Either.Left(error)

        // Act
        val actual = useCase(walletId = WALLET_ID, sessionId = SESSION_ID, offset = 0)

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