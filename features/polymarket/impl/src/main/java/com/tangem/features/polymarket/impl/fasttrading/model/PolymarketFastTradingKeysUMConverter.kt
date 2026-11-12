package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.newestPerAddress
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingActivateBannerUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeysUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
        val reconciliation = overview.assessment.reconciliation as? PolymarketSessionKeyReconciliation.Checked
        val foreignSigners = reconciliation?.foreignSigners.orEmpty()
        val submittedOperation = operation?.takeIf { it.stage != PolymarketSessionKeyOperationStage.SIGNING }
        val context = RowContext(
            now = clock.instant(),
            submittedOperation = submittedOperation,
            operationTarget = submittedOperation?.targetAddress?.lowercase(),
            localKeyAddress = overview.localKeyAddress?.lowercase(),
            thisDeviceId = overview.deviceId,
            missingOnExchange = reconciliation?.missingOnExchange.orEmpty(),
            listedSigners = reconciliation?.listedSigners.orEmpty(),
            foreignAddresses = foreignSigners.mapTo(mutableSetOf()) { it.address.lowercase() },
        )
        val rows = overview.wallet.sessionKeys
            .newestPerAddress()
            .filter { it.status != PolymarketSessionKeyStatus.REVOKED }
            .map { key -> keyRowOf(key, context) }
        val isActivationOffered = overview.assessment.deviceState.isActivationOffered()
        val thisDeviceRows = rows
            .filter { it.isThisDevice }
            .filterNot { keyRow ->
                val isHideable = keyRow.isDead || keyRow.address == context.localKeyAddress && isActivationOffered
                isHideable && reconciliation != null && keyRow.address !in context.listedSigners
            }
            .sortedByDescending { it.address == context.localKeyAddress }

        return FastTradingKeysUM(
            isUnknownSignersWarningShown = foreignSigners.isNotEmpty(),
            unknownSigners = unknownSignersOf(foreignSigners, rows.mapTo(mutableSetOf()) { it.address }, context),
            thisDevice = thisDeviceRowsOf(thisDeviceRows, context).toImmutableList(),
            activateBanner = activateBannerOf(isActivationOffered, operation),
            otherDevices = rows.filterNot { it.isThisDevice }.map { it.row }.toImmutableList(),
            areActionsEnabled = operation == null,
        )
    }

    private fun keyRowOf(key: PolymarketSessionKey, context: RowContext): KeyRow {
        val address = key.address.lowercase()
        val isForeign = address in context.foreignAddresses
        val status = context.operationStatusFor(address)
            ?: if (isForeign) FastTradingKeyStatusUM.UnknownSigner else key.status(context)
        val deviceName = key.deviceName?.let(::stringReference)
            ?: resourceReference(R.string.prediction_session_key_unknown_device)
        val revoke = revokeOf(
            address = key.address,
            subject = if (isForeign) {
                FastTradingKeyActionUM.RevokeSubject.UnknownSigner
            } else {
                FastTradingKeyActionUM.RevokeSubject.Device(deviceName)
            },
        )
        return KeyRow(
            address = address,
            isThisDevice = address == context.localKeyAddress || key.deviceId == context.thisDeviceId,
            isDead = key.isDeadAt(context.now),
            row = FastTradingKeyRowUM(
                id = key.address,
                deviceName = deviceName,
                status = status,
                isExpiringSoon = !isForeign && key.isExpiringSoon(context.now),
                actions = actionsFor(revoke = revoke, status = status, isListed = address in context.listedSigners),
            ),
        )
    }

    private fun unknownSignersOf(
        foreignSigners: List<PolymarketSessionSigner>,
        shownAddresses: Set<String>,
        context: RowContext,
    ): ImmutableList<FastTradingUnknownSignerUM> = foreignSigners
        .filter { it.address.lowercase() !in shownAddresses }
        .map { signer ->
            val revoke = revokeOf(signer.address, FastTradingKeyActionUM.RevokeSubject.UnknownSigner)
            val isRevoking = context.operationStatusFor(signer.address.lowercase()) ==
                FastTradingKeyStatusUM.Revoking
            FastTradingUnknownSignerUM(
                address = signer.address,
                actions = if (isRevoking) persistentListOf() else persistentListOf(revoke),
                isRevoking = isRevoking,
            )
        }
        .toImmutableList()

    private fun thisDeviceRowsOf(keyRows: List<KeyRow>, context: RowContext): List<FastTradingKeyRowUM> {
        val isTracked = keyRows.any { it.address == context.operationTarget } && !context.isRevokingAnotherKey()
        val pendingRow = context.submittedOperation?.takeUnless { isTracked }?.pendingThisDeviceRow()
        return listOfNotNull(pendingRow) + keyRows.map { it.row }
    }

    private fun RowContext.isRevokingAnotherKey(): Boolean {
        val operation = submittedOperation ?: return false
        return operation.request.isActivation && operation.stage in REVOCATION_STAGES &&
            operationTarget != localKeyAddress
    }

    private fun PolymarketSessionKey.isDeadAt(now: Instant): Boolean {
        val until = validUntil
        return status in DEAD_STATUSES ||
            status == PolymarketSessionKeyStatus.ACTIVE && (until == null || !until.isAfter(now))
    }

    private fun activateBannerOf(
        isActivationOffered: Boolean,
        operation: PolymarketSessionKeyOperation?,
    ): FastTradingActivateBannerUM? = when {
        !isActivationOffered -> null
        operation == null -> FastTradingActivateBannerUM(isLoading = false, isEnabled = true)
        !operation.request.isActivation -> FastTradingActivateBannerUM(isLoading = false, isEnabled = false)
        operation.stage == PolymarketSessionKeyOperationStage.SIGNING -> {
            FastTradingActivateBannerUM(isLoading = true, isEnabled = false)
        }
        else -> null
    }

    private fun PolymarketSessionKeyDeviceState.isActivationOffered(): Boolean = when (this) {
        PolymarketSessionKeyDeviceState.OfferEnable,
        is PolymarketSessionKeyDeviceState.Reenable,
        is PolymarketSessionKeyDeviceState.RepairByRenewal,
        is PolymarketSessionKeyDeviceState.Replace,
        -> true
        is PolymarketSessionKeyDeviceState.Enabling,
        is PolymarketSessionKeyDeviceState.Revoking,
        is PolymarketSessionKeyDeviceState.Active,
        is PolymarketSessionKeyDeviceState.Disabled,
        is PolymarketSessionKeyDeviceState.RenewalRequired,
        is PolymarketSessionKeyDeviceState.WaitingForPrevious,
        -> false
    }

    private fun revokeOf(address: String, subject: FastTradingKeyActionUM.RevokeSubject) =
        FastTradingKeyActionUM.Revoke(address = address, subject = subject)

    private fun actionsFor(
        revoke: FastTradingKeyActionUM.Revoke,
        status: FastTradingKeyStatusUM,
        isListed: Boolean,
    ): ImmutableList<FastTradingKeyActionUM> = when (status) {
        is FastTradingKeyStatusUM.Active,
        is FastTradingKeyStatusUM.Paused,
        FastTradingKeyStatusUM.UnknownSigner,
        -> persistentListOf(revoke)
        is FastTradingKeyStatusUM.Expired,
        FastTradingKeyStatusUM.ReconnectNeeded,
        FastTradingKeyStatusUM.ActivationFailed,
        FastTradingKeyStatusUM.Unknown,
        -> if (isListed) persistentListOf(revoke) else persistentListOf()
        FastTradingKeyStatusUM.Activating,
        FastTradingKeyStatusUM.Revoking,
        FastTradingKeyStatusUM.Pausing,
        FastTradingKeyStatusUM.Resuming,
        FastTradingKeyStatusUM.Extending,
        -> persistentListOf()
    }

    private fun PolymarketSessionKey.status(context: RowContext): FastTradingKeyStatusUM = when (status) {
        PolymarketSessionKeyStatus.PENDING -> FastTradingKeyStatusUM.Activating
        PolymarketSessionKeyStatus.REVOKING -> FastTradingKeyStatusUM.Revoking
        PolymarketSessionKeyStatus.FAILED -> FastTradingKeyStatusUM.ActivationFailed
        PolymarketSessionKeyStatus.EXPIRED -> FastTradingKeyStatusUM.Expired(
            at = validUntil?.let(dateFormatter::format),
        )
        PolymarketSessionKeyStatus.ACTIVE -> activeStatus(context.now, context.missingOnExchange)
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

    private fun RowContext.operationStatusFor(address: String): FastTradingKeyStatusUM? {
        val operation = submittedOperation ?: return null
        if (address != operationTarget) return null
        return when (val request = operation.request) {
            PolymarketSessionKeyOperationRequest.Activate,
            PolymarketSessionKeyOperationRequest.Reactivate,
            -> if (isRevokingAnotherKey()) {
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
        val status = when {
            request.isActivation -> FastTradingKeyStatusUM.Activating
            request == PolymarketSessionKeyOperationRequest.Extend -> FastTradingKeyStatusUM.Extending
            else -> return null
        }
        return FastTradingKeyRowUM(
            id = THIS_DEVICE_PENDING_ROW_ID,
            deviceName = stringReference(thisDeviceName),
            status = status,
            isExpiringSoon = false,
            actions = persistentListOf(),
        )
    }

    private data class RowContext(
        val now: Instant,
        val submittedOperation: PolymarketSessionKeyOperation?,
        val operationTarget: String?,
        val localKeyAddress: String?,
        val thisDeviceId: String,
        val missingOnExchange: Set<String>,
        val listedSigners: Set<String>,
        val foreignAddresses: Set<String>,
    )

    private data class KeyRow(
        val address: String,
        val isThisDevice: Boolean,
        val isDead: Boolean,
        val row: FastTradingKeyRowUM,
    )

    private companion object {
        val EXPIRY_WARNING_WINDOW: Duration = Duration.ofDays(30)
        const val THIS_DEVICE_PENDING_ROW_ID = "this_device_pending"
        val DEAD_STATUSES = setOf(
            PolymarketSessionKeyStatus.EXPIRED,
            PolymarketSessionKeyStatus.FAILED,
            PolymarketSessionKeyStatus.UNKNOWN,
        )
        val REVOCATION_STAGES =
            setOf(PolymarketSessionKeyOperationStage.REVOKING, PolymarketSessionKeyOperationStage.REVOKED)
    }
}

private val PolymarketSessionKeyOperationRequest.isActivation: Boolean
    get() = this == PolymarketSessionKeyOperationRequest.Activate ||
        this == PolymarketSessionKeyOperationRequest.Reactivate