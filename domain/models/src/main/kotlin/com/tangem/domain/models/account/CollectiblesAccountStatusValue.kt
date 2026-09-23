package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * The states a collectibles account can have: the gacha vault does not exist on chain yet, exists and answers to
 * this device, exists and answers to another one, or could not be read.
 *
 * Like [PredictionAccountStatusValue], no state reports [TotalFiatBalance.Loading]: the wallet total is the sum of
 * every account's contribution and a single loading one short-circuits it.
 *
 * @property source the source of the status information
 */
@Serializable
sealed class CollectiblesAccountStatusValue {

    abstract val source: StatusSource

    val balance: CollectiblesVaultBalance?
        get() = when (this) {
            is Onboarded -> vaultBalance
            is UpdateRequired -> vaultBalance
            is Loading,
            is NotOnboarded,
            is MissedDerivation,
            is Error,
            -> null
        }

    val fiatBalance: SerializedBigDecimal?
        get() = balance?.fiatAmount

    val totalFiatBalance: TotalFiatBalance
        get() = when (this) {
            is Loading,
            is NotOnboarded,
            is MissedDerivation,
            is Error,
            -> TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
            is Onboarded,
            is UpdateRequired,
            -> {
                // An unread vault contributes a zero, not a failure: a failed contribution blanks the whole wallet
                // total rather than this account
                if (balance == null) {
                    TotalFiatBalance.Loaded(amount = SerializedBigDecimal.ZERO, source = source)
                } else {
                    val amount = fiatBalance ?: return TotalFiatBalance.Failed
                    TotalFiatBalance.Loaded(amount = amount, source = source)
                }
            }
        }

    /**
     * Copies the status with a new [source] and [vaultBalance]. States that carry neither are returned unchanged.
     *
     * @param source       the new source of the status information
     * @param vaultBalance the new vault balance
     */
    fun copySealed(
        source: StatusSource = this.source,
        vaultBalance: CollectiblesVaultBalance? = balance,
    ): CollectiblesAccountStatusValue {
        return when (this) {
            is Onboarded -> copy(source = source, vaultBalance = vaultBalance)
            is UpdateRequired -> copy(source = source, vaultBalance = vaultBalance)
            is Loading,
            is NotOnboarded,
            is MissedDerivation,
            is Error,
            -> this
        }
    }

    /** The vault state is being read for the first time. */
    @Serializable
    data object Loading : CollectiblesAccountStatusValue() {
        override val source: StatusSource = StatusSource.CACHE
    }

    /** The wallet has no gacha vault on chain yet. */
    @Serializable
    data object NotOnboarded : CollectiblesAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /**
     * The vault exists but this device has not derived its owner key yet — a wallet restored on a new phone.
     * Like a token with no derivation: a dash instead of a balance, a zero contribution to the wallet total, and
     * no notification, because deriving the key needs no card tap.
     */
    @Serializable
    data object MissedDerivation : CollectiblesAccountStatusValue() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    /** The vault exists and its `hot_delegate` is the key this device holds, so it can sign spins. */
    @Serializable
    data class Onboarded(
        override val source: StatusSource,
        val vaultBalance: CollectiblesVaultBalance? = null,
    ) : CollectiblesAccountStatusValue()

    /**
     * The vault exists but its `hot_delegate` is some other key — the app was reinstalled, or the delegate was
     * rotated on another device. Spins fail with error 2001 until `update_vault` points the vault back at this
     * device, which needs a signature from the vault's owner.
     */
    @Serializable
    data class UpdateRequired(
        override val source: StatusSource,
        val vaultBalance: CollectiblesVaultBalance? = null,
    ) : CollectiblesAccountStatusValue()

    /** Represents an error state for the collectibles account status. */
    @Serializable
    sealed class Error : CollectiblesAccountStatusValue() {

        /** The state of the vault could not be obtained. */
        @Serializable
        data object Unavailable : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}

/**
 * Whether the wallet has a collectibles account at all — the vault exists on chain, whichever device it currently
 * answers to. `null` is the absence of any record, which is not an account.
 */
val CollectiblesAccountStatusValue?.isAccountCreated: Boolean
    get() = when (this) {
        is CollectiblesAccountStatusValue.Onboarded,
        is CollectiblesAccountStatusValue.UpdateRequired,
        is CollectiblesAccountStatusValue.MissedDerivation,
        -> true
        null,
        is CollectiblesAccountStatusValue.Loading,
        is CollectiblesAccountStatusValue.NotOnboarded,
        is CollectiblesAccountStatusValue.Error,
        -> false
    }