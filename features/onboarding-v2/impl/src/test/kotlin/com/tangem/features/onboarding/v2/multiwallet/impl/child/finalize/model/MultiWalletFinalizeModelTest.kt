package com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.features.onboarding.v2.common.ui.CantLeaveBackupDialog
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.MultiWalletFinalizeComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.ui.state.MultiWalletFinalizeUM
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
internal class MultiWalletFinalizeModelTest {

    private val backupServiceHolder: BackupServiceHolder = mockk()
    private val backupService: BackupService = mockk()
    private val backupServiceWeakRef: WeakReference<BackupService> = WeakReference(backupService)
    private val tangemSdkManager: TangemSdkManager = mockk(relaxUnitFun = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk()
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory = mockk()
    private val saveWalletUseCase: SaveWalletUseCase = mockk()
    private val getUserWalletsUseCase: GetWalletsUseCase = mockk()
    private val updateWalletUseCase: UpdateWalletUseCase = mockk()
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase = mockk()
    private val cardRepository: CardRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val walletsRepository: WalletsRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val backupValidator: BackupValidator = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val paramsContainer: ParamsContainer = mockk()

    private val scanResponse: ScanResponse = mockk()

    private val multiWalletStateFlow = MutableStateFlow(
        OnboardingMultiWalletState(
            currentStep = OnboardingMultiWalletState.Step.Finalize,
            accessCode = null,
            isThreeCards = true,
            currentScanResponse = scanResponse,
            startFromFinalize = null,
            resultUserWallet = null,
        ),
    )

    private val parentParams: OnboardingMultiWalletComponent.Params = mockk {
        every { mode } returns OnboardingMultiWalletComponent.Mode.Onboarding
        every { scanResponse } returns this@MultiWalletFinalizeModelTest.scanResponse
    }

    private val params: MultiWalletChildParams = mockk {
        every { multiWalletState } returns multiWalletStateFlow
        every { parentParams } returns this@MultiWalletFinalizeModelTest.parentParams
    }

    @BeforeEach
    fun setUp() {
        every { paramsContainer.require<MultiWalletChildParams>() } returns params
        every { backupServiceHolder.backupService } returns backupServiceWeakRef
        every { backupService.primaryCardId } returns "primary-id-aaaa"
        every { backupService.primaryCardBatchId } returns NON_RING_BATCH_ID
        every { backupService.backupCardIds } returns listOf("backup-1-bbbb", "backup-2-cccc")
        every { backupService.backupCardsBatchIds } returns listOf(NON_RING_BATCH_ID, NON_RING_BATCH_ID)
        every { backupService.currentState } returns BackupService.State.FinalizingPrimaryCard
        coEvery { onboardingRepository.saveUnfinishedFinalizeOnboarding(any()) } just Runs
    }

    @Test
    fun `WHEN init AND startFromFinalize is null THEN no events emitted`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(startFromFinalize = null)

        val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect { events.add(it) } }
        advanceUntilIdle()

