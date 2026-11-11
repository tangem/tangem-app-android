package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeysUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import kotlinx.collections.immutable.toImmutableList
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal class PolymarketFastTradingKeysUMConverter(
    private val thisDeviceName: String,
    private val clock: Clock = Clock.systemDefaultZone(),
    locale: Locale = Locale.getDefault(),
) {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .withZone(clock.zone)

    fun convert(overview: PolymarketSessionKeyOverview, operation: PolymarketSessionKeyOperation?): FastTradingKeysUM {
        val now = clock.instant()
        val reconciliation = overview.assessment.reconciliation as? PolymarketSessionKeyReconciliation.Checked
        val missingOnExchange = reconciliation?.missingOnExchange.orEmpty()
        val submittedOperation = operation?.takeIf { it.stage != PolymarketSessionKeyOperationStage.SIGNING }
        val localKeyAddress = overview.localKeyAddress

        val rows = overview.wallet.sessionKeys
            .filter { it.status != PolymarketSessionKeyStatus.REVOKED }
            .map { key ->
                val isOwn = key.address.equals(localKeyAddress, ignoreCase = true)
                key.address to FastTradingKeyRowUM(
                    id = key.address,
                    deviceName = key.deviceName?.let(::stringReference)
                        ?: resourceReference(R.string.prediction_session_key_unknown_device),
                    status = submittedOperation?.statusFor(key.address, isOwn) ?: key.status(now, missingOnExchange),
                    isExpiringSoon = key.isExpiringSoon(now),
                )
            }
        val ownRow = rows.firstOrNull { (address, _) -> address.equals(localKeyAddress, ignoreCase = true) }?.second

        return FastTradingKeysUM(
            unknownSigners = reconciliation?.foreignSigners.orEmpty()
                .map { FastTradingUnknownSignerUM(address = it.address) }
                .toImmutableList(),
            thisDevice = ownRow ?: submittedOperation?.pendingThisDeviceRow(),
            otherDevices = rows.map { it.second }.filter { it !== ownRow }.toImmutableList(),
        )
    }

    private fun PolymarketSessionKey.status(now: Instant, missingOnExchange: Set<String>): FastTradingKeyStatusUM =
        when (status) {
            PolymarketSessionKeyStatus.PENDING -> FastTradingKeyStatusUM.Activating
            PolymarketSessionKeyStatus.REVOKING -> FastTradingKeyStatusUM.Revoking
            PolymarketSessionKeyStatus.FAILED -> FastTradingKeyStatusUM.ActivationFailed
            PolymarketSessionKeyStatus.EXPIRED -> FastTradingKeyStatusUM.Expired(
                at = validUntil?.let(dateFormatter::format),
            )
            PolymarketSessionKeyStatus.ACTIVE -> activeStatus(now, missingOnExchange)
            PolymarketSessionKeyStatus.REVOKED,
            PolymarketSessionKeyStatus.UNKNOWN,
            -> FastTradingKeyStatusUM.Unknown
        }

    private fun PolymarketSessionKey.activeStatus(
        now: Instant,
        missingOnExchange: Set<String>,
    ): FastTradingKeyStatusUM {
        val until = validUntil
        return when {
            until == null || !until.isAfter(now) -> FastTradingKeyStatusUM.Expired(
                at = until?.let(dateFormatter::format),
            )
            address in missingOnExchange -> FastTradingKeyStatusUM.ReconnectNeeded
            !isEnabled -> FastTradingKeyStatusUM.Paused(activeUntil = dateFormatter.format(until))
            else -> FastTradingKeyStatusUM.Active(until = dateFormatter.format(until))
        }
    }

    private fun PolymarketSessionKey.isExpiringSoon(now: Instant): Boolean {
        val until = validUntil ?: return false
        val isLive = status == PolymarketSessionKeyStatus.ACTIVE && until.isAfter(now)
        return isLive && Duration.between(now, until) <= EXPIRY_WARNING_WINDOW
    }

    private fun PolymarketSessionKeyOperation.statusFor(address: String, isOwn: Boolean): FastTradingKeyStatusUM? {
        if (!address.equals(targetAddress, ignoreCase = true)) return null
        return when (val request = request) {
            PolymarketSessionKeyOperationRequest.Activate,
            PolymarketSessionKeyOperationRequest.Reactivate,
            -> if (!isOwn && stage in REVOCATION_STAGES) {
                FastTradingKeyStatusUM.Revoking
            } else {
                FastTradingKeyStatusUM.Activating
            }
            PolymarketSessionKeyOperationRequest.Extend -> FastTradingKeyStatusUM.Extending
            is PolymarketSessionKeyOperationRequest.Revoke -> FastTradingKeyStatusUM.Revoking
            is PolymarketSessionKeyOperationRequest.SetEnabled -> if (request.isEnabled) {
                FastTradingKeyStatusUM.Resuming
            } else {
                FastTradingKeyStatusUM.Pausing
            }
        }
    }

    private fun PolymarketSessionKeyOperation.pendingThisDeviceRow(): FastTradingKeyRowUM? {
        val status = when (request) {
            PolymarketSessionKeyOperationRequest.Activate,
            PolymarketSessionKeyOperationRequest.Reactivate,
            -> FastTradingKeyStatusUM.Activating
            PolymarketSessionKeyOperationRequest.Extend -> FastTradingKeyStatusUM.Extending
            is PolymarketSessionKeyOperationRequest.Revoke,
            is PolymarketSessionKeyOperationRequest.SetEnabled,
            -> return null
        }
        return FastTradingKeyRowUM(
            id = THIS_DEVICE_PENDING_ROW_ID,
            deviceName = stringReference(thisDeviceName),
            status = status,
            isExpiringSoon = false,
        )
    }

    private companion object {
        val EXPIRY_WARNING_WINDOW: Duration = Duration.ofDays(30)
        const val THIS_DEVICE_PENDING_ROW_ID = "this_device_pending"
        val REVOCATION_STAGES =
            setOf(PolymarketSessionKeyOperationStage.REVOKING, PolymarketSessionKeyOperationStage.REVOKED)
    }
}