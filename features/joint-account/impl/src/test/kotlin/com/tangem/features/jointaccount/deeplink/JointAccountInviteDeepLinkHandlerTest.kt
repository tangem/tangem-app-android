package com.tangem.features.jointaccount.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountInviteDeepLinkHandlerTest {

    private val router: Router = mockk()
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(router, jointAccountFeatureToggles, analyticsEventHandler)
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns true
        coEvery { router.contains(any()) } returns false
        every { router.push(any<AppRoute>(), any()) } just Runs
        every { router.popTo(any<AppRoute>(), any()) } just Runs
    }

    @ParameterizedTest
    @ProvideTestModels
    fun handleDeepLink(model: TestModel) = runTest {
        // Arrange
        val uri = fakeUri(model.pathSegments)

        // Act
        createHandler(scope = this, deeplinkUri = uri)
        advanceUntilIdle()

        // Assert
        if (model.expectedInviteId != null) {
            verify {
                router.push(route = AppRoute.JointAccountJoin(inviteId = model.expectedInviteId), onComplete = any())
            }
            verify(exactly = 1) { analyticsEventHandler.send(JointAccountAnalyticsEvent.InviteLinkOpened()) }
        } else {
            verify(exactly = 0) { router.push(any<AppRoute>(), any()) }
            verify { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }
    }

    @Test
    fun `GIVEN feature toggle disabled WHEN handle deeplink THEN nothing is pushed`() = runTest {
        // Arrange
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns false
        val uri = fakeUri(listOf("joint-account", "join", "test-invite-id"))

        // Act
        createHandler(scope = this, deeplinkUri = uri)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(any<AppRoute>(), any()) }
        verify { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
    }

    @Test
    fun `GIVEN the same invite already in the stack WHEN handle deeplink THEN nothing is pushed`() = runTest {
        // Arrange
        coEvery { router.contains(AppRoute.JointAccountJoin(inviteId = "test-invite-id")) } returns true
        val uri = fakeUri(listOf("joint-account", "join", "test-invite-id"))

        // Act
        createHandler(scope = this, deeplinkUri = uri)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(any<AppRoute>(), any()) }
        verify(exactly = 0) { router.popTo(any<AppRoute>(), any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN another invite on top WHEN handle deeplink THEN the new invite is pushed`() = runTest {
        // Arrange
        coEvery { router.contains(AppRoute.JointAccountJoin(inviteId = "other-invite-id")) } returns true
        val uri = fakeUri(listOf("joint-account", "join", "test-invite-id"))

        // Act
        createHandler(scope = this, deeplinkUri = uri)
        advanceUntilIdle()

        // Assert
        verify { router.push(route = AppRoute.JointAccountJoin(inviteId = "test-invite-id"), onComplete = any()) }
    }

    private fun provideTestModels() = listOf(
        TestModel(
            pathSegments = listOf("joint-account", "join", "test-invite-id"),
            expectedInviteId = "test-invite-id",
        ),
        TestModel(pathSegments = listOf("joint-account", "join"), expectedInviteId = null),
        TestModel(pathSegments = listOf("joint-account"), expectedInviteId = null),
        TestModel(pathSegments = listOf("news", "1-slug"), expectedInviteId = null),
        TestModel(
            pathSegments = listOf("joint-account", "join", "test-invite-id", "extra"),
            expectedInviteId = null,
        ),
        TestModel(pathSegments = emptyList(), expectedInviteId = null),
    )

    data class TestModel(val pathSegments: List<String>, val expectedInviteId: String?)

    private fun fakeUri(segments: List<String>): Uri = mockk {
        every { pathSegments } returns segments
    }

    private fun createHandler(scope: CoroutineScope, deeplinkUri: Uri) {
        JointAccountInviteDeepLinkHandler(
            jointAccountFeatureToggles = jointAccountFeatureToggles,
            analyticsEventHandler = analyticsEventHandler,
        ).handle(
            request = DeepLinkRequest(
                uri = deeplinkUri,
                queryParams = emptyMap(),
                scope = scope,
                isFromOnNewIntent = false,
                router = router,
            ),
        )
    }
}