        Assertions.assertEquals(emptyList<MultiWalletFinalizeComponent.Event>(), events)
    }

    @Test
    fun `WHEN init AND startFromFinalize is ScanBackupFirstCard THEN OneBackupCardAdded emitted`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )

        val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect { events.add(it) } }
        advanceUntilIdle()

        Assertions.assertEquals(
            listOf(MultiWalletFinalizeComponent.Event.OneBackupCardAdded),
            events,
        )
    }

    @Test
    fun `WHEN init AND startFromFinalize is ScanBackupSecondCard THEN both events emitted in order`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupSecondCard,
        )

        val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect { events.add(it) } }
        advanceUntilIdle()

        Assertions.assertEquals(
            listOf(
                MultiWalletFinalizeComponent.Event.OneBackupCardAdded,
                MultiWalletFinalizeComponent.Event.TwoBackupCardsAdded,
            ),
            events,
        )
    }

    @Test
    fun `GIVEN backupService is null WHEN model is created THEN initial state is default`() = runTest {
        every { backupServiceHolder.backupService } returns WeakReference(null)

        val model = createModel(this)
        advanceUntilIdle()

        val state = model.uiState.value
        Assertions.assertEquals(MultiWalletFinalizeUM.Step.Primary, state.step)
        Assertions.assertEquals(true, state.scanPrimary)
        Assertions.assertEquals("", state.cardNumber)
        Assertions.assertEquals(false, state.isRing)
    }

    @Test
    fun `GIVEN startFromFinalize null AND non-Ring primary WHEN model is created THEN state is Primary non-Ring`() =
        runTest {
            multiWalletStateFlow.value = multiWalletStateFlow.value.copy(startFromFinalize = null)
            every { backupService.primaryCardBatchId } returns NON_RING_BATCH_ID

            val model = createModel(this)
            advanceUntilIdle()

            val state = model.uiState.value
            Assertions.assertEquals(MultiWalletFinalizeUM.Step.Primary, state.step)
            Assertions.assertEquals(true, state.scanPrimary)
            Assertions.assertEquals(false, state.isRing)
            Assertions.assertEquals("primary-id-aaaa".lastMaskedExpected(), state.cardNumber)
        }

    @Test
    fun `GIVEN startFromFinalize ScanPrimaryCard AND Ring primary WHEN model is created THEN state is Primary Ring`() =
        runTest {
            multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
                startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanPrimaryCard,
            )
            every { backupService.primaryCardBatchId } returns RING_BATCH_ID_AC17

            val model = createModel(this)
            advanceUntilIdle()

            val state = model.uiState.value
            Assertions.assertEquals(MultiWalletFinalizeUM.Step.Primary, state.step)
            Assertions.assertEquals(true, state.scanPrimary)
            Assertions.assertEquals(true, state.isRing)
        }

    @Test
    fun `GIVEN startFromFinalize ScanBackupFirstCard WHEN model is created THEN state is BackupDevice1`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        val state = model.uiState.value
        Assertions.assertEquals(MultiWalletFinalizeUM.Step.BackupDevice1, state.step)
        Assertions.assertEquals(false, state.scanPrimary)
        Assertions.assertEquals("backup-1-bbbb".lastMaskedExpected(), state.cardNumber)
    }

    @Test
    fun `GIVEN startFromFinalize ScanBackupSecondCard WHEN model is created THEN state is BackupDevice2`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupSecondCard,
        )

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        val state = model.uiState.value
        Assertions.assertEquals(MultiWalletFinalizeUM.Step.BackupDevice2, state.step)
        Assertions.assertEquals(false, state.scanPrimary)
        Assertions.assertEquals("backup-2-cccc".lastMaskedExpected(), state.cardNumber)
    }

    @Test
    fun `GIVEN scanPrimary true WHEN onBack THEN onBackFlow emits Unit`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(startFromFinalize = null)

        val model = createModel(this)
        val received = mutableListOf<Unit>()
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onBackFlow.collect { received.add(it) } }
        advanceUntilIdle()

        model.onBack()
        advanceUntilIdle()

        Assertions.assertEquals(listOf(Unit), received)
        verify(exactly = 0) { uiMessageSender.send(CantLeaveBackupDialog) }
    }

    @Test
    fun `GIVEN scanPrimary false WHEN onBack THEN CantLeaveBackupDialog is sent`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        model.onBack()
        advanceUntilIdle()

        verify { uiMessageSender.send(CantLeaveBackupDialog) }
    }

    @Test
    fun `GIVEN primary batchId is null WHEN onScanClick THEN proceedBackup is not called`() = runTest {
        every { backupService.primaryCardBatchId } returns null

        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify(exactly = 0) { backupService.proceedBackup(iconScanRes = any(), callback = any()) }
        verify(exactly = 0) { tangemSdkManager.changeProductType(any()) }
    }

    @Test
    fun `GIVEN non-Ring primary AND success WHEN onScanClick THEN state moves to BackupDevice1`() = runTest {
        every { backupService.primaryCardBatchId } returns NON_RING_BATCH_ID
        val callbackSlot = slot<(CompletionResult<Card>) -> Unit>()
        every {
            backupService.proceedBackup(iconScanRes = null, callback = capture(callbackSlot))
        } just Runs

        val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect { events.add(it) } }
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify { tangemSdkManager.changeProductType(false) }
        verify { backupService.proceedBackup(iconScanRes = null, callback = any()) }

        callbackSlot.captured.invoke(CompletionResult.Success(mockk()))
        advanceUntilIdle()

        verify { tangemSdkManager.clearProductType() }
        coVerify { onboardingRepository.saveUnfinishedFinalizeOnboarding(scanResponse = scanResponse) }

        val state = model.uiState.value
        Assertions.assertEquals(MultiWalletFinalizeUM.Step.BackupDevice1, state.step)
        Assertions.assertEquals(false, state.scanPrimary)
        Assertions.assertEquals("backup-1-bbbb".lastMaskedExpected(), state.cardNumber)
        Assertions.assertEquals(false, state.isRing)
        Assertions.assertEquals(
            listOf(MultiWalletFinalizeComponent.Event.OneBackupCardAdded),
            events,
        )
    }

    @Test
    fun `GIVEN Ring primary WHEN onScanClick THEN changeProductType is true and ring icon is used`() = runTest {
        every { backupService.primaryCardBatchId } returns RING_BATCH_ID_AC17
        every {
            backupService.proceedBackup(iconScanRes = any(), callback = any())
        } just Runs

        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify { tangemSdkManager.changeProductType(true) }
        verify {
            backupService.proceedBackup(
                iconScanRes = com.tangem.features.onboarding.v2.impl.R.drawable.img_hand_scan_ring,
                callback = any(),
            )
        }
    }

    @Test
    fun `GIVEN primary AND failure WHEN onScanClick THEN state is unchanged AND no event emitted`() = runTest {
        every { backupService.primaryCardBatchId } returns NON_RING_BATCH_ID
        val callbackSlot = slot<(CompletionResult<Card>) -> Unit>()
        every {
            backupService.proceedBackup(iconScanRes = null, callback = capture(callbackSlot))
        } just Runs

        val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect { events.add(it) } }
        advanceUntilIdle()
        val stateBefore = model.uiState.value

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()
        callbackSlot.captured.invoke(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        advanceUntilIdle()

        verify { tangemSdkManager.clearProductType() }
        coVerify(exactly = 0) { onboardingRepository.saveUnfinishedFinalizeOnboarding(any()) }
        Assertions.assertEquals(stateBefore.step, model.uiState.value.step)
        Assertions.assertEquals(stateBefore.scanPrimary, model.uiState.value.scanPrimary)
        Assertions.assertEquals(emptyList<MultiWalletFinalizeComponent.Event>(), events)
    }

    @Test
    fun `GIVEN BackupDevice1 AND batchId null WHEN onScanClick THEN proceedBackup is not called`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )
        every { backupService.backupCardsBatchIds } returns emptyList()

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()

        verify(exactly = 0) { backupService.proceedBackup(iconScanRes = any(), callback = any()) }
    }

    @Test
    fun `GIVEN BackupDevice1 AND failure WalletAlreadyCreated WHEN onScanClick THEN dialog is set`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )
        val callbackSlot = slot<(CompletionResult<Card>) -> Unit>()
        every {
            backupService.proceedBackup(iconScanRes = null, callback = capture(callbackSlot))
        } just Runs

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()
        callbackSlot.captured.invoke(CompletionResult.Failure(TangemSdkError.WalletAlreadyCreated()))
        advanceUntilIdle()

        Assertions.assertNotNull(model.uiState.value.dialog)
        verify { tangemSdkManager.clearProductType() }
    }

    @Test
    fun `GIVEN BackupDevice1 AND failure other error WHEN onScanClick THEN no dialog is set`() = runTest {
        multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
            startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
        )
        val callbackSlot = slot<(CompletionResult<Card>) -> Unit>()
        every {
            backupService.proceedBackup(iconScanRes = null, callback = capture(callbackSlot))
        } just Runs

        val model = createModel(this)
        backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) { model.onEvent.collect {} }
        advanceUntilIdle()

        model.uiState.value.onScanClick.invoke()
        advanceUntilIdle()
        callbackSlot.captured.invoke(CompletionResult.Failure(TangemSdkError.UserCancelled()))
        advanceUntilIdle()

        Assertions.assertNull(model.uiState.value.dialog)
        verify { tangemSdkManager.clearProductType() }
    }

    @Test
    fun `GIVEN BackupDevice1 AND success AND not Finished WHEN onScanClick THEN state moves to BackupDevice2`() =
        runTest {
            multiWalletStateFlow.value = multiWalletStateFlow.value.copy(
                startFromFinalize = OnboardingMultiWalletState.FinalizeStage.ScanBackupFirstCard,
            )
            every { backupService.currentState } returns BackupService.State.FinalizingBackupCard(index = 1)

            mockkConstructor(BackupValidator::class)
            every { anyConstructed<BackupValidator>().isValidBackupStatus(any()) } returns true

            val card: Card = mockk(relaxed = true)
            val callbackSlot = slot<(CompletionResult<Card>) -> Unit>()
            every {
                backupService.proceedBackup(iconScanRes = null, callback = capture(callbackSlot))
            } just Runs

            val events = mutableListOf<MultiWalletFinalizeComponent.Event>()
            val model = createModel(this)
            backgroundScope.launch(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                model.onEvent.collect { events.add(it) }
            }
            advanceUntilIdle()

            model.uiState.value.onScanClick.invoke()
            advanceUntilIdle()
            callbackSlot.captured.invoke(CompletionResult.Success(card))
            advanceUntilIdle()

            val state = model.uiState.value
            Assertions.assertEquals(MultiWalletFinalizeUM.Step.BackupDevice2, state.step)
            Assertions.assertEquals("backup-2-cccc".lastMaskedExpected(), state.cardNumber)
            Assertions.assertTrue(events.contains(MultiWalletFinalizeComponent.Event.TwoBackupCardsAdded))

            unmockkConstructor(BackupValidator::class)
        }

    private fun String.lastMaskedExpected(): String {
        val space = ' '
        val last4 = takeLast(4)
        return "$space*$space*$space*$space$last4"
    }

    private fun createModel(testScope: TestScope): MultiWalletFinalizeModel {
        return MultiWalletFinalizeModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            backupServiceHolder = backupServiceHolder,
            tangemSdkManager = tangemSdkManager,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            coldUserWalletBuilderFactory = coldUserWalletBuilderFactory,
            saveWalletUseCase = saveWalletUseCase,
            getUserWalletsUseCase = getUserWalletsUseCase,
            updateWalletUseCase = updateWalletUseCase,
            syncWalletWithRemoteUseCase = syncWalletWithRemoteUseCase,
            cardRepository = cardRepository,
            onboardingRepository = onboardingRepository,
            walletsRepository = walletsRepository,
            uiMessageSender = uiMessageSender,
            backupValidator = backupValidator,
            analyticsEventHandler = analyticsEventHandler,
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

    private companion object {
        private const val NON_RING_BATCH_ID = "AC02"
        private const val RING_BATCH_ID_AC17 = "AC17"
    }
}