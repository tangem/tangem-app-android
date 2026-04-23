package com.tangem.features.onboarding.v2.addresssync.model

import arrow.core.Either
import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.settings.ShouldShowAskBiometryUseCase
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.MultiWalletInnerNavigationState
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddressSyncModelTest {

    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase = mockk()
    private val canUseBiometryUseCase: CanUseBiometryUseCase = mockk()
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase = mockk()
    private val multiWalletAccountListFetcher: MultiWalletAccountListFetcher = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val paramsContainer: ParamsContainer = mockk()
    private val testInnerNavigation = MutableStateFlow(
        value = MultiWalletInnerNavigationState(
            stackSize = 0,
            stackMaxSize = 0,
        )
    )
    private val titleProvider: TitleProvider = mockk(relaxUnitFun = true)
    private val walletId = UserWalletId("011")
    private val params: MultiWalletChildParams = mockk {
        every { innerNavigation } returns testInnerNavigation
        every { parentParams } returns mockk {
            every { titleProvider } returns this@AddressSyncModelTest.titleProvider
            every { mode } returns OnboardingMultiWalletComponent.Mode.AddressSync(walletId)
        }
    }

    @BeforeEach
    fun setUp() {
        coEvery { canUseBiometryUseCase.strict() } returns false
        coEvery { shouldShowAskBiometryUseCase() } returns false
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false
        coEvery { multiWalletAccountListFetcher.invoke(any()) } returns Either.Right(Unit)
        every { multiAccountListSupplier() } returns flowOf(
            listOf(AccountList.empty(userWalletId = walletId)),
        )
        every { paramsContainer.require<MultiWalletChildParams>() } returns params
    }

    @Test
    fun `WHEN model is created THEN inner navigation stack size and max size are set`() = runTest {
        createModel(this)

        val state = testInnerNavigation.value
        Assertions.assertEquals(AddressSyncStep.ASK_BIOMETRY.pageNumber, state.stackSize)
        Assertions.assertEquals(AddressSyncStep.entries.size, state.stackMaxSize)
    }

    @Test
    fun `GIVEN biometry allowed AND should show biometry WHEN Next ASK_BIOMETRY THEN ASK_BIOMETRY on top`() = runTest {
        coEvery { canUseBiometryUseCase.strict() } returns true
        coEvery { shouldShowAskBiometryUseCase() } returns true

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY))
        advanceUntilIdle()

        Assertions.assertEquals(listOf(AddressSyncStep.ASK_BIOMETRY), stack)
        assertStepperAndTitleFor(AddressSyncStep.ASK_BIOMETRY)
    }

    @Test
    fun `GIVEN biometry skipped AND notifications required WHEN Next ASK_BIOMETRY THEN ASK_NOTIFICATIONS on top`() =
        runTest {
            coEvery { canUseBiometryUseCase.strict() } returns true
            coEvery { shouldShowAskBiometryUseCase() } returns false
            coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns true

            val model = createModel(this)
            val stack = model.stackNavigation.trackStack()

            model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY))
            advanceUntilIdle()

            Assertions.assertEquals(listOf(AddressSyncStep.ASK_NOTIFICATIONS), stack)
            assertStepperAndTitleFor(AddressSyncStep.ASK_NOTIFICATIONS)
        }

    @Test
    fun `GIVEN biometry skipped AND notifications skipped WHEN Next ASK_BIOMETRY THEN ADDRESS_SYNC on top`() = runTest {
        coEvery { canUseBiometryUseCase.strict() } returns true
        coEvery { shouldShowAskBiometryUseCase() } returns false
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_BIOMETRY))
        advanceUntilIdle()

        Assertions.assertEquals(listOf(AddressSyncStep.ADDRESS_SYNC), stack)
        assertStepperAndTitleFor(AddressSyncStep.ADDRESS_SYNC)
    }

    @Test
    fun `GIVEN notifications required WHEN Next ASK_NOTIFICATIONS THEN ASK_NOTIFICATIONS on top`() = runTest {
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns true

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS))
        advanceUntilIdle()

        Assertions.assertEquals(listOf(AddressSyncStep.ASK_NOTIFICATIONS), stack)
        assertStepperAndTitleFor(AddressSyncStep.ASK_NOTIFICATIONS)
    }

    @Test
    fun `GIVEN notifications skipped WHEN Next ASK_NOTIFICATIONS THEN ADDRESS_SYNC on top`() = runTest {
        coEvery { shouldAskPermissionUseCase(PUSH_PERMISSION) } returns false

        val model = createModel(this)
        val stack = model.stackNavigation.trackStack()

        model.onIntent(AddressSyncIntent.Next(step = AddressSyncStep.ASK_NOTIFICATIONS))
        advanceUntilIdle()

        Assertions.assertEquals(listOf(AddressSyncStep.ADDRESS_SYNC), stack)
        assertStepperAndTitleFor(AddressSyncStep.ADDRESS_SYNC)
    }

    @Test
    fun `GIVEN multiAccountListSupplier emits no currencies WHEN model is created THEN state is NoTokens`() = runTest {
        every { multiAccountListSupplier() } returns flowOf(
            listOf(
                AccountList.empty(
                    userWalletId = walletId,
                    cryptoCurrencies = emptyList(),
                ),
            ),
        )

        val model = createModel(this)
        advanceUntilIdle()

        coVerify {
            multiWalletAccountListFetcher.invoke(
                params = MultiWalletAccountListFetcher.Params(userWalletId = walletId)
            )
        }
        Assertions.assertEquals(AddressSyncState.NoTokens, model.state.value)
    }

    @Test
    fun `GIVEN multiAccountListSupplier emits currencies WHEN model is created THEN state is Success`() = runTest {
        val currencies = listOf<CryptoCurrency>(mockk(), mockk(), mockk())
        every { multiAccountListSupplier() } returns flowOf(
            listOf(
                AccountList.empty(
                    userWalletId = walletId,
                    cryptoCurrencies = currencies,
                ),
            ),
        )

        val model = createModel(this)
        advanceUntilIdle()

        coVerify {
            multiWalletAccountListFetcher.invoke(
                params = MultiWalletAccountListFetcher.Params(userWalletId = walletId)
            )
        }
        Assertions.assertEquals(
            AddressSyncState.Success(currenciesCount = currencies.size),
            model.state.value,
        )
    }

    @Test
    fun `WHEN multiWalletAccountListFetcher emits error WHEN model is created THEN get NoToken state`() = runTest {
        val currencies = listOf<CryptoCurrency>(mockk(), mockk(), mockk())
        coEvery { multiWalletAccountListFetcher.invoke(any()) } returns Either.Left(
            value = IllegalStateException("Test")
        )

        every { multiAccountListSupplier() } returns flowOf(
            listOf(
                AccountList.empty(
                    userWalletId = walletId,
                    cryptoCurrencies = currencies,
                ),
            ),
        )

        val model = createModel(this)
        advanceUntilIdle()

        coVerify {
            multiWalletAccountListFetcher.invoke(
                params = MultiWalletAccountListFetcher.Params(userWalletId = walletId)
            )
        }
        Assertions.assertEquals(
            AddressSyncState.NoTokens,
            model.state.value,
        )
    }

    private fun assertStepperAndTitleFor(step: AddressSyncStep) {
        Assertions.assertEquals(step.pageNumber, testInnerNavigation.value.stackSize)
        verify { titleProvider.changeTitle(resourceReference(step.stringId)) }
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
            multiWalletAccountListFetcher = multiWalletAccountListFetcher,
            multiAccountListSupplier = multiAccountListSupplier,
            paramsContainer = paramsContainer,
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