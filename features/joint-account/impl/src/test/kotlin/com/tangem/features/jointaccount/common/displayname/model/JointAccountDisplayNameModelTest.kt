package com.tangem.features.jointaccount.common.displayname.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.common.JointAccountDisplayNameComponent
import com.tangem.features.jointaccount.common.displayname.state.JointAccountDisplayNameStateController
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class JointAccountDisplayNameModelTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk {
        every { userWallets } returns MutableStateFlow(value = null)
    }

    @Test
    fun `GIVEN name with surrounding spaces WHEN continue THEN the trimmed name is handed on`() = runTest {
        // Arrange
        var handedName: String? = null
        val model = createModel(testScope = this, onContinueClick = { name -> handedName = name })
        model.uiState.value.onNameChange(" Alice ")

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        assertThat(handedName).isEqualTo("Alice")
        assertThat(model.uiState.value.name).isEqualTo(" Alice ")
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        onContinueClick: suspend (String) -> Unit,
    ): JointAccountDisplayNameModel = JointAccountDisplayNameModel(
        paramsContainer = MutableParamsContainer(
            value = JointAccountDisplayNameComponent.Params(
                userWalletId = UserWalletId("011"),
                buttonText = stringReference("Continue"),
                initialName = null,
                onContinueClick = onContinueClick,
                onCloseClick = {},
            ),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        stateController = JointAccountDisplayNameStateController(),
        userWalletsListRepository = userWalletsListRepository,
    )
}