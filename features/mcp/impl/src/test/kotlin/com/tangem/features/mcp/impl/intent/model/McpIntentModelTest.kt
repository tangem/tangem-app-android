package com.tangem.features.mcp.impl.intent.model

import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.impl.intent.McpIntentComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class McpIntentModelTest {

    private val router: Router = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @Test
    fun `GIVEN model created WHEN created THEN router is not touched`() = runTest {
        // Act
        createModel(testScope = this)

        // Assert
        verify(exactly = 0) { router.pop(any()) }
    }

    @Test
    fun `GIVEN model WHEN onClose THEN router pops once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.onClose()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    private fun createModel(testScope: TestScope): McpIntentModel {
        val params = McpIntentComponent.Params(
            request = McpRequest.Intent(id = "my_intent_id123", source = "express", type = "exchange"),
        )
        return McpIntentModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            paramsContainer = MutableParamsContainer(value = params),
        )
    }
}