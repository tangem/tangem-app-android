package com.tangem.features.tangempay.card.gpay

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayCardProvisioningData
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.card.gpay.TangemPayAddToGoogleWalletController.Entry
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.test.core.ProvideTestModels
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.core.ui.R as CoreUiR

internal class TangemPayAddToGoogleWalletControllerTest {

    private val provisioner: GoogleWalletProvisioner = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val uiMessageSender: UiMessageSender = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val featureToggles: TangemPayFeatureToggles = mockk()
    private val context: Context = mockk()

    private val walletChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val sentMessages = mutableListOf<UiMessage>()

    @BeforeEach
    fun setUp() {
        clearMocks(provisioner, cardDetailsRepository, uiMessageSender, analytics, featureToggles, context)
        sentMessages.clear()
        every { featureToggles.isGoogleWalletPushProvisioningEnabled } returns true
        every { featureToggles.isGoogleWalletButtonAlwaysShown } returns false
        every { context.getString(R.string.tangempay_title) } returns DEFAULT_CARD_NAME
        every { uiMessageSender.send(capture(sentMessages)) } just runs
        every { provisioner.environment } returns Environment.TEST
        every { provisioner.isAdditionInProgress } returns false
        every { provisioner.walletChanges } returns walletChanges
        coEvery { provisioner.getCardStatus(any()) } returns CardStatus.NotAdded
        coEvery { provisioner.addCard(any()) } returns AddResult.Added
        coEvery { provisioner.resumeAddingCard(any(), any()) } returns AddResult.Added
        every { cardDetailsRepository.isProductionEnvironment() } returns false
        coEvery { cardDetailsRepository.getProvisioningData(any(), any()) } returns PROVISIONING_DATA.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Entries {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card and wallet status WHEN card shown THEN entry resolved`(model: EntryModel) = runTest {
            // Arrange
            every { featureToggles.isGoogleWalletButtonAlwaysShown } returns model.isButtonAlwaysShown
            coEvery { provisioner.getCardStatus(any()) } returns model.walletStatus
            val controller = createController()

            // Act
            controller.show(card = model.card, frozenState = model.frozenState)
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            EntryModel(walletStatus = CardStatus.NotAdded, expected = BUTTON),
            EntryModel(walletStatus = CardStatus.Incomplete, expected = BUTTON),
            EntryModel(walletStatus = CardStatus.Added, expected = Entry.Hidden),
            EntryModel(walletStatus = CardStatus.Unknown, expected = Entry.Hidden),
            EntryModel(walletStatus = CardStatus.Unsupported, expected = Entry.ManualGuide),
            EntryModel(frozenState = TangemPayCardFrozenState.Frozen, expected = Entry.Hidden),
            EntryModel(frozenState = TangemPayCardFrozenState.Pending, expected = Entry.Hidden),
            EntryModel(
                walletStatus = CardStatus.Unsupported,
                frozenState = TangemPayCardFrozenState.Frozen,
                expected = Entry.Hidden,
            ),
            EntryModel(card = null, expected = Entry.Hidden),
            EntryModel(card = addableCard(isPlaceholder = true), expected = Entry.Hidden),
            EntryModel(card = addableCard(cardStatus = TangemPayCard.Status.INACTIVE), expected = Entry.Hidden),
            EntryModel(card = addableCard(state = TangemPayCardState.Reissuing), expected = Entry.Hidden),
            EntryModel(card = addableCard(lastDigits = ""), expected = Entry.Hidden),
            EntryModel(walletStatus = CardStatus.Unsupported, isButtonAlwaysShown = true, expected = BUTTON),
            EntryModel(walletStatus = CardStatus.Added, isButtonAlwaysShown = true, expected = BUTTON),
            EntryModel(
                frozenState = TangemPayCardFrozenState.Frozen,
                isButtonAlwaysShown = true,
                expected = Entry.Hidden,
            ),
            EntryModel(
                card = addableCard(cardStatus = TangemPayCard.Status.INACTIVE),
                isButtonAlwaysShown = true,
                expected = Entry.Hidden,
            ),
        )

        @ParameterizedTest
        @MethodSource("provideDisabledModels")
        fun `GIVEN push provisioning disabled WHEN card shown THEN manual guide follows frozen state only`(
            model: DisabledModel,
        ) = runTest {
            // Arrange
            every { featureToggles.isGoogleWalletPushProvisioningEnabled } returns false
            val controller = createController()

            // Act
            controller.show(card = model.card, frozenState = model.frozenState)
            controller.onStart()
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(model.expected)
            verify { provisioner wasNot Called }
        }

        private fun provideDisabledModels() = listOf(
            DisabledModel(
                card = addableCard(),
                frozenState = TangemPayCardFrozenState.Unfrozen,
                expected = Entry.ManualGuide,
            ),
            DisabledModel(card = null, frozenState = TangemPayCardFrozenState.Unfrozen, expected = Entry.ManualGuide),
            DisabledModel(card = addableCard(), frozenState = TangemPayCardFrozenState.Frozen, expected = Entry.Hidden),
            DisabledModel(
                card = addableCard(),
                frozenState = TangemPayCardFrozenState.Pending,
                expected = Entry.Hidden,
            ),
        )

        @Test
        fun `GIVEN wallet status not known yet WHEN card shown THEN entry hidden`() = runTest {
            // Arrange
            coEvery { provisioner.getCardStatus(any()) } coAnswers { awaitCancellation() }
            val controller = createController()

            // Act
            controller.show(card = addableCard())
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Hidden)
        }

        @Test
        fun `GIVEN button shown WHEN card data becomes cached THEN button hidden`() = runTest {
            // Arrange
            val controller = createCheckedController()

            // Act
            controller.selectCard(card = null)

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Hidden)
        }

        @Test
        fun `GIVEN previous card check pending WHEN another card selected THEN its status is ignored`() = runTest {
            // Arrange
            val firstCardStatus = CompletableDeferred<CardStatus>()
            coEvery { provisioner.getCardStatus(LAST_FOUR_DIGITS) } coAnswers { firstCardStatus.await() }
            coEvery { provisioner.getCardStatus(OTHER_LAST_FOUR_DIGITS) } coAnswers { awaitCancellation() }
            val controller = createController()
            controller.show(card = addableCard())
            runCurrent()

            // Act
            controller.selectCard(card = addableCard(id = OTHER_CARD_ID, lastDigits = OTHER_LAST_FOUR_DIGITS))
            firstCardStatus.complete(CardStatus.NotAdded)
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Hidden)
        }
    }

    @Nested
    inner class StatusChecks {

        @Test
        fun `GIVEN card checked WHEN same card selected again THEN wallet status not re-checked`() = runTest {
            // Arrange
            val controller = createCheckedController()

            // Act
            controller.selectCard(card = addableCard())
            controller.updateFrozenState(TangemPayCardFrozenState.Frozen)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.getCardStatus(LAST_FOUR_DIGITS) }
        }

        @Test
        fun `GIVEN check in flight WHEN screen started THEN wallet status not checked again`() = runTest {
            // Arrange
            coEvery { provisioner.getCardStatus(any()) } coAnswers { awaitCancellation() }
            val controller = createController()
            controller.show(card = addableCard())
            runCurrent()

            // Act
            controller.onStart()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.getCardStatus(any()) }
        }

        @Test
        fun `GIVEN earlier check finished WHEN screen started THEN wallet status re-checked`() = runTest {
            // Arrange
            val controller = createCheckedController()
            clearMocks(provisioner, answers = false)

            // Act
            controller.onStart()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.getCardStatus(LAST_FOUR_DIGITS) }
        }

        @Test
        fun `GIVEN screen started WHEN wallet changes THEN wallet status re-checked`() = runTest {
            // Arrange
            val controller = createCheckedController()
            controller.onStart()
            runCurrent()
            clearMocks(provisioner, answers = false)

            // Act
            walletChanges.tryEmit(Unit)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.getCardStatus(LAST_FOUR_DIGITS) }
        }

        @Test
        fun `GIVEN screen stopped WHEN wallet changes THEN wallet status not re-checked`() = runTest {
            // Arrange
            val controller = createCheckedController()
            controller.onStart()
            runCurrent()
            controller.onStop()
            clearMocks(provisioner, answers = false)

            // Act
            walletChanges.tryEmit(Unit)
            runCurrent()

            // Assert
            coVerify(exactly = 0) { provisioner.getCardStatus(any()) }
        }
    }

    @Nested
    inner class AddCard {

        @Test
        fun `GIVEN card not in wallet WHEN add clicked THEN card pushed with its provisioning data`() = runTest {
            // Arrange
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) {
                provisioner.addCard(
                    GoogleWalletProvisioner.Card(
                        processorCardId = PROCESSOR_CARD_ID,
                        cardSecret = CARD_SECRET,
                        lastFourDigits = LAST_FOUR_DIGITS,
                        displayName = CARD_NAME,
                        environment = Environment.TEST,
                    ),
                )
            }
            verify(exactly = 1) { analytics.send(ofType<TangemPayAnalyticsEvents.AddToWalletClicked>()) }
        }

        @Test
        fun `GIVEN card without display name WHEN add clicked THEN default name sent to Google Wallet`() = runTest {
            // Arrange
            val controller = createStartedController(card = addableCard(displayName = null))

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.addCard(match { it.displayName == DEFAULT_CARD_NAME }) }
        }

        @Test
        fun `GIVEN verification pending WHEN add clicked THEN adding resumed without card secret`() = runTest {
            // Arrange
            coEvery { provisioner.getCardStatus(any()) } returns CardStatus.Incomplete
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.resumeAddingCard(LAST_FOUR_DIGITS, CARD_NAME) }
            coVerify(exactly = 0) { cardDetailsRepository.getProvisioningData(any(), any()) }
            coVerify(exactly = 0) { provisioner.addCard(any()) }
        }

        @Test
        fun `GIVEN verification pending WHEN resuming fails THEN error shown without pushing the card`() = runTest {
            // Arrange
            coEvery { provisioner.getCardStatus(any()) } returns CardStatus.Incomplete
            coEvery { provisioner.resumeAddingCard(any(), any()) } returns AddResult.Failed(
                reason = FailureReason.NO_TOKEN_AWAITING_VERIFICATION,
                description = "No token awaits identity verification",
            )
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(sentDialogs()).containsExactly(GENERIC_ERROR_DIALOG)
            coVerify(exactly = 0) { provisioner.addCard(any()) }
        }

        @Test
        fun `GIVEN addition running WHEN button shown THEN it is loading and further clicks ignored`() = runTest {
            // Arrange
            coEvery { provisioner.addCard(any()) } coAnswers { awaitCancellation() }
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Button(isLoading = true))
            coVerify(exactly = 1) { cardDetailsRepository.getProvisioningData(any(), any()) }
            coVerify(exactly = 1) { provisioner.addCard(any()) }
        }

        @Test
        fun `GIVEN addition finished without adding WHEN button shown THEN it stops loading`() = runTest {
            // Arrange
            coEvery { provisioner.addCard(any()) } returns AddResult.Cancelled
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(BUTTON)
        }

        @Test
        fun `GIVEN card being added elsewhere WHEN add clicked THEN secret not requested`() = runTest {
            // Arrange
            every { provisioner.isAdditionInProgress } returns true
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 0) { cardDetailsRepository.getProvisioningData(any(), any()) }
            coVerify(exactly = 0) { provisioner.addCard(any()) }
            assertThat(controller.entry.value).isEqualTo(BUTTON)
        }

        @Test
        fun `GIVEN card frozen after the check WHEN add clicked THEN card not pushed`() = runTest {
            // Arrange
            val controller = createStartedController()
            controller.updateFrozenState(TangemPayCardFrozenState.Frozen)

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 0) { cardDetailsRepository.getProvisioningData(any(), any()) }
            coVerify(exactly = 0) { provisioner.addCard(any()) }
        }

        @Test
        fun `GIVEN screen stopped while card data loads WHEN data arrives THEN card not pushed`() = runTest {
            // Arrange
            val provisioningData = CompletableDeferred<Either<UniversalError, TangemPayCardProvisioningData>>()
            coEvery { cardDetailsRepository.getProvisioningData(any(), any()) } coAnswers { provisioningData.await() }
            val controller = createStartedController()
            controller.onAddClick()
            runCurrent()

            // Act
            controller.onStop()
            provisioningData.complete(PROVISIONING_DATA.right())
            runCurrent()

            // Assert
            coVerify(exactly = 0) { provisioner.addCard(any()) }
            assertThat(controller.entry.value).isEqualTo(BUTTON)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Environments {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN SDK and Pay environments WHEN add clicked THEN card pushed only when they match`(
            model: EnvironmentModel,
        ) = runTest {
            // Arrange
            every { provisioner.environment } returns model.sdkEnvironment
            every { cardDetailsRepository.isProductionEnvironment() } returns model.isPayProduction
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = model.expectedPushes) { cardDetailsRepository.getProvisioningData(any(), any()) }
            coVerify(exactly = model.expectedPushes) { provisioner.addCard(any()) }
            assertThat(sentDialogs()).isEqualTo(model.expectedDialogs)
        }

        private fun provideTestModels() = listOf(
            EnvironmentModel(sdkEnvironment = Environment.TEST, isPayProduction = false, expectedPushes = 1),
            EnvironmentModel(sdkEnvironment = Environment.PROD, isPayProduction = true, expectedPushes = 1),
            EnvironmentModel(sdkEnvironment = Environment.TEST, isPayProduction = true, expectedPushes = 0),
            EnvironmentModel(sdkEnvironment = Environment.PROD, isPayProduction = false, expectedPushes = 0),
            EnvironmentModel(sdkEnvironment = null, isPayProduction = false, expectedPushes = 0),
            EnvironmentModel(sdkEnvironment = null, isPayProduction = true, expectedPushes = 0),
        )

        @Test
        fun `GIVEN production Pay and SDK WHEN add clicked THEN card carries the production environment`() = runTest {
            // Arrange
            every { provisioner.environment } returns Environment.PROD
            every { cardDetailsRepository.isProductionEnvironment() } returns true
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            coVerify(exactly = 1) { provisioner.addCard(match { it.environment == Environment.PROD }) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BackendFailures {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN provisioning data unavailable WHEN add clicked THEN matching error shown and card not pushed`(
            model: BackendFailureModel,
        ) = runTest {
            // Arrange
            coEvery { cardDetailsRepository.getProvisioningData(any(), any()) } returns model.error.left()
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(sentDialogs()).containsExactly(model.expectedDialog)
            coVerify(exactly = 0) { provisioner.addCard(any()) }
        }

        private fun provideTestModels() = listOf(
            BackendFailureModel(error = VisaApiError.UnknownWithoutCode, expectedDialog = NETWORK_ERROR_DIALOG),
            BackendFailureModel(error = VisaApiError.ServerUnavailable, expectedDialog = NETWORK_ERROR_DIALOG),
            BackendFailureModel(error = VisaApiError.Unspecified, expectedDialog = GENERIC_ERROR_DIALOG),
            BackendFailureModel(error = VisaApiError.RefreshTokenExpired, expectedDialog = GENERIC_ERROR_DIALOG),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddResults {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN add result WHEN addition finishes THEN matching message shown`(model: AddResultModel) = runTest {
            // Arrange
            coEvery { provisioner.addCard(any()) } returns model.result
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(sentDialogs()).isEqualTo(listOfNotNull(model.expectedDialog))
        }

        private fun provideTestModels() = listOf(
            AddResultModel(result = AddResult.Added, expectedDialog = null),
            AddResultModel(result = AddResult.AlreadyAdded, expectedDialog = null),
            AddResultModel(result = AddResult.Cancelled, expectedDialog = null),
            AddResultModel(result = failure(FailureReason.NETWORK_ERROR), expectedDialog = NETWORK_ERROR_DIALOG),
            AddResultModel(result = failure(FailureReason.GOOGLE_WALLET_ERROR), expectedDialog = GENERIC_ERROR_DIALOG),
            AddResultModel(result = failure(FailureReason.TOKENIZATION_DECLINED), expectedDialog = null),
            AddResultModel(result = failure(FailureReason.ADDITION_IN_PROGRESS), expectedDialog = null),
            AddResultModel(result = failure(FailureReason.ENVIRONMENT_MISMATCH), expectedDialog = GENERIC_ERROR_DIALOG),
            AddResultModel(result = failure(FailureReason.UNSUPPORTED), expectedDialog = GENERIC_ERROR_DIALOG),
        )

        @Test
        fun `GIVEN card added WHEN wallet has not confirmed it yet THEN button hidden`() = runTest {
            // Arrange
            var statusChecks = 0
            coEvery { provisioner.getCardStatus(any()) } coAnswers {
                if (statusChecks++ == 0) CardStatus.NotAdded else awaitCancellation()
            }
            val controller = createStartedController()

            // Act
            controller.onAddClick()
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Hidden)
        }

        @Test
        fun `GIVEN status check started before the result WHEN it returns stale status THEN it is ignored`() = runTest {
            // Arrange
            val staleStatus = CompletableDeferred<CardStatus>()
            val statuses = ArrayDeque(
                listOf<suspend () -> CardStatus>(
                    { CardStatus.NotAdded },
                    { staleStatus.await() },
                    { awaitCancellation() },
                ),
            )
            coEvery { provisioner.getCardStatus(any()) } coAnswers { statuses.removeFirst().invoke() }
            val controller = createCheckedController()
            controller.onStart()
            runCurrent()
            controller.onAddClick()
            runCurrent()

            // Act
            staleStatus.complete(CardStatus.NotAdded)
            runCurrent()

            // Assert
            assertThat(controller.entry.value).isEqualTo(Entry.Hidden)
            coVerify(exactly = 3) { provisioner.getCardStatus(any()) }
        }

        private fun failure(reason: FailureReason) = AddResult.Failed(reason = reason, description = "Adding failed")
    }

    private fun TestScope.createController() = TangemPayAddToGoogleWalletController(
        scope = backgroundScope,
        userWalletId = USER_WALLET_ID,
        context = context,
        provisioner = provisioner,
        cardDetailsRepository = cardDetailsRepository,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
        featureToggles = featureToggles,
    )

    private fun TestScope.createCheckedController(
        card: TangemPayCard = addableCard(),
    ): TangemPayAddToGoogleWalletController {
        val controller = createController()
        controller.show(card = card)
        runCurrent()
        return controller
    }

    private fun TestScope.createStartedController(
        card: TangemPayCard = addableCard(),
    ): TangemPayAddToGoogleWalletController {
        val controller = createCheckedController(card)
        controller.onStart()
        runCurrent()
        return controller
    }

    private fun TangemPayAddToGoogleWalletController.show(
        card: TangemPayCard?,
        frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
    ) {
        updateFrozenState(frozenState)
        selectCard(card)
    }

    private fun sentDialogs(): List<ErrorDialog> = sentMessages.map { message ->
        val dialog = message as DialogMessage
        ErrorDialog(title = dialog.title, message = dialog.message)
    }

    internal data class ErrorDialog(val title: TextReference?, val message: TextReference)

    internal data class EntryModel(
        val card: TangemPayCard? = addableCard(),
        val frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
        val walletStatus: CardStatus = CardStatus.NotAdded,
        val isButtonAlwaysShown: Boolean = false,
        val expected: Entry,
    )

    internal data class DisabledModel(
        val card: TangemPayCard?,
        val frozenState: TangemPayCardFrozenState,
        val expected: Entry,
    )

    internal data class EnvironmentModel(
        val sdkEnvironment: Environment?,
        val isPayProduction: Boolean,
        val expectedPushes: Int,
    ) {
        val expectedDialogs: List<ErrorDialog> = if (expectedPushes == 0) listOf(GENERIC_ERROR_DIALOG) else emptyList()
    }

    internal data class BackendFailureModel(val error: UniversalError, val expectedDialog: ErrorDialog)

    internal data class AddResultModel(val result: AddResult, val expectedDialog: ErrorDialog?)

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val CARD_ID = "card_1"
        const val OTHER_CARD_ID = "card_2"
        const val LAST_FOUR_DIGITS = "1234"
        const val OTHER_LAST_FOUR_DIGITS = "5678"
        const val CARD_NAME = "Travel card"
        const val DEFAULT_CARD_NAME = "Tangem Pay"
        const val PROCESSOR_CARD_ID = "processor-card-id"
        const val CARD_SECRET = "time-based-secret"

        val BUTTON = Entry.Button(isLoading = false)

        val PROVISIONING_DATA = TangemPayCardProvisioningData(
            processorCardId = PROCESSOR_CARD_ID,
            cardSecret = CARD_SECRET,
        )

        val ERROR_TITLE = resourceReference(CoreUiR.string.tangempay_card_details_add_to_wallet_error_title)
        val NETWORK_ERROR_DIALOG = ErrorDialog(
            title = ERROR_TITLE,
            message = resourceReference(CoreUiR.string.hw_cloud_backup_error_network),
        )
        val GENERIC_ERROR_DIALOG = ErrorDialog(
            title = ERROR_TITLE,
            message = resourceReference(CoreUiR.string.tangempay_card_details_add_to_wallet_error_description),
        )

        fun addableCard(
            id: String = CARD_ID,
            lastDigits: String = LAST_FOUR_DIGITS,
            displayName: CardDisplayName? = CardDisplayName(CARD_NAME).getOrNull(),
            state: TangemPayCardState = TangemPayCardState.Active,
            cardStatus: TangemPayCard.Status = TangemPayCard.Status.ACTIVE,
            isPlaceholder: Boolean = false,
        ): TangemPayCard = tangemPayCard(
            id = id,
            lastDigits = lastDigits,
            state = state,
            isPlaceholder = isPlaceholder,
            cardStatus = cardStatus,
            displayName = displayName,
        )
    }
}