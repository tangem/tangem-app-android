package com.tangem.features.polymarket.impl.fasttrading.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
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
        val now = clock.instant()
        val reconciliation = overview.assessment.reconciliation as? PolymarketSessionKeyReconciliation.Checked
        val missingOnExchange = reconciliation?.missingOnExchange.orEmpty()
        val submittedOperation = operation?.takeIf { it.stage != PolymarketSessionKeyOperationStage.SIGNING }
        val localKeyAddress = overview.localKeyAddress
        val foreignSigners = reconciliation?.foreignSigners.orEmpty()
        val listedSigners = reconciliation?.listedSigners.orEmpty()
        val foreignAddresses = foreignSigners.mapTo(mutableSetOf()) { it.address.lowercase() }

        val shownKeys = overview.wallet.sessionKeys
            .filter { it.status != PolymarketSessionKeyStatus.REVOKED }
            .map { it.address.lowercase() to it }
        val shownAddresses = shownKeys.mapTo(mutableSetOf()) { (address, _) -> address }
        val rows = shownKeys
            .map { (address, key) ->
                val isOwn = key.address.equals(localKeyAddress, ignoreCase = true)
                val isForeign = address in foreignAddresses
                val status = submittedOperation?.statusFor(key.address, isOwn)
                    ?: if (isForeign) FastTradingKeyStatusUM.UnknownSigner else key.status(now, missingOnExchange)
                val deviceName = key.deviceName?.let(::stringReference)
                    ?: resourceReference(R.string.prediction_session_key_unknown_device)
                key.address to FastTradingKeyRowUM(
                    id = key.address,
                    deviceName = deviceName,
                    status = status,
                    isExpiringSoon = !isForeign && key.isExpiringSoon(now),
                    actions = actionsFor(
                        revoke = revokeOf(
                            address = key.address,
                            subject = if (isForeign) {
                                FastTradingKeyActionUM.RevokeSubject.UnknownSigner
                            } else {
                                FastTradingKeyActionUM.RevokeSubject.Device(deviceName)
                            },
                        ),
                        status = status,
                        isListed = address in listedSigners,
                    ),
                )
            }
        val deviceState = overview.assessment.deviceState
        val isActivationOffered = deviceState.isActivationOffered()
        val ownRow = rows.rowOf(localKeyAddress)?.takeIf { (address, _) ->
            !isActivationOffered || address in listedSigners
        }
        val thisDeviceRows = thisDeviceRowsOf(
            ownRow = ownRow,
            previousRow = deviceState.previousKey()?.let { rows.rowOf(it.address) },
            submittedOperation = submittedOperation,
        )

        return FastTradingKeysUM(
            isUnknownSignersWarningShown = foreignSigners.isNotEmpty(),
            unknownSigners = unknownSignersOf(foreignSigners, shownAddresses, submittedOperation),
            thisDevice = thisDeviceRows.toImmutableList(),
            activateBanner = activateBannerOf(isActivationOffered, operation),
            otherDevices = rows
                .filter { (address, row) ->
                    !address.equals(localKeyAddress, ignoreCase = true) && thisDeviceRows.none { it === row }
                }
                .map { (_, row) -> row }
                .toImmutableList(),
            areActionsEnabled = operation == null,
        )
    }

    private fun unknownSignersOf(
        foreignSigners: List<PolymarketSessionSigner>,
        shownAddresses: Set<String>,
        submittedOperation: PolymarketSessionKeyOperation?,
    ): ImmutableList<FastTradingUnknownSignerUM> = foreignSigners
        .filter { it.address.lowercase() !in shownAddresses }
        .map { signer ->
            val revoke = revokeOf(signer.address, FastTradingKeyActionUM.RevokeSubject.UnknownSigner)
            val isRevoking = submittedOperation?.statusFor(signer.address, isOwn = false) ==
                FastTradingKeyStatusUM.Revoking
            FastTradingUnknownSignerUM(
                address = signer.address,
                actions = if (isRevoking) persistentListOf() else persistentListOf(revoke),
                isRevoking = isRevoking,
            )
        }
        .toImmutableList()

    private fun thisDeviceRowsOf(
        ownRow: Pair<String, FastTradingKeyRowUM>?,
        previousRow: Pair<String, FastTradingKeyRowUM>?,
        submittedOperation: PolymarketSessionKeyOperation?,
    ): List<FastTradingKeyRowUM> {
        val keyRows = listOfNotNull(ownRow, previousRow).distinct().map { (_, row) -> row }
        val pendingRow = submittedOperation?.pendingThisDeviceRow()
            ?.takeUnless { pending -> keyRows.any { it.status == pending.status } }
        return listOfNotNull(pendingRow) + keyRows
    }

    private fun List<Pair<String, FastTradingKeyRowUM>>.rowOf(address: String?): Pair<String, FastTradingKeyRowUM>? =
        firstOrNull { (rowAddress, _) -> rowAddress.equals(address, ignoreCase = true) }

    private fun PolymarketSessionKeyDeviceState.previousKey(): PolymarketSessionKey? = when (this) {
        is PolymarketSessionKeyDeviceState.WaitingForPrevious -> previous
        is PolymarketSessionKeyDeviceState.Replace -> previous
        else -> null
    }

    private fun activateBannerOf(
        isActivationOffered: Boolean,
        operation: PolymarketSessionKeyOperation?,
    ): FastTradingActivateBannerUM? = when {
        !isActivationOffered -> null
        operation == null -> FastTradingActivateBannerUM(isLoading = false)
        !operation.isActivation() -> FastTradingActivateBannerUM(isLoading = false)
        operation.stage == PolymarketSessionKeyOperationStage.SIGNING -> FastTradingActivateBannerUM(isLoading = true)
        else -> null
    }

    private fun PolymarketSessionKeyOperation.isActivation(): Boolean =
        request == PolymarketSessionKeyOperationRequest.Activate ||
            request == PolymarketSessionKeyOperationRequest.Reactivate

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
            actions = persistentListOf(),
        )
    }

    private companion object {
        val EXPIRY_WARNING_WINDOW: Duration = Duration.ofDays(30)
        const val THIS_DEVICE_PENDING_ROW_ID = "this_device_pending"
        val REVOCATION_STAGES =
            setOf(PolymarketSessionKeyOperationStage.REVOKING, PolymarketSessionKeyOperationStage.REVOKED)
    }
}