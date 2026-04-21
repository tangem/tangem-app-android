package com.tangem.features.onboarding.v2.addresssync.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddressSyncModelTest {

    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase = mockk()
    private val canUseBiometryUseCase: CanUseBiometryUseCase = mockk()
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase = mockk()

    @BeforeEach
    fun setUp() {
        coEvery { canUseBiometryUseCase.strict() } returns false
        coEvery { shouldShowAskBiometryUseCase() } returns false
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false
    }

    @Test
    fun `GIVEN biometry allowed AND should show biometry WHEN Next ASK_BIOMETRY THEN ASK_BIOMETRY on top`() = runTest {
        coEvery { canUseBiometryUseCase.strict() } returns true
        coEvery { shouldShowAskBiometryUseCase() } returns true

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY, shouldReplace = false))
        advanceUntilIdle()

        assert(stack == listOf(AddressSyncStep.ASK_BIOMETRY))
    }

    @Test
    fun `GIVEN biometry skipped AND notifications required WHEN Next ASK_BIOMETRY THEN ASK_NOTIFICATIONS on top`() =
        runTest {
            coEvery { canUseBiometryUseCase.strict() } returns true
            coEvery { shouldShowAskBiometryUseCase() } returns false
            coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns true

            val model = createModel(this)
            val stack = model.stackNavigation.trackStack()

            model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY, shouldReplace = false))
            advanceUntilIdle()

            assert(stack == listOf(AddressSyncStep.ASK_NOTIFICATIONS))
        }

    @Test
    fun `GIVEN biometry skipped AND notifications skipped WHEN Next ASK_BIOMETRY THEN ADDRESS_SYNC on top`() = runTest {
        coEvery { canUseBiometryUseCase.strict() } returns true
        coEvery { shouldShowAskBiometryUseCase() } returns false
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY, shouldReplace = false))
        advanceUntilIdle()

        assert(stack == listOf(AddressSyncStep.ADDRESS_SYNC))
    }

    @Test
    fun `GIVEN notifications required WHEN Next ASK_NOTIFICATIONS THEN ASK_NOTIFICATIONS on top`() = runTest {
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns true

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS, shouldReplace = false))
        advanceUntilIdle()

        assert(stack == listOf(AddressSyncStep.ASK_NOTIFICATIONS))
    }

    @Test
    fun `GIVEN notifications skipped WHEN Next ASK_NOTIFICATIONS THEN ADDRESS_SYNC on top`() = runTest {
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS, shouldReplace = false))
        advanceUntilIdle()

        assert(stack == listOf(AddressSyncStep.ADDRESS_SYNC))
    }

    private fun StackNavigation<AddressSyncStep>.trackStack(): List<AddressSyncStep> {
        val tracked = mutableListOf<AddressSyncStep>()
        subscribe { event ->
            val newStack = event.transformer(tracked.toList())
            tracked.clear()
            tracked.addAll(newStack)
        }
        return tracked
    }

    private fun createModel(testScope: TestScope): AddressSyncModel {
        return AddressSyncModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            shouldShowAskBiometryUseCase = shouldShowAskBiometryUseCase,
            canUseBiometryUseCase = canUseBiometryUseCase,
            shouldAskPermissionUseCase = shouldAskPermissionUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}