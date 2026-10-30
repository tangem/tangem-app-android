package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Active
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Disabled
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Enabling
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.OfferEnable
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Reenable
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.RenewalRequired
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.RepairByRenewal
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Replace
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Revoking
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.WaitingForPrevious
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation.Checked
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation.Unavailable
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Duration
import java.time.Instant

internal class PolymarketSessionKeyAssessorTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeviceState {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN session keys and signers WHEN assess THEN resolves what this device does next`(model: Model) {
            // Act
            val actual = PolymarketSessionKeyAssessor.assess(model.snapshot).deviceState

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            Model(
                case = "no session keys offers enabling",
                snapshot = snapshot(keys = emptyList()),
                expected = OfferEnable,
            ),
            Model(
                case = "no local key and no keys of this device offers enabling",
                snapshot = snapshot(keys = emptyList(), localAddress = null),
                expected = OfferEnable,
            ),
            Model(
                case = "pending local key is enabling",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.PENDING))),
                expected = Enabling(key(Owner.LOCAL, PolymarketSessionKeyStatus.PENDING)),
            ),
            Model(
                case = "revoking local key is revoking",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.REVOKING))),
                expected = Revoking(key(Owner.LOCAL, PolymarketSessionKeyStatus.REVOKING)),
            ),
            Model(
                case = "active enabled local key trades",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL)), signers = listOf(matching(key(Owner.LOCAL)))),
                expected = Active(key(Owner.LOCAL), isRenewDue = false),
            ),
            Model(
                case = "active local key switched off stays paused",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, isEnabled = false)),
                    signers = listOf(matching(key(Owner.LOCAL, isEnabled = false))),
                ),
                expected = Disabled(key(Owner.LOCAL, isEnabled = false), isRenewDue = false),
            ),
            Model(
                case = "revoked local key is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.REVOKED))),
                expected = Reenable(key(Owner.LOCAL, PolymarketSessionKeyStatus.REVOKED)),
            ),
            Model(
                case = "expired local key is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.EXPIRED))),
                expected = Reenable(key(Owner.LOCAL, PolymarketSessionKeyStatus.EXPIRED)),
            ),
            Model(
                case = "active local key past its validity is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, validUntil = NOW.minusSeconds(1)))),
                expected = Reenable(key(Owner.LOCAL, validUntil = NOW.minusSeconds(1))),
            ),
            Model(
                case = "active local key the exchange no longer lists is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL)), signers = emptyList()),
                expected = Reenable(key(Owner.LOCAL)),
            ),
            Model(
                case = "failed local key the exchange lists anyway is repaired by renewal",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
                    signers = listOf(matching(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED))),
                ),
                expected = RepairByRenewal(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
            ),
            Model(
                case = "failed local key the exchange does not list is enabled again",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
                    signers = emptyList(),
                ),
                expected = Reenable(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
            ),
            Model(
                case = "failed local key without the exchange's list is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)), signers = null),
                expected = Reenable(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
            ),
            Model(
                case = "pending previous key of this device is waited for",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS, PolymarketSessionKeyStatus.PENDING))),
                expected = WaitingForPrevious(key(Owner.PREVIOUS, PolymarketSessionKeyStatus.PENDING)),
            ),
            Model(
                case = "revoking previous key of this device is waited for",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS, PolymarketSessionKeyStatus.REVOKING))),
                expected = WaitingForPrevious(key(Owner.PREVIOUS, PolymarketSessionKeyStatus.REVOKING)),
            ),
            Model(
                case = "active previous key the exchange lists is revoked and replaced",
                snapshot = snapshot(
                    keys = listOf(key(Owner.PREVIOUS)),
                    signers = listOf(matching(key(Owner.PREVIOUS))),
                ),
                expected = Replace.ByRevocation(key(Owner.PREVIOUS)),
            ),
            Model(
                case = "active previous key the exchange no longer lists is replaced by overwrite",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS)), signers = emptyList()),
                expected = Replace.ByOverwrite(key(Owner.PREVIOUS)),
            ),
            Model(
                case = "active previous key without the exchange's list is revoked and replaced",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS)), signers = null),
                expected = Replace.ByRevocation(key(Owner.PREVIOUS)),
            ),
            Model(
                case = "previous key that turned active moments ago is revoked, not overwritten",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS, authorizedAt = FRESH)), signers = emptyList()),
                expected = Replace.ByRevocation(key(Owner.PREVIOUS, authorizedAt = FRESH)),
            ),
            Model(
                case = "active previous key without a local key is replaced",
                snapshot = snapshot(
                    keys = listOf(key(Owner.PREVIOUS)),
                    signers = listOf(matching(key(Owner.PREVIOUS))),
                    localAddress = null,
                ),
                expected = Replace.ByRevocation(key(Owner.PREVIOUS)),
            ),
            Model(
                case = "expired previous key of this device leaves the device free to enable",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS, validUntil = NOW.minusSeconds(1)))),
                expected = OfferEnable,
            ),
            Model(
                case = "dead previous key of this device leaves the device free to enable",
                snapshot = snapshot(keys = listOf(key(Owner.PREVIOUS, PolymarketSessionKeyStatus.REVOKED))),
                expected = OfferEnable,
            ),
            Model(
                case = "key of another device does not change what this device does",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = OfferEnable,
            ),
            Model(
                case = "live local key decides before a previous key of this device",
                snapshot = snapshot(
                    keys = listOf(key(Owner.PREVIOUS), key(Owner.LOCAL)),
                    signers = listOf(matching(key(Owner.LOCAL)), matching(key(Owner.PREVIOUS))),
                ),
                expected = Active(key(Owner.LOCAL), isRenewDue = false),
            ),
            Model(
                case = "active previous key decides before a dead local key",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.REVOKED), key(Owner.PREVIOUS)),
                    signers = listOf(matching(key(Owner.PREVIOUS))),
                ),
                expected = Replace.ByRevocation(key(Owner.PREVIOUS)),
            ),
            Model(
                case = "active local key past its renewal date trades with renewal due",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, renewAfter = NOW.minusSeconds(1))),
                    signers = listOf(matching(key(Owner.LOCAL, renewAfter = NOW.minusSeconds(1)))),
                ),
                expected = Active(key(Owner.LOCAL, renewAfter = NOW.minusSeconds(1)), isRenewDue = true),
            ),
            Model(
                case = "active local key before its renewal date trades without renewal due",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, renewAfter = NOW.plusSeconds(60))),
                    signers = listOf(matching(key(Owner.LOCAL, renewAfter = NOW.plusSeconds(60)))),
                ),
                expected = Active(key(Owner.LOCAL, renewAfter = NOW.plusSeconds(60)), isRenewDue = false),
            ),
            Model(
                case = "paused local key past its renewal date reports renewal due",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, isEnabled = false, renewAfter = NOW)),
                    signers = listOf(matching(key(Owner.LOCAL, isEnabled = false, renewAfter = NOW))),
                ),
                expected = Disabled(key(Owner.LOCAL, isEnabled = false, renewAfter = NOW), isRenewDue = true),
            ),
            Model(
                case = "active local key within ten minutes of its validity no longer signs",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, validUntil = ALMOST_EXPIRED)),
                    signers = listOf(matching(key(Owner.LOCAL, validUntil = ALMOST_EXPIRED))),
                ),
                expected = RenewalRequired(key(Owner.LOCAL, validUntil = ALMOST_EXPIRED)),
            ),
            Model(
                case = "local key with an unknown status is enabled again",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.UNKNOWN))),
                expected = Reenable(key(Owner.LOCAL, PolymarketSessionKeyStatus.UNKNOWN)),
            ),
            Model(
                case = "active local key without the exchange's list trades",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL)), signers = null),
                expected = Active(key(Owner.LOCAL), isRenewDue = false),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Reconciliation {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN session keys and signers WHEN assess THEN reconciles them with the exchange`(
            model: ReconciliationModel,
        ) {
            // Act
            val actual = PolymarketSessionKeyAssessor.assess(model.snapshot).reconciliation

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ReconciliationModel(
                case = "signer the backend never recorded is foreign",
                snapshot = snapshot(keys = emptyList(), signers = listOf(UNRECORDED)),
                expected = checked(foreign = listOf(UNRECORDED)),
            ),
            ReconciliationModel(
                case = "signer of a revoked record is foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.REVOKED)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.REVOKED))),
                ),
                expected = checked(foreign = listOf(matching(key(Owner.OTHER_DEVICE)))),
            ),
            ReconciliationModel(
                case = "signer of an expired record is foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.EXPIRED)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(foreign = listOf(matching(key(Owner.OTHER_DEVICE)))),
            ),
            ReconciliationModel(
                case = "signer of a record with an unknown status is foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.UNKNOWN)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(foreign = listOf(matching(key(Owner.OTHER_DEVICE)))),
            ),
            ReconciliationModel(
                case = "failed record of another device the exchange lists is foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.FAILED)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(foreign = listOf(matching(key(Owner.OTHER_DEVICE)))),
            ),
            ReconciliationModel(
                case = "failed local key the exchange lists is not foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL, PolymarketSessionKeyStatus.FAILED)),
                    signers = listOf(matching(key(Owner.LOCAL))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "active record the exchange lists with other scopes is foreign",
                snapshot = snapshot(keys = listOf(key(Owner.OTHER_DEVICE)), signers = listOf(OTHER_DEVICE_ALL_SCOPES)),
                expected = checked(foreign = listOf(OTHER_DEVICE_ALL_SCOPES)),
            ),
            ReconciliationModel(
                case = "active record the exchange lists with another validity is foreign",
                snapshot = snapshot(keys = listOf(key(Owner.OTHER_DEVICE)), signers = listOf(OTHER_DEVICE_LONGER)),
                expected = checked(foreign = listOf(OTHER_DEVICE_LONGER)),
            ),
            ReconciliationModel(
                case = "expired active record the exchange lists with another validity is still foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, validUntil = NOW.minusSeconds(1))),
                    signers = listOf(OTHER_DEVICE_LONGER),
                ),
                expected = checked(foreign = listOf(OTHER_DEVICE_LONGER)),
            ),
            ReconciliationModel(
                case = "expired active record the exchange lists with its own terms is not foreign",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, validUntil = NOW.minusSeconds(1))),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE, validUntil = NOW.minusSeconds(1)))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "scopes are compared ignoring case",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE)).copy(scopes = listOf("clob"))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "fresh active record with other terms is not judged yet",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, authorizedAt = FRESH)),
                    signers = listOf(OTHER_DEVICE_ALL_SCOPES),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "signer of a pending record is not judged yet",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.PENDING)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "signer of a revoking record is not judged yet",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, PolymarketSessionKeyStatus.REVOKING)),
                    signers = listOf(matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "key that turned active moments ago is not reported missing",
                snapshot = snapshot(keys = listOf(key(Owner.OTHER_DEVICE, authorizedAt = FRESH)), signers = emptyList()),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "active key without a confirmation time is not reported missing",
                snapshot = snapshot(keys = listOf(key(Owner.OTHER_DEVICE, authorizedAt = null)), signers = emptyList()),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "active key the exchange does not list is reported missing",
                snapshot = snapshot(keys = listOf(key(Owner.OTHER_DEVICE)), signers = emptyList()),
                expected = checked(missing = setOf(Owner.OTHER_DEVICE.address)),
            ),
            ReconciliationModel(
                case = "expired active key the exchange does not list is not reported missing",
                snapshot = snapshot(
                    keys = listOf(key(Owner.OTHER_DEVICE, validUntil = NOW.minusSeconds(1))),
                    signers = emptyList(),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "matching lists report nothing",
                snapshot = snapshot(
                    keys = listOf(key(Owner.LOCAL), key(Owner.OTHER_DEVICE)),
                    signers = listOf(matching(key(Owner.LOCAL)), matching(key(Owner.OTHER_DEVICE))),
                ),
                expected = checked(),
            ),
            ReconciliationModel(
                case = "without the exchange's list nothing is judged",
                snapshot = snapshot(keys = listOf(key(Owner.LOCAL)), signers = null),
                expected = Unavailable,
            ),
        )
    }

    data class Model(
        val case: String,
        val snapshot: PolymarketSessionKeySnapshot,
        val expected: PolymarketSessionKeyDeviceState,
    ) {
        override fun toString(): String = case
    }

    data class ReconciliationModel(
        val case: String,
        val snapshot: PolymarketSessionKeySnapshot,
        val expected: PolymarketSessionKeyReconciliation,
    ) {
        override fun toString(): String = case
    }

    enum class Owner(val address: String, val deviceId: String) {
        LOCAL(address = "0x5e55000000000000000000000000000000000001", deviceId = DEVICE_ID),
        PREVIOUS(address = "0x5e55000000000000000000000000000000000002", deviceId = DEVICE_ID),
        OTHER_DEVICE(address = "0x5e55000000000000000000000000000000000003", deviceId = "other-device"),
    }

    private companion object {
        const val DEVICE_ID = "this-device"
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_400)
        val VALID_UNTIL: Instant = NOW + Duration.ofDays(180)
        val ALMOST_EXPIRED: Instant = NOW + Duration.ofMinutes(9)
        val FRESH: Instant = NOW - Duration.ofMinutes(1)

        val UNRECORDED = PolymarketSessionSigner(
            address = "0xf0e1000000000000000000000000000000000009",
            scopes = listOf("CLOB"),
            validUntil = VALID_UNTIL,
        )
        val OTHER_DEVICE_ALL_SCOPES = PolymarketSessionSigner(
            address = Owner.OTHER_DEVICE.address,
            scopes = listOf("ALL"),
            validUntil = VALID_UNTIL,
        )
        val OTHER_DEVICE_LONGER = PolymarketSessionSigner(
            address = Owner.OTHER_DEVICE.address,
            scopes = listOf("CLOB"),
            validUntil = VALID_UNTIL + Duration.ofDays(1),
        )

        fun key(
            owner: Owner,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant = VALID_UNTIL,
            renewAfter: Instant? = null,
            authorizedAt: Instant? = NOW - Duration.ofDays(1),
        ) = PolymarketSessionKey(
            address = owner.address,
            deviceId = owner.deviceId,
            deviceName = "Pixel",
            isEnabled = isEnabled,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = validUntil,
            renewAfter = renewAfter,
            authorizedAt = authorizedAt,
            createdAt = NOW - Duration.ofDays(1),
        )

        fun matching(key: PolymarketSessionKey) = PolymarketSessionSigner(
            address = key.address,
            scopes = key.scopes,
            validUntil = key.validUntil,
        )

        fun snapshot(
            keys: List<PolymarketSessionKey>,
            signers: List<PolymarketSessionSigner>? = emptyList(),
            localAddress: String? = Owner.LOCAL.address,
        ) = PolymarketSessionKeySnapshot(
            sessionKeys = keys,
            localKeyAddress = localAddress,
            deviceId = DEVICE_ID,
            signers = signers,
            now = NOW,
        )

        fun checked(
            foreign: List<PolymarketSessionSigner> = emptyList(),
            missing: Set<String> = emptySet(),
        ) = Checked(foreignSigners = foreign, missingOnExchange = missing)
    }
}