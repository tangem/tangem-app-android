package com.tangem.features.jointaccount.creation.model

import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountFlowError
import com.tangem.domain.jointaccount.usecase.CreateJointAccountUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountCreationModelTest {

    private val createJointAccount: CreateJointAccountUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(createJointAccount, router, uiMessageSender, analyticsEventHandler)
    }

    @Test
    fun `GIVEN creation succeeds WHEN createAccount THEN created event carries the account composition`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { createJointAccount(any(), any(), any()) } returns createdAccount.right()

        // Act
        model.createAccount(displayName = NAME)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = JointAccountAnalyticsEvent.JointAccountCreated(membersCount = 3, threshold = 2),
            )
        }
    }

    @Test
    fun `GIVEN backend refuses with a body code WHEN createAccount THEN error event carries that code`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        val cause = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.BAD_REQUEST,
            message = "Bad request",
            errorBody = """{"code":"INVALID_SIGNATURE"}""",
        )
        coEvery { createJointAccount(any(), any(), any()) } returns JointAccountFlowError.Failed(cause).left()

        // Act
        model.createAccount(displayName = NAME)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                event = JointAccountAnalyticsEvent.JointAccountError(source = ErrorSource.Create, cause = cause),
            )
        }
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN signing dismissed WHEN createAccount THEN no event is sent`() = runTest {
        // Arrange
        val model = createModelWithDraft()
        coEvery { createJointAccount(any(), any(), any()) } returns JointAccountFlowError.UserCancelled.left()

        // Act
        model.createAccount(displayName = NAME)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN incomplete draft WHEN createAccount THEN error event is sent`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.createAccount(displayName = NAME)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(event = ofType<JointAccountAnalyticsEvent.JointAccountError>())
        }
        verify(exactly = 1) { uiMessageSender.send(any()) }
    }

    private fun createModelWithDraft(): JointAccountCreationModel = createModel().apply {
        draftHolder.setConfig(
            JointAccountCreationDraft.Config(
                name = "Family",
                icon = CryptoPortfolioIcon.Icon.entries.first(),
                color = CryptoPortfolioIcon.Color.entries.first(),
                walletId = USER_WALLET_ID,
            ),
        )
        draftHolder.setComposition(JointAccountCreationDraft.Composition(totalMembers = 3, requiredToSign = 2))
    }

    private fun createModel(): JointAccountCreationModel = JointAccountCreationModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        createJointAccount = createJointAccount,
        router = router,
        uiMessageSender = uiMessageSender,
        analyticsEventHandler = analyticsEventHandler,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val NAME = "Alice"

        val createdAccount = JointAccount(
            cryptoAccountId = "a1".repeat(n = 32),
            membersCount = 3,
            threshold = 2,
            safeAddress = null,
            status = JointAccount.Status.PENDING,
            members = emptyList(),
            source = StatusSource.ACTUAL,
        )
    }
}