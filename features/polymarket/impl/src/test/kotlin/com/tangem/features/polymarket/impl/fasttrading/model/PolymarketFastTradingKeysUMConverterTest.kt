package com.tangem.features.polymarket.impl.fasttrading.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingActivateBannerUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

internal class PolymarketFastTradingKeysUMConverterTest {

    private val converter = PolymarketFastTradingKeysUMConverter(
        thisDeviceName = THIS_DEVICE_NAME,
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
        locale = Locale.US,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KeyStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN another device key WHEN convert THEN row shows the expected status`(model: StatusModel) {
            // Arrange
            val key = createKey(status = model.status, isEnabled = model.isEnabled, validUntil = model.validUntil)
            val overview = createOverview(sessionKeys = listOf(key), missingOnExchange = model.missingOnExchange)

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            val expected = model.expected?.let { status ->
                FastTradingKeyRowUM(
                    id = OTHER_ADDRESS,
                    deviceName = stringReference(OTHER_DEVICE_NAME),
                    status = status,
                    isExpiringSoon = model.isExpiringSoon,
                    actions = if (model.offersRevoke) revokeOf(OTHER_ADDRESS) else persistentListOf(),
                )
            }
            assertThat(actual.otherDevices).containsExactlyElementsIn(listOfNotNull(expected))
        }

        private fun provideTestModels() = listOf(
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                expected = FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"),
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                validUntil = NOW.plus(Duration.ofDays(20)),
                expected = FastTradingKeyStatusUM.Active(until = "Nov 29, 2026"),
                isExpiringSoon = true,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                validUntil = NOW.plus(Duration.ofDays(31)),
                expected = FastTradingKeyStatusUM.Active(until = "Dec 10, 2026"),
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                isEnabled = false,
                expected = FastTradingKeyStatusUM.Paused(activeUntil = "Feb 17, 2027"),
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                isEnabled = false,
                validUntil = NOW.plus(Duration.ofDays(20)),
                expected = FastTradingKeyStatusUM.Paused(activeUntil = "Nov 29, 2026"),
                isExpiringSoon = true,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                validUntil = NOW.minus(Duration.ofDays(5)),
                expected = FastTradingKeyStatusUM.Expired(at = "Nov 4, 2026"),
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                validUntil = null,
                expected = FastTradingKeyStatusUM.Expired(at = null),
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                missingOnExchange = setOf(OTHER_ADDRESS),
                expected = FastTradingKeyStatusUM.ReconnectNeeded,
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.EXPIRED,
                validUntil = NOW.minus(Duration.ofDays(5)),
                expected = FastTradingKeyStatusUM.Expired(at = "Nov 4, 2026"),
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.PENDING,
                expected = FastTradingKeyStatusUM.Activating,
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.REVOKING,
                expected = FastTradingKeyStatusUM.Revoking,
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.FAILED,
                expected = FastTradingKeyStatusUM.ActivationFailed,
                offersRevoke = false,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.UNKNOWN,
                expected = FastTradingKeyStatusUM.Unknown,
                offersRevoke = false,
            ),
            StatusModel(status = PolymarketSessionKeyStatus.REVOKED, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RunningOperation {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an operation on another device key WHEN convert THEN row shows the operation`(
            model: OperationModel,
        ) {
            // Arrange
            val overview = createOverview(sessionKeys = listOf(createKey()))
            val operation = createOperation(request = model.request, stage = model.stage, targetAddress = OTHER_ADDRESS)

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.otherDevices.single().status).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN this device activating before its key is listed WHEN convert THEN pending row is named after this device`() {
            // Arrange
            val overview = createOverview(sessionKeys = emptyList(), localKeyAddress = null)
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Activate,
                stage = PolymarketSessionKeyOperationStage.AUTHORIZING,
                targetAddress = null,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.thisDevice).containsExactly(
                FastTradingKeyRowUM(
                    id = "this_device_pending",
                    deviceName = stringReference(THIS_DEVICE_NAME),
                    status = FastTradingKeyStatusUM.Activating,
                    isExpiringSoon = false,
                    actions = persistentListOf(),
                ),
            )
        }

        @Test
        fun `GIVEN extend after the old key is revoked WHEN convert THEN this device shows extending`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS, status = PolymarketSessionKeyStatus.REVOKED)
            val overview = createOverview(sessionKeys = listOf(ownKey), localKeyAddress = OWN_ADDRESS)
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Extend,
                stage = PolymarketSessionKeyOperationStage.REVOKED,
                targetAddress = OWN_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.thisDevice.single().status).isEqualTo(FastTradingKeyStatusUM.Extending)
            assertThat(actual.otherDevices).isEmpty()
        }

        @Test
        fun `GIVEN renewal of this device key while revoking WHEN convert THEN this device shows activating`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS)
            val overview = createOverview(sessionKeys = listOf(ownKey), localKeyAddress = OWN_ADDRESS)
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Activate,
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                targetAddress = OWN_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.thisDevice.single().status).isEqualTo(FastTradingKeyStatusUM.Activating)
        }

        private fun provideTestModels() = listOf(
            OperationModel(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.SIGNING,
                expected = FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"),
            ),
            OperationModel(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                expected = FastTradingKeyStatusUM.Revoking,
            ),
            OperationModel(
                request = PolymarketSessionKeyOperationRequest.SetEnabled(OTHER_ADDRESS, isEnabled = false),
                stage = PolymarketSessionKeyOperationStage.TOGGLING,
                expected = FastTradingKeyStatusUM.Pausing,
            ),
            OperationModel(
                request = PolymarketSessionKeyOperationRequest.SetEnabled(OTHER_ADDRESS, isEnabled = true),
                stage = PolymarketSessionKeyOperationStage.TOGGLING,
                expected = FastTradingKeyStatusUM.Resuming,
            ),
            OperationModel(
                request = PolymarketSessionKeyOperationRequest.Activate,
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                expected = FastTradingKeyStatusUM.Revoking,
            ),
        )
    }

    @Nested
    inner class Sections {

        @Test
        fun `GIVEN this device key and others WHEN convert THEN this device is split from other devices`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS, deviceName = null)
            val otherKey = createKey()
            val overview = createOverview(sessionKeys = listOf(otherKey, ownKey), localKeyAddress = OWN_ADDRESS)

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.thisDevice).containsExactly(
                FastTradingKeyRowUM(
                    id = OWN_ADDRESS,
                    deviceName = resourceReference(R.string.prediction_session_key_unknown_device),
                    status = FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"),
                    isExpiringSoon = false,
                    actions = revokeOf(
                        address = OWN_ADDRESS,
                        name = resourceReference(R.string.prediction_session_key_unknown_device),
                    ),
                ),
            )
            assertThat(actual.otherDevices.map { it.id }).containsExactly(OTHER_ADDRESS)
        }

        @Test
        fun `GIVEN no key on this device WHEN convert THEN this device section is empty`() {
            // Arrange
            val overview = createOverview(sessionKeys = listOf(createKey()), localKeyAddress = null)

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.thisDevice).isEmpty()
        }

        @Test
        fun `GIVEN foreign signers WHEN convert THEN they are listed as unknown signers`() {
            // Arrange
            val signer = PolymarketSessionSigner(address = FOREIGN_ADDRESS, scopes = listOf("CLOB"), validUntil = null)
            val overview = createOverview(sessionKeys = emptyList(), foreignSigners = listOf(signer))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.unknownSigners).containsExactly(
                FastTradingUnknownSignerUM(address = FOREIGN_ADDRESS, actions = signerRevokeOf(FOREIGN_ADDRESS)),
            )
        }

        @Test
        fun `GIVEN reconciliation unavailable WHEN convert THEN no unknown signers nor reconnect status`() {
            // Arrange
            val overview = createOverview(sessionKeys = listOf(createKey()), isReconciled = false)

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.unknownSigners).isEmpty()
            assertThat(actual.otherDevices.single().status)
                .isEqualTo(FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"))
        }
    }

    @Nested
    inner class Actions {

        @Test
        fun `GIVEN no running operation WHEN convert THEN actions are enabled`() {
            // Arrange
            val overview = createOverview(sessionKeys = listOf(createKey()))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.areActionsEnabled).isTrue()
        }

        @Test
        fun `GIVEN an operation awaiting signature WHEN convert THEN actions are disabled but still offered`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS)
            val signer = PolymarketSessionSigner(address = FOREIGN_ADDRESS, scopes = listOf("CLOB"), validUntil = null)
            val overview = createOverview(sessionKeys = listOf(ownKey, createKey()), foreignSigners = listOf(signer))
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.SIGNING,
                targetAddress = OTHER_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.areActionsEnabled).isFalse()
            assertThat(actual.thisDevice.single().actions).isEqualTo(revokeOf(OWN_ADDRESS))
            assertThat(actual.otherDevices.single().actions).isEqualTo(revokeOf(OTHER_ADDRESS))
            assertThat(actual.unknownSigners.single().actions).isEqualTo(signerRevokeOf(FOREIGN_ADDRESS))
        }

        @Test
        fun `GIVEN a submitted revocation WHEN convert THEN only the revoked row loses its actions`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS)
            val overview = createOverview(sessionKeys = listOf(ownKey, createKey()))
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                targetAddress = OTHER_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.areActionsEnabled).isFalse()
            assertThat(actual.otherDevices.single().status).isEqualTo(FastTradingKeyStatusUM.Revoking)
            assertThat(actual.otherDevices.single().actions).isEmpty()
            assertThat(actual.thisDevice.single().actions).isEqualTo(revokeOf(OWN_ADDRESS))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ThisDevice {

        @ParameterizedTest
        @MethodSource("provideStateModels")
        fun `GIVEN this device state WHEN convert THEN banner and rows follow it`(model: ThisDeviceModel) {
            // Arrange
            val overview = createOverview(
                sessionKeys = listOf(OWN_KEY, PREVIOUS_KEY, createKey()),
                listedSigners = if (model.isOwnListed) setOf(OWN_ADDRESS) else emptySet(),
                deviceState = model.deviceState,
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.activateBanner).isEqualTo(model.expectedBanner)
            assertThat(actual.thisDevice.map { it.id }).containsExactlyElementsIn(model.expectedThisDevice).inOrder()
            assertThat(actual.otherDevices.map { it.id }).containsExactly(OTHER_ADDRESS)
        }

        @ParameterizedTest
        @MethodSource("provideOperationModels")
        fun `GIVEN activation offered and an operation WHEN convert THEN banner and row follow the operation`(
            model: BannerOperationModel,
        ) {
            // Arrange
            val overview = createOverview(sessionKeys = emptyList(), localKeyAddress = null)
            val operation = createOperation(request = model.request, stage = model.stage, targetAddress = null)

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.activateBanner).isEqualTo(model.expectedBanner)
            assertThat(actual.thisDevice.map { it.status }).containsExactlyElementsIn(listOfNotNull(model.expectedRow))
            assertThat(actual.areActionsEnabled).isFalse()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN this device keys and an operation WHEN convert THEN sections follow them`(model: ScenarioModel) {
            // Arrange
            val overview = createOverview(
                sessionKeys = model.sessionKeys,
                localKeyAddress = model.localKeyAddress,
                listedSigners = model.listedSigners,
                isReconciled = model.isReconciled,
                deviceState = model.deviceState,
            )

            // Act
            val actual = converter.convert(overview, model.operation)

            // Assert
            assertThat(actual.activateBanner).isEqualTo(model.expectedBanner)
            assertThat(actual.thisDevice.map { it.id to it.status })
                .containsExactlyElementsIn(model.expectedThisDevice)
                .inOrder()
            assertThat(actual.otherDevices.map { it.id }).containsExactlyElementsIn(model.expectedOtherDevices)
        }

        @Test
        fun `GIVEN this device failed key the exchange lists WHEN convert THEN it keeps revoke under the banner`() {
            // Arrange
            val overview = createOverview(
                sessionKeys = listOf(OWN_FAILED_KEY),
                listedSigners = setOf(OWN_ADDRESS),
                deviceState = PolymarketSessionKeyDeviceState.RepairByRenewal(OWN_FAILED_KEY),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.activateBanner).isEqualTo(IDLE_BANNER)
            assertThat(actual.thisDevice.single().actions).isEqualTo(revokeOf(OWN_ADDRESS, THIS_DEVICE))
        }

        @Test
        fun `GIVEN this device expired key honoured as a foreign signer WHEN convert THEN it stays under the banner`() {
            // Arrange
            val ownKey = createKey(address = OWN_ADDRESS, status = PolymarketSessionKeyStatus.EXPIRED)
            val overview = createOverview(
                sessionKeys = listOf(ownKey),
                foreignSigners = listOf(signerOf(OWN_ADDRESS)),
                listedSigners = setOf(OWN_ADDRESS),
                deviceState = PolymarketSessionKeyDeviceState.Reenable(ownKey),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.isUnknownSignersWarningShown).isTrue()
            assertThat(actual.unknownSigners).isEmpty()
            assertThat(actual.thisDevice.single().status).isEqualTo(FastTradingKeyStatusUM.UnknownSigner)
            assertThat(actual.thisDevice.single().actions).isEqualTo(signerRevokeOf(OWN_ADDRESS))
        }

        @Test
        fun `GIVEN replacement offered WHEN convert THEN previous key keeps its menu under this device`() {
            // Arrange
            val overview = createOverview(
                sessionKeys = listOf(PREVIOUS_KEY),
                localKeyAddress = null,
                deviceState = PolymarketSessionKeyDeviceState.Replace.ByRevocation(PREVIOUS_KEY),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.activateBanner).isEqualTo(IDLE_BANNER)
            assertThat(actual.thisDevice.single().actions).isEqualTo(revokeOf(PREVIOUS_ADDRESS, THIS_DEVICE))
        }

        private fun provideTestModels() = listOf(
            ScenarioModel(
                sessionKeys = listOf(createKey(address = OWN_ADDRESS, status = PolymarketSessionKeyStatus.REVOKED)),
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_KEY),
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = emptyList(),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_FAILED_KEY),
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_FAILED_KEY),
                isReconciled = false,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(OWN_ADDRESS to FastTradingKeyStatusUM.ActivationFailed),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_FAILED_KEY),
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_FAILED_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.AUTHORIZING, target = OWN_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(PENDING_ROW_ID to FastTradingKeyStatusUM.Activating),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_FAILED_KEY),
                listedSigners = setOf(OWN_ADDRESS),
                deviceState = PolymarketSessionKeyDeviceState.RepairByRenewal(OWN_FAILED_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.AUTHORIZING, target = OWN_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS to FastTradingKeyStatusUM.Activating),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_PENDING_KEY, createKey()),
                deviceState = PolymarketSessionKeyDeviceState.Enabling(OWN_PENDING_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.CHECKING_READINESS, target = OWN_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS to FastTradingKeyStatusUM.Activating),
                expectedOtherDevices = listOf(OTHER_ADDRESS),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_PENDING_KEY),
                listedSigners = setOf(OWN_ADDRESS),
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_PENDING_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.AUTHORIZING, target = PREVIOUS_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(
                    PENDING_ROW_ID to FastTradingKeyStatusUM.Activating,
                    OWN_ADDRESS to FastTradingKeyStatusUM.Activating,
                ),
            ),
            ScenarioModel(
                sessionKeys = listOf(PREVIOUS_KEY),
                localKeyAddress = null,
                deviceState = PolymarketSessionKeyDeviceState.Replace.ByRevocation(PREVIOUS_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.REVOKING, target = PREVIOUS_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(
                    PENDING_ROW_ID to FastTradingKeyStatusUM.Activating,
                    PREVIOUS_ADDRESS to FastTradingKeyStatusUM.Revoking,
                ),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_PENDING_KEY, PREVIOUS_KEY, createKey()),
                deviceState = PolymarketSessionKeyDeviceState.Enabling(OWN_PENDING_KEY),
                operation = activation(PolymarketSessionKeyOperationStage.AUTHORIZING, target = OWN_ADDRESS),
                expectedBanner = null,
                expectedThisDevice = listOf(
                    OWN_ADDRESS to FastTradingKeyStatusUM.Activating,
                    PREVIOUS_ADDRESS to FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"),
                ),
                expectedOtherDevices = listOf(OTHER_ADDRESS),
            ),
            ScenarioModel(
                sessionKeys = listOf(OWN_KEY),
                deviceState = PolymarketSessionKeyDeviceState.Active(OWN_KEY, isRenewDue = true),
                operation = createOperation(
                    request = PolymarketSessionKeyOperationRequest.Extend,
                    stage = PolymarketSessionKeyOperationStage.REVOKING,
                    targetAddress = OWN_ADDRESS.uppercase(),
                ),
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS to FastTradingKeyStatusUM.Extending),
            ),
        )

        private fun provideStateModels() = listOf(
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_KEY),
                isOwnListed = false,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Reenable(OWN_KEY),
                isOwnListed = true,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.RepairByRenewal(OWN_KEY),
                isOwnListed = false,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.RepairByRenewal(OWN_KEY),
                isOwnListed = true,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Replace.ByRevocation(PREVIOUS_KEY),
                isOwnListed = false,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Replace.ByOverwrite(PREVIOUS_KEY),
                isOwnListed = true,
                expectedBanner = IDLE_BANNER,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Enabling(OWN_KEY),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Revoking(OWN_KEY),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Active(OWN_KEY, isRenewDue = false),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.Disabled(OWN_KEY, isRenewDue = false),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.RenewalRequired(OWN_KEY),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
            ThisDeviceModel(
                deviceState = PolymarketSessionKeyDeviceState.WaitingForPrevious(PREVIOUS_KEY),
                isOwnListed = false,
                expectedBanner = null,
                expectedThisDevice = listOf(OWN_ADDRESS, PREVIOUS_ADDRESS),
            ),
        )

        private fun provideOperationModels() = listOf(
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Activate,
                stage = PolymarketSessionKeyOperationStage.SIGNING,
                expectedBanner = LOADING_BANNER,
                expectedRow = null,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Reactivate,
                stage = PolymarketSessionKeyOperationStage.SIGNING,
                expectedBanner = LOADING_BANNER,
                expectedRow = null,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Activate,
                stage = PolymarketSessionKeyOperationStage.AUTHORIZING,
                expectedBanner = null,
                expectedRow = FastTradingKeyStatusUM.Activating,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Reactivate,
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                expectedBanner = null,
                expectedRow = FastTradingKeyStatusUM.Activating,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.SIGNING,
                expectedBanner = DISABLED_BANNER,
                expectedRow = null,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                expectedBanner = DISABLED_BANNER,
                expectedRow = null,
            ),
            BannerOperationModel(
                request = PolymarketSessionKeyOperationRequest.Extend,
                stage = PolymarketSessionKeyOperationStage.REVOKED,
                expectedBanner = DISABLED_BANNER,
                expectedRow = FastTradingKeyStatusUM.Extending,
            ),
        )
    }

    @Nested
    inner class ExchangeListing {

        @Test
        fun `GIVEN another device failed key the exchange lists WHEN convert THEN it offers revoke`() {
            // Arrange
            val key = createKey(status = PolymarketSessionKeyStatus.FAILED)
            val overview = createOverview(sessionKeys = listOf(key), listedSigners = setOf(OTHER_ADDRESS))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.otherDevices.single().actions).isEqualTo(revokeOf(OTHER_ADDRESS))
        }

        @Test
        fun `GIVEN another device failed key the exchange does not list WHEN convert THEN it offers nothing`() {
            // Arrange
            val key = createKey(status = PolymarketSessionKeyStatus.FAILED)
            val overview = createOverview(sessionKeys = listOf(key), listedSigners = setOf(FOREIGN_ADDRESS))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.otherDevices.single().actions).isEmpty()
        }

        @Test
        fun `GIVEN expired and unknown keys the exchange lists WHEN convert THEN they offer revoke`() {
            // Arrange
            val expired = createKey(address = OTHER_ADDRESS.uppercase(), status = PolymarketSessionKeyStatus.EXPIRED)
            val unknown = createKey(address = FOREIGN_ADDRESS, status = PolymarketSessionKeyStatus.UNKNOWN)
            val overview = createOverview(
                sessionKeys = listOf(expired, unknown),
                listedSigners = setOf(OTHER_ADDRESS, FOREIGN_ADDRESS),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.otherDevices.map { it.actions }).containsExactly(
                revokeOf(OTHER_ADDRESS.uppercase()),
                revokeOf(FOREIGN_ADDRESS),
            ).inOrder()
        }

        @Test
        fun `GIVEN reconciliation unavailable WHEN convert THEN only active and paused keys offer revoke`() {
            // Arrange
            val active = createKey(address = OWN_ADDRESS)
            val paused = createKey(isEnabled = false)
            val expired = createKey(address = FOREIGN_ADDRESS, status = PolymarketSessionKeyStatus.EXPIRED)
            val overview = createOverview(sessionKeys = listOf(active, paused, expired), isReconciled = false)

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.thisDevice.single().actions).isEqualTo(revokeOf(OWN_ADDRESS))
            assertThat(actual.otherDevices.map { it.actions })
                .containsExactly(revokeOf(OTHER_ADDRESS), persistentListOf<FastTradingKeyActionUM>())
                .inOrder()
        }
    }

    @Nested
    inner class UnknownSigners {

        @Test
        fun `GIVEN only foreign signers shown as key rows WHEN convert THEN warning is shown without signer rows`() {
            // Arrange
            val key = createKey(status = PolymarketSessionKeyStatus.EXPIRED)
            val overview = createOverview(
                sessionKeys = listOf(key),
                foreignSigners = listOf(signerOf(OTHER_ADDRESS.uppercase())),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.isUnknownSignersWarningShown).isTrue()
            assertThat(actual.unknownSigners).isEmpty()
            assertThat(actual.otherDevices.map { it.id }).containsExactly(OTHER_ADDRESS)
        }

        @Test
        fun `GIVEN a key the exchange honours on other terms WHEN convert THEN its row reads unknown signer`() {
            // Arrange
            val key = createKey(validUntil = NOW.plus(Duration.ofDays(20)))
            val overview = createOverview(
                sessionKeys = listOf(key),
                foreignSigners = listOf(signerOf(OTHER_ADDRESS.uppercase())),
                listedSigners = setOf(OTHER_ADDRESS),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.otherDevices).containsExactly(
                FastTradingKeyRowUM(
                    id = OTHER_ADDRESS,
                    deviceName = stringReference(OTHER_DEVICE_NAME),
                    status = FastTradingKeyStatusUM.UnknownSigner,
                    isExpiringSoon = false,
                    actions = signerRevokeOf(OTHER_ADDRESS),
                ),
            )
        }

        @Test
        fun `GIVEN a foreign key being revoked WHEN convert THEN its row shows revoking`() {
            // Arrange
            val overview = createOverview(
                sessionKeys = listOf(createKey()),
                foreignSigners = listOf(signerOf(OTHER_ADDRESS)),
                listedSigners = setOf(OTHER_ADDRESS),
            )
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Revoke(OTHER_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                targetAddress = OTHER_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.otherDevices.single().status).isEqualTo(FastTradingKeyStatusUM.Revoking)
        }

        @Test
        fun `GIVEN a signer without key row being revoked WHEN convert THEN its row shows revoking without actions`() {
            // Arrange
            val overview = createOverview(sessionKeys = emptyList(), foreignSigners = listOf(signerOf(FOREIGN_ADDRESS)))
            val operation = createOperation(
                request = PolymarketSessionKeyOperationRequest.Revoke(FOREIGN_ADDRESS),
                stage = PolymarketSessionKeyOperationStage.REVOKING,
                targetAddress = FOREIGN_ADDRESS,
            )

            // Act
            val actual = converter.convert(overview, operation)

            // Assert
            assertThat(actual.unknownSigners).containsExactly(
                FastTradingUnknownSignerUM(address = FOREIGN_ADDRESS, actions = persistentListOf(), isRevoking = true),
            )
        }

        @Test
        fun `GIVEN no foreign signers WHEN convert THEN warning is hidden`() {
            // Arrange
            val overview = createOverview(sessionKeys = listOf(createKey()))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.isUnknownSignersWarningShown).isFalse()
        }

        @Test
        fun `GIVEN foreign signers with and without key rows WHEN convert THEN only those without get a row`() {
            // Arrange
            val key = createKey(status = PolymarketSessionKeyStatus.EXPIRED)
            val overview = createOverview(
                sessionKeys = listOf(key),
                foreignSigners = listOf(signerOf(OTHER_ADDRESS), signerOf(FOREIGN_ADDRESS)),
            )

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.unknownSigners).containsExactly(
                FastTradingUnknownSignerUM(address = FOREIGN_ADDRESS, actions = signerRevokeOf(FOREIGN_ADDRESS)),
            )
        }

        @Test
        fun `GIVEN a foreign signer whose record is revoked WHEN convert THEN it gets a signer row`() {
            // Arrange
            val key = createKey(status = PolymarketSessionKeyStatus.REVOKED)
            val overview = createOverview(sessionKeys = listOf(key), foreignSigners = listOf(signerOf(OTHER_ADDRESS)))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.otherDevices).isEmpty()
            assertThat(actual.unknownSigners.map { it.address }).containsExactly(OTHER_ADDRESS)
        }
    }

    data class StatusModel(
        val status: PolymarketSessionKeyStatus,
        val expected: FastTradingKeyStatusUM?,
        val isEnabled: Boolean = true,
        val validUntil: Instant? = DEFAULT_VALID_UNTIL,
        val missingOnExchange: Set<String> = emptySet(),
        val isExpiringSoon: Boolean = false,
        val offersRevoke: Boolean = true,
    )

    data class ThisDeviceModel(
        val deviceState: PolymarketSessionKeyDeviceState,
        val isOwnListed: Boolean,
        val expectedBanner: FastTradingActivateBannerUM?,
        val expectedThisDevice: List<String>,
    )

    data class ScenarioModel(
        val sessionKeys: List<PolymarketSessionKey>,
        val deviceState: PolymarketSessionKeyDeviceState,
        val expectedBanner: FastTradingActivateBannerUM?,
        val expectedThisDevice: List<Pair<String, FastTradingKeyStatusUM>>,
        val expectedOtherDevices: List<String> = emptyList(),
        val localKeyAddress: String? = OWN_ADDRESS,
        val listedSigners: Set<String> = emptySet(),
        val isReconciled: Boolean = true,
        val operation: PolymarketSessionKeyOperation? = null,
    )

    data class BannerOperationModel(
        val request: PolymarketSessionKeyOperationRequest,
        val stage: PolymarketSessionKeyOperationStage,
        val expectedBanner: FastTradingActivateBannerUM?,
        val expectedRow: FastTradingKeyStatusUM?,
    )

    data class OperationModel(
        val request: PolymarketSessionKeyOperationRequest,
        val stage: PolymarketSessionKeyOperationStage,
        val expected: FastTradingKeyStatusUM,
    )

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_794_182_400)
        val DEFAULT_VALID_UNTIL: Instant = NOW.plus(Duration.ofDays(100))
        val OWN_KEY = createKey(address = OWN_ADDRESS, deviceName = THIS_DEVICE_NAME)
        val PREVIOUS_KEY = createKey(
            address = PREVIOUS_ADDRESS,
            deviceName = THIS_DEVICE_NAME,
            deviceId = THIS_DEVICE_ID,
        )
        val OWN_FAILED_KEY = createKey(
            address = OWN_ADDRESS,
            deviceName = THIS_DEVICE_NAME,
            status = PolymarketSessionKeyStatus.FAILED,
        )
        val OWN_PENDING_KEY = createKey(
            address = OWN_ADDRESS,
            deviceName = THIS_DEVICE_NAME,
            status = PolymarketSessionKeyStatus.PENDING,
        )
        const val THIS_DEVICE_NAME = "Pixel 9"
        const val OTHER_DEVICE_NAME = "Galaxy S24"
        const val OWN_ADDRESS = "0x5e55000000000000000000000000000000000001"
        const val OTHER_ADDRESS = "0x5e55000000000000000000000000000000000002"
        const val FOREIGN_ADDRESS = "0x5e55000000000000000000000000000000000003"
        const val PREVIOUS_ADDRESS = "0x5e55000000000000000000000000000000000004"
        val THIS_DEVICE = stringReference(THIS_DEVICE_NAME)
        val IDLE_BANNER = FastTradingActivateBannerUM(isLoading = false, isEnabled = true)
        val DISABLED_BANNER = FastTradingActivateBannerUM(isLoading = false, isEnabled = false)
        val LOADING_BANNER = FastTradingActivateBannerUM(isLoading = true, isEnabled = false)
        const val PENDING_ROW_ID = "this_device_pending"
        const val THIS_DEVICE_ID = "this-device"

        fun signerOf(address: String) =
            PolymarketSessionSigner(address = address, scopes = listOf("CLOB"), validUntil = null)

        fun revokeOf(address: String, name: TextReference = stringReference(OTHER_DEVICE_NAME)) =
            persistentListOf<FastTradingKeyActionUM>(
                FastTradingKeyActionUM.Revoke(address, FastTradingKeyActionUM.RevokeSubject.Device(name)),
            )

        fun signerRevokeOf(address: String) = persistentListOf<FastTradingKeyActionUM>(
            FastTradingKeyActionUM.Revoke(address, FastTradingKeyActionUM.RevokeSubject.UnknownSigner),
        )

        fun createKey(
            address: String = OTHER_ADDRESS,
            deviceName: String? = OTHER_DEVICE_NAME,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant? = DEFAULT_VALID_UNTIL,
            deviceId: String = "device-$address",
        ) = PolymarketSessionKey(
            address = address,
            deviceId = deviceId,
            deviceName = deviceName,
            isEnabled = isEnabled,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = validUntil,
            renewAfter = null,
            authorizedAt = null,
            createdAt = NOW,
        )

        fun createOverview(
            sessionKeys: List<PolymarketSessionKey>,
            localKeyAddress: String? = OWN_ADDRESS,
            missingOnExchange: Set<String> = emptySet(),
            foreignSigners: List<PolymarketSessionSigner> = emptyList(),
            listedSigners: Set<String> = emptySet(),
            isReconciled: Boolean = true,
            deviceState: PolymarketSessionKeyDeviceState = sessionKeys
                .firstOrNull { it.address == localKeyAddress }
                ?.let { PolymarketSessionKeyDeviceState.Active(sessionKey = it, isRenewDue = false) }
                ?: PolymarketSessionKeyDeviceState.OfferEnable,
        ) = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = sessionKeys,
            ),
            localKeyAddress = localKeyAddress,
            deviceId = THIS_DEVICE_ID,
            assessment = PolymarketSessionKeyAssessment(
                deviceState = deviceState,
                reconciliation = if (isReconciled) {
                    PolymarketSessionKeyReconciliation.Checked(
                        foreignSigners = foreignSigners,
                        missingOnExchange = missingOnExchange,
                        listedSigners = listedSigners,
                    )
                } else {
                    PolymarketSessionKeyReconciliation.Unavailable
                },
                nextChangeAt = null,
            ),
            isTradable = true,
        )

        fun activation(stage: PolymarketSessionKeyOperationStage, target: String) = createOperation(
            request = PolymarketSessionKeyOperationRequest.Activate,
            stage = stage,
            targetAddress = target,
        )

        fun createOperation(
            request: PolymarketSessionKeyOperationRequest,
            stage: PolymarketSessionKeyOperationStage,
            targetAddress: String?,
        ) = PolymarketSessionKeyOperation(id = 1, request = request, targetAddress = targetAddress, stage = stage)
    }
}