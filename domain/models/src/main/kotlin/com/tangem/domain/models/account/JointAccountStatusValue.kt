package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
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

    /**
     * Slots are being filled; the Safe address does not exist yet.
     *
     * @property joinedMembers members that have taken a slot, the creator included
     * @property totalMembers  slots of the account, fixed at creation
     */
    @Serializable
    data class Pending(
        override val source: StatusSource,
        val joinedMembers: Int,
        val totalMembers: Int,
    ) : JointAccountStatusValue()

    /**
     * Every slot is taken and the Safe address is computed; the members have yet to confirm it.
     *
     * @property totalMembers    slots of the account, all of them taken in this state
     * @property verifiedMembers members who confirmed the shared address
     * @property isOwnVerified   this wallet confirmed the shared address
     */
    @Serializable
    data class Confirming(
        override val source: StatusSource,
        val totalMembers: Int,
        val verifiedMembers: Int = 0,
        val isOwnVerified: Boolean = false,
    ) : JointAccountStatusValue()

    /**
     * Deposits and operations are available.
     *
     * @property totalMembers members of the account
     * @property safeAddress  the account's Safe address, one and the same in every supported EVM network — the only
     * address the account owns; the participant's owner key holds no funds
     */
    @Serializable
    data class Active(
        override val source: StatusSource,
        val totalMembers: Int,
        val safeAddress: String,
    ) : JointAccountStatusValue()

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

        /**
         * The composition the backend states does not verify: this wallet's owner address is not among the members,
         * or the Safe address does not follow from them. Nothing can be done with the account, and its tokens stay
         * out of every flow; only the row is shown, so its figures travel here instead of in a token list.
         *
         * @property totalMembers members of the account
         * @property tokensCount  tokens the account holds
         * @property fiatBalance  what those tokens are worth, kept out of the wallet total
         */
        @Serializable
        data class Compromised(
            val totalMembers: Int,
            val tokensCount: Int,
            val fiatBalance: TotalFiatBalance,
        ) : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}