package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import kotlinx.serialization.Serializable

/**
 * Represents the lifecycle states of a joint (Safe multisig) account. The backend moves the account forward only:
 * [Pending] → [Confirming] → [Active]. State payloads (participants, threshold, Safe address, balances) arrive
 * together with the wallet-screen integration; until then the states carry only the data [source].
 *
 * @property source the freshness of the status information
 */
@Serializable
sealed class JointAccountStatusValue {

    abstract val source: StatusSource

    /**
     * Copies the status with a new [source].
     *
     * @param source the new source of the status information
     */
    fun copySealed(source: StatusSource): JointAccountStatusValue {
        return when (this) {
            is Pending -> copy(source = source)
            is Confirming -> copy(source = source)
            is Active -> copy(source = source)
            is Loading,
            is Error,
            -> this
        }
    }

    /** The state before the first data for the account has arrived. */
    @Serializable
    data object Loading : JointAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** Slots are being filled; the Safe address does not exist yet. */
    @Serializable
    data class Pending(override val source: StatusSource) : JointAccountStatusValue()

    /** Every slot is taken and the Safe address is computed; the creator has yet to activate the account. */
    @Serializable
    data class Confirming(override val source: StatusSource) : JointAccountStatusValue()

    /** Deposits and operations are available. */
    @Serializable
    data class Active(override val source: StatusSource) : JointAccountStatusValue()

    /** Represents error states of a joint account. */
    @Serializable
    sealed class Error : JointAccountStatusValue() {

        /**
         * The account state could not be fetched and no cached data exists. The source stays [StatusSource.ACTUAL]
         * on purpose, matching [PaymentAccountStatusValue.Error.Unavailable]: a single unavailable account must
         * never downgrade the wallet-level total balance freshness.
         */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}