package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import app.cash.turbine.test
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.common.truth.Truth.assertThat
import com.meawallet.mpp.GooglePayDataChangedListener
import com.meawallet.mpp.GooglePayHasEligibleTokenizationTargetListener
import com.meawallet.mpp.GooglePayRegisteredTokensListener
import com.meawallet.mpp.GooglePayTokenizeListener
import com.meawallet.mpp.MppErrorCode
import com.meawallet.mpp.MppPushCardToGooglePayListener
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Card
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.Environment
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultGoogleWalletProvisionerTest {

    private val sdk: MeaWalletGooglePay = mockk()
    private val activity: Activity = mockk()
    private lateinit var activityBridge: DefaultPushProvisioningActivityBridge

    @BeforeEach
    fun setUp() {
        clearMocks(sdk)
        every { sdk.isOsSupported } returns true
        every { sdk.isWalletAvailable() } returns true
        justRun { sdk.initialize() }
        every { sdk.hasEligibleTokenizationTarget(LAST_FOUR_DIGITS, any()) } answers {
            secondArg<GooglePayHasEligibleTokenizationTargetListener>().onSuccess(true)
        }
        activityBridge = DefaultPushProvisioningActivityBridge(sdk)
        activityBridge.attach(activity)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SdkEnvironment {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN linked SDK flavour WHEN environment read THEN matching environment returned`(
            model: EnvironmentModel,
        ) = runTest {
            // Arrange
            every { sdk.isProdSdk } returns model.isProdSdk
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.environment

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            EnvironmentModel(isProdSdk = true, expected = Environment.PROD),
            EnvironmentModel(isProdSdk = false, expected = Environment.TEST),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCardStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN wallet lookup outcome WHEN getCardStatus THEN status resolved`(
            model: SdkCase<GooglePayRegisteredTokensListener, CardStatus>,
        ) = runTest {
            // Arrange
            every { sdk.checkWalletForCardSuffix(LAST_FOUR_DIGITS, any()) } answers { model.answer(secondArg()) }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SdkCase<GooglePayRegisteredTokensListener, CardStatus>> = listOf(
            SdkCase(
                description = "active token",
                answer = { it.onSuccess(listOf(createTokenInfo(TapAndPay.TOKEN_STATE_ACTIVE))) },
                expected = CardStatus.Added,
            ),
            SdkCase(
                description = "token awaits identity verification",
                answer = { it.onSuccess(listOf(createTokenInfo(TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION))) },
                expected = CardStatus.Incomplete,
            ),
            SdkCase(
                description = "untokenized card",
                answer = { it.onSuccess(listOf(createTokenInfo(TapAndPay.TOKEN_STATE_UNTOKENIZED))) },
                expected = CardStatus.NotAdded,
            ),
            SdkCase(
                description = "no tokens",
                answer = { it.onSuccess(emptyList()) },
                expected = CardStatus.NotAdded,
            ),
            SdkCase(
                description = "no active wallet",
                answer = { it.onFailure(createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET)) },
                expected = CardStatus.NotAdded,
            ),
            SdkCase(
                description = "app not allowlisted",
                answer = { it.onFailure(createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE)) },
                expected = CardStatus.Unsupported,
            ),
            SdkCase(
                description = "lookup error",
                answer = { it.onFailure(createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR)) },
                expected = CardStatus.Unknown,
            ),
            SdkCase(
                description = "synchronous exception",
                answer = { throw createMppException(FakeMppError(errorCode = MppErrorCode.NOT_INITIALIZED)) },
                expected = CardStatus.Unknown,
            ),
            SdkCase(
                description = "lookup never completes",
                answer = {},
                expected = CardStatus.Unknown,
            ),
        )

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeviceSupport {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN device cannot provision WHEN getCardStatus THEN unsupported without SDK calls`(model: DeviceCase) =
            runTest {
                // Arrange
                model.arrange()
                val provisioner = createProvisioner()

                // Act
                val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

                // Assert
                assertThat(actual).isEqualTo(CardStatus.Unsupported)
                verify(exactly = 0) { sdk.initialize() }
                verify(exactly = 0) { sdk.checkWalletForCardSuffix(any(), any()) }
            }

        private fun provideTestModels() = listOf(
            DeviceCase(description = "OS below Android 9") { every { sdk.isOsSupported } returns false },
            DeviceCase(description = "Google Wallet not installed") { every { sdk.isWalletAvailable() } returns false },
            DeviceCase(description = "Google Wallet check throws") {
                every { sdk.isWalletAvailable() } throws IllegalStateException("Package manager died")
            },
        )

        @Test
        fun `GIVEN SDK initialized WHEN getCardStatus twice THEN SDK initialized once`() = runTest {
            // Arrange
            givenWalletTokens(TapAndPay.TOKEN_STATE_ACTIVE)
            val provisioner = createProvisioner()

            // Act
            provisioner.getCardStatus(LAST_FOUR_DIGITS)
            provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            verify(exactly = 1) { sdk.initialize() }
        }

        @Test
        fun `GIVEN SDK initialization fails WHEN getCardStatus twice THEN unsupported and initialized once`() =
            runTest {
                // Arrange
                every { sdk.initialize() } throws
                    createMppException(FakeMppError(errorCode = MppErrorCode.INTERNAL_ERROR))
                val provisioner = createProvisioner()

                // Act
                val actual = listOf(
                    provisioner.getCardStatus(LAST_FOUR_DIGITS),
                    provisioner.getCardStatus(LAST_FOUR_DIGITS),
                )

                // Assert
                assertThat(actual).containsExactly(CardStatus.Unsupported, CardStatus.Unsupported)
                verify(exactly = 1) { sdk.initialize() }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenizationTargetCheck {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN card not in wallet WHEN tokenization target check completes THEN status follows it`(
            model: SdkCase<GooglePayHasEligibleTokenizationTargetListener, CardStatus>,
        ) = runTest {
            // Arrange
            givenWalletTokens()
            every { sdk.hasEligibleTokenizationTarget(LAST_FOUR_DIGITS, any()) } answers { model.answer(secondArg()) }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SdkCase<GooglePayHasEligibleTokenizationTargetListener, CardStatus>> =
            listOf(
                SdkCase(
                    description = "eligible device",
                    answer = { it.onSuccess(true) },
                    expected = CardStatus.NotAdded,
                ),
                SdkCase(
                    description = "no eligible device",
                    answer = { it.onSuccess(false) },
                    expected = CardStatus.Unsupported,
                ),
                SdkCase(
                    description = "check reports the wallet API as unavailable",
                    answer = { it.onFailure(createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE)) },
                    expected = CardStatus.NotAdded,
                ),
                SdkCase(
                    description = "check fails",
                    answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_ERROR)) },
                    expected = CardStatus.NotAdded,
                ),
                SdkCase(
                    description = "check throws",
                    answer = { throw createMppException(FakeMppError(errorCode = MppErrorCode.NOT_INITIALIZED)) },
                    expected = CardStatus.NotAdded,
                ),
                SdkCase(
                    description = "check never completes",
                    answer = {},
                    expected = CardStatus.NotAdded,
                ),
            )

        @Test
        fun `GIVEN card in wallet WHEN getCardStatus THEN tokenization target not checked`() = runTest {
            // Arrange
            givenWalletTokens(TapAndPay.TOKEN_STATE_ACTIVE)
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(CardStatus.Added)
            verify(exactly = 0) { sdk.hasEligibleTokenizationTarget(any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RecentlyAddedOverride {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN push outcome WHEN wallet does not list the card yet THEN status follows the outcome`(
            model: SdkCase<MppPushCardToGooglePayListener, CardStatus>,
        ) = runTest {
            // Arrange
            every { sdk.push(CARD, activity, any()) } answers { model.answer(thirdArg()) }
            givenWalletTokens()
            val provisioner = createProvisioner()
            provisioner.addCard(CARD)

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SdkCase<MppPushCardToGooglePayListener, CardStatus>> = listOf(
            SdkCase(
                description = "added",
                answer = { it.onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null) },
                expected = CardStatus.Added,
            ),
            SdkCase(
                description = "already added",
                answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS)) },
                expected = CardStatus.Added,
            ),
            SdkCase(
                description = "cancelled",
                answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.OPERATION_CANCELLED_BY_USER)) },
                expected = CardStatus.NotAdded,
            ),
            SdkCase(
                description = "declined",
                answer = { it.onFailure(createDeclinedError()) },
                expected = CardStatus.NotAdded,
            ),
        )

        @Test
        fun `GIVEN card added by resume WHEN wallet does not list it yet THEN status is added`() = runTest {
            // Arrange
            givenWalletTokens(TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION)
            every { sdk.tokenize(any(), DISPLAY_NAME, activity, any()) } answers {
                arg<GooglePayTokenizeListener>(3).onSuccess()
            }
            val provisioner = createProvisioner()
            provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)
            givenWalletTokens()

            // Act
            val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(actual).isEqualTo(CardStatus.Added)
        }

        @Test
        fun `GIVEN added card WHEN wallet lookup fails THEN status unknown and override kept`() = runTest {
            // Arrange
            every { sdk.push(CARD, activity, any()) } answers {
                thirdArg<MppPushCardToGooglePayListener>().onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)
            }
            every { sdk.checkWalletForCardSuffix(LAST_FOUR_DIGITS, any()) } answers {
                secondArg<GooglePayRegisteredTokensListener>()
                    .onFailure(createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR))
            }
            val provisioner = createProvisioner()
            provisioner.addCard(CARD)

            // Act
            val duringLookupFailure = provisioner.getCardStatus(LAST_FOUR_DIGITS)
            givenWalletTokens()
            val afterLookupRecovers = provisioner.getCardStatus(LAST_FOUR_DIGITS)

            // Assert
            assertThat(listOf(duringLookupFailure, afterLookupRecovers))
                .containsExactly(CardStatus.Unknown, CardStatus.Added)
                .inOrder()
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class Clearing {

            @ParameterizedTest
            @ProvideTestModels
            fun `GIVEN added card seen by wallet WHEN wallet later misses it THEN status is not added`(
                confirmedTokenState: Int,
            ) = runTest {
                // Arrange
                every { sdk.push(CARD, activity, any()) } answers {
                    thirdArg<MppPushCardToGooglePayListener>().onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)
                }
                val provisioner = createProvisioner()
                provisioner.addCard(CARD)
                givenWalletTokens(confirmedTokenState)
                provisioner.getCardStatus(LAST_FOUR_DIGITS)
                givenWalletTokens()

                // Act
                val actual = provisioner.getCardStatus(LAST_FOUR_DIGITS)

                // Assert
                assertThat(actual).isEqualTo(CardStatus.NotAdded)
            }

            private fun provideTestModels() = listOf(
                TapAndPay.TOKEN_STATE_ACTIVE,
                TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION,
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN push outcome WHEN addCard THEN result mapped`(
            model: SdkCase<MppPushCardToGooglePayListener, AddResult>,
        ) = runTest {
            // Arrange
            every { sdk.push(CARD, activity, any()) } answers { model.answer(thirdArg()) }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.addCard(CARD)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SdkCase<MppPushCardToGooglePayListener, AddResult>> {
            val declinedError = createDeclinedError()

            return listOf(
                SdkCase(
                    description = "success",
                    answer = { it.onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null) },
                    expected = AddResult.Added,
                ),
                SdkCase(
                    description = "cancelled by user",
                    answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.OPERATION_CANCELLED_BY_USER)) },
                    expected = AddResult.Cancelled,
                ),
                SdkCase(
                    description = "token already exists",
                    answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS)) },
                    expected = AddResult.AlreadyAdded,
                ),
                SdkCase(
                    description = "declined",
                    answer = { it.onFailure(declinedError) },
                    expected = AddResult.Failed(
                        reason = FailureReason.GOOGLE_WALLET_ERROR,
                        description = declinedError.describe(),
                        sdkErrorCode = MppErrorCode.GOOGLE_PAY_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_TOKENIZATION_DECLINED,
                    ),
                ),
                SdkCase(
                    description = "synchronous SDK exception",
                    answer = { throw createMppException(FakeMppError(MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS)) },
                    expected = AddResult.AlreadyAdded,
                ),
                SdkCase(
                    description = "synchronous unexpected exception",
                    answer = { throw IllegalStateException("Unexpected") },
                    expected = AddResult.Failed(
                        reason = FailureReason.UNEXPECTED_ERROR,
                        description = "IllegalStateException: Unexpected",
                    ),
                ),
                SdkCase(
                    description = "success followed by an exception",
                    answer = { listener ->
                        listener.onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)
                        throw IllegalStateException("Late")
                    },
                    expected = AddResult.Added,
                ),
            )
        }

        @Test
        fun `GIVEN push WHEN it completes THEN bridge awaits the activity result only during the push`() = runTest {
            // Arrange
            var isAwaitingDuringPush = false
            every { sdk.push(CARD, activity, any()) } answers {
                isAwaitingDuringPush = activityBridge.isAwaitingResult
                thirdArg<MppPushCardToGooglePayListener>().onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)
            }
            val provisioner = createProvisioner()

            // Act
            provisioner.addCard(CARD)

            // Assert
            assertThat(isAwaitingDuringPush).isTrue()
            assertThat(activityBridge.isAwaitingResult).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AdditionInProgress {

        private var listenerSlot = slot<MppPushCardToGooglePayListener>()

        @BeforeEach
        fun givenPushAwaitingGoogleWallet() {
            listenerSlot = slot()
            justRun { sdk.push(CARD, activity, capture(listenerSlot)) }
            givenWalletTokens(TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN push awaits Google Wallet WHEN another addition starts THEN it is rejected`(
            model: SecondAdditionCase,
        ) = runTest {
            // Arrange
            val provisioner = createProvisioner()
            val firstAddition = async { provisioner.addCard(CARD) }
            runCurrent()

            // Act
            val secondAddition = model.add(provisioner)
            val isAwaitingAfterRejection = activityBridge.isAwaitingResult
            listenerSlot.captured.onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)

            // Assert
            assertThat(secondAddition).isEqualTo(
                AddResult.Failed(
                    reason = FailureReason.ADDITION_IN_PROGRESS,
                    description = "Another card is being added to Google Wallet",
                ),
            )
            assertThat(isAwaitingAfterRejection).isTrue()
            assertThat(firstAddition.await()).isEqualTo(AddResult.Added)
            verify(exactly = 1) { sdk.push(any(), any(), any()) }
            verify(exactly = 0) { sdk.tokenize(any(), any(), any(), any()) }
        }

        private fun provideTestModels() = listOf(
            SecondAdditionCase(description = "add card") { it.addCard(CARD) },
            SecondAdditionCase(description = "resume adding card") {
                it.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)
            },
        )

        @Test
        fun `GIVEN push awaits Google Wallet WHEN caller cancelled THEN activity results still reach the SDK`() =
            runTest {
                // Arrange
                justRun { sdk.handleOnActivityResult(any(), any(), any(), any()) }
                val provisioner = createProvisioner()
                val addition = launch { provisioner.addCard(CARD) }
                runCurrent()
                addition.cancel()
                runCurrent()

                // Act
                activityBridge.onActivityResult(
                    requestCode = PUSH_REQUEST_CODE,
                    resultCode = Activity.RESULT_OK,
                    data = null,
                )

                // Assert
                assertThat(activityBridge.isAwaitingResult).isTrue()
                verify(exactly = 1) {
                    sdk.handleOnActivityResult(PUSH_REQUEST_CODE, Activity.RESULT_OK, null, activity)
                }
            }

        @Test
        fun `GIVEN cancelled addition WHEN SDK reports its outcome THEN next addition starts`() = runTest {
            // Arrange
            val provisioner = createProvisioner()
            val cancelledAddition = launch { provisioner.addCard(CARD) }
            runCurrent()
            cancelledAddition.cancel()
            runCurrent()
            listenerSlot.captured.onFailure(FakeMppError(errorCode = MppErrorCode.OPERATION_CANCELLED_BY_USER))
            every { sdk.push(CARD, activity, any()) } answers {
                thirdArg<MppPushCardToGooglePayListener>().onSuccess(TOKEN_ID, LAST_FOUR_DIGITS, null)
            }

            // Act
            val actual = provisioner.addCard(CARD)

            // Assert
            assertThat(actual).isEqualTo(AddResult.Added)
            verify(exactly = 2) { sdk.push(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddingPreconditions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN adding is impossible WHEN card added or resumed THEN failed without SDK calls`(
            model: PreconditionCase,
        ) = runTest {
            // Arrange
            model.arrange()
            val provisioner = createProvisioner()

            // Act
            val actual = model.add(provisioner)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            verify(exactly = 0) { sdk.checkWalletForCardSuffix(any(), any()) }
            verify(exactly = 0) { sdk.push(any(), any(), any()) }
            verify(exactly = 0) { sdk.tokenize(any(), any(), any(), any()) }
        }

        private fun provideTestModels(): List<PreconditionCase> {
            val detachActivity: () -> Unit = { activityBridge.detach(activity) }
            val uninstallWallet: () -> Unit = { every { sdk.isWalletAvailable() } returns false }
            val addCard: suspend (DefaultGoogleWalletProvisioner) -> AddResult = { it.addCard(CARD) }
            val resume: suspend (DefaultGoogleWalletProvisioner) -> AddResult = {
                it.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)
            }
            val noActivity = AddResult.Failed(
                reason = FailureReason.NO_ACTIVITY,
                description = "No activity to open Google Wallet from",
            )
            val unsupported = AddResult.Failed(
                reason = FailureReason.UNSUPPORTED,
                description = "Push provisioning is not supported on this device",
            )

            return listOf(
                PreconditionCase("no activity, add", detachActivity, addCard, noActivity),
                PreconditionCase("no activity, resume", detachActivity, resume, noActivity),
                PreconditionCase("no Google Wallet, add", uninstallWallet, addCard, unsupported),
                PreconditionCase("no Google Wallet, resume", uninstallWallet, resume, unsupported),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResumeAddingCard {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN token awaits verification WHEN resumeAddingCard THEN tokenize outcome returned`(
            model: SdkCase<GooglePayTokenizeListener, AddResult>,
        ) = runTest {
            // Arrange
            givenWalletTokens(TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION)
            every { sdk.tokenize(any(), DISPLAY_NAME, activity, any()) } answers { model.answer(arg(3)) }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<SdkCase<GooglePayTokenizeListener, AddResult>> {
            val declinedError = createDeclinedError()

            return listOf(
                SdkCase(
                    description = "success",
                    answer = { it.onSuccess() },
                    expected = AddResult.Added,
                ),
                SdkCase(
                    description = "cancelled by user",
                    answer = { it.onFailure(FakeMppError(errorCode = MppErrorCode.OPERATION_CANCELLED_BY_USER)) },
                    expected = AddResult.Cancelled,
                ),
                SdkCase(
                    description = "declined",
                    answer = { it.onFailure(declinedError) },
                    expected = AddResult.Failed(
                        reason = FailureReason.GOOGLE_WALLET_ERROR,
                        description = declinedError.describe(),
                        sdkErrorCode = MppErrorCode.GOOGLE_PAY_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_TOKENIZATION_DECLINED,
                    ),
                ),
                SdkCase(
                    description = "synchronous unexpected exception",
                    answer = { throw IllegalStateException("Unexpected") },
                    expected = AddResult.Failed(
                        reason = FailureReason.UNEXPECTED_ERROR,
                        description = "IllegalStateException: Unexpected",
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN active and verification tokens WHEN resumeAddingCard THEN pending token tokenized`() = runTest {
            // Arrange
            val activeToken = createTokenInfo(TapAndPay.TOKEN_STATE_ACTIVE)
            val pendingToken = createTokenInfo(TapAndPay.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION)
            every { sdk.checkWalletForCardSuffix(LAST_FOUR_DIGITS, any()) } answers {
                secondArg<GooglePayRegisteredTokensListener>().onSuccess(listOf(activeToken, pendingToken))
            }
            every { sdk.tokenize(pendingToken, DISPLAY_NAME, activity, any()) } answers {
                arg<GooglePayTokenizeListener>(3).onSuccess()
            }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)

            // Assert
            assertThat(actual).isEqualTo(AddResult.Added)
            verify(exactly = 1) { sdk.tokenize(pendingToken, DISPLAY_NAME, activity, any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResumeWithoutPendingToken {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no token awaits verification WHEN resumeAddingCard THEN result returned without tokenization`(
            model: SdkCase<GooglePayRegisteredTokensListener, AddResult>,
        ) = runTest {
            // Arrange
            every { sdk.checkWalletForCardSuffix(LAST_FOUR_DIGITS, any()) } answers { model.answer(secondArg()) }
            val provisioner = createProvisioner()

            // Act
            val actual = provisioner.resumeAddingCard(lastFourDigits = LAST_FOUR_DIGITS, displayName = DISPLAY_NAME)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            verify(exactly = 0) { sdk.tokenize(any(), any(), any(), any()) }
        }

        private fun provideTestModels(): List<SdkCase<GooglePayRegisteredTokensListener, AddResult>> {
            val lookupError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR)
            val notAllowlistedError = createLookupError(TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE)
            val noPendingToken = AddResult.Failed(
                reason = FailureReason.NO_TOKEN_AWAITING_VERIFICATION,
                description = "No token awaits identity verification",
            )

            return listOf(
                SdkCase(
                    description = "active token",
                    answer = { it.onSuccess(listOf(createTokenInfo(TapAndPay.TOKEN_STATE_ACTIVE))) },
                    expected = AddResult.AlreadyAdded,
                ),
                SdkCase(
                    description = "untokenized card",
                    answer = { it.onSuccess(listOf(createTokenInfo(TapAndPay.TOKEN_STATE_UNTOKENIZED))) },
                    expected = noPendingToken,
                ),
                SdkCase(
                    description = "no tokens",
                    answer = { it.onSuccess(emptyList()) },
                    expected = noPendingToken,
                ),
                SdkCase(
                    description = "lookup error",
                    answer = { it.onFailure(lookupError) },
                    expected = AddResult.Failed(
                        reason = FailureReason.TOKEN_LOOKUP_FAILED,
                        description = lookupError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_ATTESTATION_ERROR,
                    ),
                ),
                SdkCase(
                    description = "app not allowlisted",
                    answer = { it.onFailure(notAllowlistedError) },
                    expected = AddResult.Failed(
                        reason = FailureReason.UNSUPPORTED,
                        description = notAllowlistedError.describe(),
                        sdkErrorCode = MppErrorCode.INTERNAL_ERROR,
                        walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE,
                    ),
                ),
                SdkCase(
                    description = "lookup never completes",
                    answer = {},
                    expected = AddResult.Failed(
                        reason = FailureReason.TOKEN_LOOKUP_FAILED,
                        description = "Token lookup timed out",
                    ),
                ),
            )
        }
    }

    @Nested
    inner class WalletChanges {

        @Test
        fun `GIVEN collector WHEN wallet data changes THEN change emitted and listener removed on cancel`() = runTest {
            // Arrange
            val listenerSlot = slot<GooglePayDataChangedListener>()
            justRun { sdk.registerDataChangedListener(capture(listenerSlot)) }
            justRun { sdk.removeDataChangedListener(any()) }
            val provisioner = createProvisioner()

            // Act
            provisioner.walletChanges.test {
                runCurrent()
                listenerSlot.captured.onDataChanged()

                // Assert
                assertThat(awaitItem()).isEqualTo(Unit)
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            verify(exactly = 1) { sdk.removeDataChangedListener(listenerSlot.captured) }
        }

        @Test
        fun `GIVEN no Google Wallet WHEN wallet changes collected THEN listener not registered`() = runTest {
            // Arrange
            every { sdk.isWalletAvailable() } returns false
            val provisioner = createProvisioner()

            // Act
            provisioner.walletChanges.test {
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }

            // Assert
            verify(exactly = 0) { sdk.registerDataChangedListener(any()) }
        }

        @Test
        fun `GIVEN listener registration fails WHEN collection cancelled THEN listener not removed`() = runTest {
            // Arrange
            every { sdk.registerDataChangedListener(any()) } throws IllegalStateException("App is in background")
            justRun { sdk.removeDataChangedListener(any()) }
            val provisioner = createProvisioner()

            // Act
            provisioner.walletChanges.test {
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()

            // Assert
            verify(exactly = 0) { sdk.removeDataChangedListener(any()) }
        }
    }

    internal class SdkCase<L, E>(private val description: String, val answer: (L) -> Unit, val expected: E) {
        override fun toString(): String = description
    }

    internal class DeviceCase(private val description: String, val arrange: () -> Unit) {
        override fun toString(): String = description
    }

    internal class SecondAdditionCase(
        private val description: String,
        val add: suspend (DefaultGoogleWalletProvisioner) -> AddResult,
    ) {
        override fun toString(): String = description
    }

    internal class PreconditionCase(
        private val description: String,
        val arrange: () -> Unit,
        val add: suspend (DefaultGoogleWalletProvisioner) -> AddResult,
        val expected: AddResult,
    ) {
        override fun toString(): String = description
    }

    internal data class EnvironmentModel(val isProdSdk: Boolean, val expected: Environment)

    private fun TestScope.createProvisioner() = DefaultGoogleWalletProvisioner(
        sdk = sdk,
        activityBridge = activityBridge,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )

    private fun givenWalletTokens(vararg tokenStates: Int) {
        every { sdk.checkWalletForCardSuffix(LAST_FOUR_DIGITS, any()) } answers {
            secondArg<GooglePayRegisteredTokensListener>().onSuccess(tokenStates.map(::createTokenInfo))
        }
    }

    private companion object {
        const val LAST_FOUR_DIGITS = "1234"
        const val DISPLAY_NAME = "Tangem Pay"
        const val TOKEN_ID = "token-id"
        const val PUSH_REQUEST_CODE = 1002

        val CARD = Card(
            processorCardId = "processor-card-id",
            cardSecret = "card-secret",
            lastFourDigits = LAST_FOUR_DIGITS,
            displayName = DISPLAY_NAME,
        )
    }
}