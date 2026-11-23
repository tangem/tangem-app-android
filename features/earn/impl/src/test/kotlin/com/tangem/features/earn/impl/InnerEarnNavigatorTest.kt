package com.tangem.features.earn.impl

import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.navigation.EarnFlowOpener
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.session.Deposit
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InnerEarnNavigatorTest {

    private val router: Router = mockk(relaxed = true)
    private val navigator = InnerEarnNavigator(router, EarnFlowOpener.InfoScreen)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router)
    }

    @Test
    fun `GIVEN route WHEN navigate THEN router push with that route`() {
        // Arrange
        val route = EarnInfoRoute(EarnProductId("lending"))

        // Act
        navigator.navigate(route)

        // Assert
        verify(exactly = 1) { router.push(route, any()) }
    }

    @Test
    fun `GIVEN flow route WHEN navigate THEN router push with the screen that opened the flow`() {
        // Arrange
        val route = EarnFlowRoute(productId = EarnProductId("lending"), action = Deposit())

        // Act
        navigator.navigate(route)

        // Assert
        verify(exactly = 1) { router.push(route.copy(opener = EarnFlowOpener.InfoScreen), any()) }
    }
}