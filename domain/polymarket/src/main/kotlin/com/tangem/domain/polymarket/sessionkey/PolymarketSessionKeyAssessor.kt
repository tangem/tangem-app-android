package com.tangem.domain.polymarket.sessionkey

import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState.Replace
import java.time.Duration
import java.time.Instant

object PolymarketSessionKeyAssessor {

    private val SIGNING_EXPIRY_MARGIN: Duration = Duration.ofMinutes(10)
    private val READINESS_GRACE: Duration = Duration.ofMinutes(10)

    fun assess(snapshot: PolymarketSessionKeySnapshot): PolymarketSessionKeyAssessment {
        val reconciliation = reconcile(snapshot)
        return PolymarketSessionKeyAssessment(
            deviceState = resolveDeviceState(snapshot, reconciliation),
            reconciliation = reconciliation,
        )
    }

    private fun resolveDeviceState(
        snapshot: PolymarketSessionKeySnapshot,
        reconciliation: PolymarketSessionKeyReconciliation,
    ): PolymarketSessionKeyDeviceState {
        val missingOnExchange = (reconciliation as? PolymarketSessionKeyReconciliation.Checked)
            ?.missingOnExchange
            .orEmpty()
        val own = snapshot.localKeyAddress?.let { local -> snapshot.sessionKeys.firstOrNull { it.address == local } }

        own?.let { resolveLiveOwnState(it, snapshot.now, missingOnExchange) }?.let { return it }

        val previous = snapshot.sessionKeys.firstOrNull { it.isLivePreviousOf(snapshot) }
        if (previous != null) {
            return when {
                previous.status != PolymarketSessionKeyStatus.ACTIVE -> {
                    PolymarketSessionKeyDeviceState.WaitingForPrevious(previous)
                }
                previous.address in missingOnExchange -> Replace.ByOverwrite(previous)
                else -> Replace.ByRevocation(previous)
            }
        }

        return when {
            own == null -> PolymarketSessionKeyDeviceState.OfferEnable
            own.status == PolymarketSessionKeyStatus.FAILED && snapshot.signers.orEmpty().any {
                it.address == own.address
            } -> PolymarketSessionKeyDeviceState.RepairByRenewal(own)
            else -> PolymarketSessionKeyDeviceState.Reenable(own)
        }
    }

    private fun resolveLiveOwnState(
        own: PolymarketSessionKey,
        now: Instant,
        missingOnExchange: Set<String>,
    ): PolymarketSessionKeyDeviceState? = when (own.status) {
        PolymarketSessionKeyStatus.PENDING -> PolymarketSessionKeyDeviceState.Enabling(own)
        PolymarketSessionKeyStatus.REVOKING -> PolymarketSessionKeyDeviceState.Revoking(own)
        PolymarketSessionKeyStatus.ACTIVE -> when {
            own.isExpiredAt(now) || own.address in missingOnExchange -> {
                PolymarketSessionKeyDeviceState.Reenable(own)
            }
            !own.isEnabled -> PolymarketSessionKeyDeviceState.Disabled(own, isRenewDue = own.isRenewDueAt(now))
            own.isExpiredAt(now + SIGNING_EXPIRY_MARGIN) -> PolymarketSessionKeyDeviceState.RenewalRequired(own)
            else -> PolymarketSessionKeyDeviceState.Active(own, isRenewDue = own.isRenewDueAt(now))
        }
        PolymarketSessionKeyStatus.REVOKED,
        PolymarketSessionKeyStatus.EXPIRED,
        PolymarketSessionKeyStatus.FAILED,
        PolymarketSessionKeyStatus.UNKNOWN,
        -> null
    }

    private fun PolymarketSessionKey.isLivePreviousOf(snapshot: PolymarketSessionKeySnapshot): Boolean {
        if (deviceId != snapshot.deviceId || address == snapshot.localKeyAddress) return false
        return when (status) {
            PolymarketSessionKeyStatus.PENDING,
            PolymarketSessionKeyStatus.REVOKING,
            -> true
            PolymarketSessionKeyStatus.ACTIVE -> !isExpiredAt(snapshot.now)
            PolymarketSessionKeyStatus.REVOKED,
            PolymarketSessionKeyStatus.EXPIRED,
            PolymarketSessionKeyStatus.FAILED,
            PolymarketSessionKeyStatus.UNKNOWN,
            -> false
        }
    }

    private fun reconcile(snapshot: PolymarketSessionKeySnapshot): PolymarketSessionKeyReconciliation {
        val signers = snapshot.signers ?: return PolymarketSessionKeyReconciliation.Unavailable
        val listed = signers.mapTo(mutableSetOf()) { it.address }
        val missingOnExchange = snapshot.sessionKeys
            .filter { it.isSettledActiveAt(snapshot.now) && it.address !in listed }
            .mapTo(mutableSetOf()) { it.address }
        return PolymarketSessionKeyReconciliation.Checked(
            foreignSigners = signers.filter { it.isForeign(snapshot) },
            missingOnExchange = missingOnExchange,
        )
    }

    private fun PolymarketSessionSigner.isForeign(snapshot: PolymarketSessionKeySnapshot): Boolean {
        val record = snapshot.sessionKeys.firstOrNull { it.address == address } ?: return true
        return when (record.status) {
            PolymarketSessionKeyStatus.PENDING,
            PolymarketSessionKeyStatus.REVOKING,
            -> false
            PolymarketSessionKeyStatus.ACTIVE -> record.isPastReadinessGraceAt(snapshot.now) && !hasTermsOf(record)
            PolymarketSessionKeyStatus.FAILED -> record.address != snapshot.localKeyAddress
            PolymarketSessionKeyStatus.REVOKED,
            PolymarketSessionKeyStatus.EXPIRED,
            PolymarketSessionKeyStatus.UNKNOWN,
            -> true
        }
    }

    private fun PolymarketSessionSigner.hasTermsOf(record: PolymarketSessionKey): Boolean {
        return scopes.normalized() == record.scopes.normalized() && validUntil == record.validUntil
    }

    private fun List<String>.normalized(): Set<String> = mapTo(mutableSetOf()) { it.uppercase() }

    private fun PolymarketSessionKey.isSettledActiveAt(now: Instant): Boolean =
        status == PolymarketSessionKeyStatus.ACTIVE && !isExpiredAt(now) && isPastReadinessGraceAt(now)

    private fun PolymarketSessionKey.isPastReadinessGraceAt(now: Instant): Boolean {
        val authorizedAt = authorizedAt ?: return false
        return Duration.between(authorizedAt, now) >= READINESS_GRACE
    }

    private fun PolymarketSessionKey.isExpiredAt(moment: Instant): Boolean {
        val validUntil = validUntil ?: return true
        return moment >= validUntil
    }

    private fun PolymarketSessionKey.isRenewDueAt(now: Instant): Boolean {
        val renewAfter = renewAfter ?: return false
        return now >= renewAfter
    }
}