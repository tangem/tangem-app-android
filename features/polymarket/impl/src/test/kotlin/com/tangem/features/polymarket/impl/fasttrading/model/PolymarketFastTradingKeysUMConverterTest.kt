package com.tangem.features.polymarket.impl.fasttrading.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
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
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                validUntil = null,
                expected = FastTradingKeyStatusUM.Expired(at = null),
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.ACTIVE,
                missingOnExchange = setOf(OTHER_ADDRESS),
                expected = FastTradingKeyStatusUM.ReconnectNeeded,
            ),
            StatusModel(
                status = PolymarketSessionKeyStatus.EXPIRED,
                validUntil = NOW.minus(Duration.ofDays(5)),
                expected = FastTradingKeyStatusUM.Expired(at = "Nov 4, 2026"),
            ),
            StatusModel(status = PolymarketSessionKeyStatus.PENDING, expected = FastTradingKeyStatusUM.Activating),
            StatusModel(status = PolymarketSessionKeyStatus.REVOKING, expected = FastTradingKeyStatusUM.Revoking),
            StatusModel(
                status = PolymarketSessionKeyStatus.FAILED,
                expected = FastTradingKeyStatusUM.ActivationFailed,
            ),
            StatusModel(status = PolymarketSessionKeyStatus.UNKNOWN, expected = FastTradingKeyStatusUM.Unknown),
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
            assertThat(actual.thisDevice).isEqualTo(
                FastTradingKeyRowUM(
                    id = "this_device_pending",
                    deviceName = stringReference(THIS_DEVICE_NAME),
                    status = FastTradingKeyStatusUM.Activating,
                    isExpiringSoon = false,
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
            assertThat(actual.thisDevice?.status).isEqualTo(FastTradingKeyStatusUM.Extending)
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
            assertThat(actual.thisDevice?.status).isEqualTo(FastTradingKeyStatusUM.Activating)
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
            assertThat(actual.thisDevice).isEqualTo(
                FastTradingKeyRowUM(
                    id = OWN_ADDRESS,
                    deviceName = resourceReference(R.string.prediction_session_key_unknown_device),
                    status = FastTradingKeyStatusUM.Active(until = "Feb 17, 2027"),
                    isExpiringSoon = false,
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
            assertThat(actual.thisDevice).isNull()
        }

        @Test
        fun `GIVEN foreign signers WHEN convert THEN they are listed as unknown signers`() {
            // Arrange
            val signer = PolymarketSessionSigner(address = FOREIGN_ADDRESS, scopes = listOf("CLOB"), validUntil = null)
            val overview = createOverview(sessionKeys = emptyList(), foreignSigners = listOf(signer))

            // Act
            val actual = converter.convert(overview, operation = null)

            // Assert
            assertThat(actual.unknownSigners).containsExactly(FastTradingUnknownSignerUM(FOREIGN_ADDRESS))
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

    data class StatusModel(
        val status: PolymarketSessionKeyStatus,
        val expected: FastTradingKeyStatusUM?,
        val isEnabled: Boolean = true,
        val validUntil: Instant? = DEFAULT_VALID_UNTIL,
        val missingOnExchange: Set<String> = emptySet(),
        val isExpiringSoon: Boolean = false,
    )

    data class OperationModel(
        val request: PolymarketSessionKeyOperationRequest,
        val stage: PolymarketSessionKeyOperationStage,
        val expected: FastTradingKeyStatusUM,
    )

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_794_182_400)
        val DEFAULT_VALID_UNTIL: Instant = NOW.plus(Duration.ofDays(100))
        const val THIS_DEVICE_NAME = "Pixel 9"
        const val OTHER_DEVICE_NAME = "Galaxy S24"
        const val OWN_ADDRESS = "0x5e55000000000000000000000000000000000001"
        const val OTHER_ADDRESS = "0x5e55000000000000000000000000000000000002"
        const val FOREIGN_ADDRESS = "0x5e55000000000000000000000000000000000003"

        fun createKey(
            address: String = OTHER_ADDRESS,
            deviceName: String? = OTHER_DEVICE_NAME,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant? = DEFAULT_VALID_UNTIL,
        ) = PolymarketSessionKey(
            address = address,
            deviceId = "device-$address",
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
            isReconciled: Boolean = true,
        ) = PolymarketSessionKeyOverview(
            wallet = PolymarketWalletState(
                depositWalletAddress = "0x2222222222222222222222222222222222222222",
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = sessionKeys,
            ),
            localKeyAddress = localKeyAddress,
            assessment = PolymarketSessionKeyAssessment(
                deviceState = PolymarketSessionKeyDeviceState.OfferEnable,
                reconciliation = if (isReconciled) {
                    PolymarketSessionKeyReconciliation.Checked(
                        foreignSigners = foreignSigners,
                        missingOnExchange = missingOnExchange,
                    )
                } else {
                    PolymarketSessionKeyReconciliation.Unavailable
                },
                nextChangeAt = null,
            ),
            isTradable = true,
        )

        fun createOperation(
            request: PolymarketSessionKeyOperationRequest,
            stage: PolymarketSessionKeyOperationStage,
            targetAddress: String?,
        ) = PolymarketSessionKeyOperation(id = 1, request = request, targetAddress = targetAddress, stage = stage)
    }
}