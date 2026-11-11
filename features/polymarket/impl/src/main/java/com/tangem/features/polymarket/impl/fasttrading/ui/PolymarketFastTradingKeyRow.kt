package com.tangem.features.polymarket.impl.fasttrading.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowAddressText
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_snowflake_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM

@Composable
internal fun PolymarketFastTradingKeyRow(state: FastTradingKeyRowUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { KeyStatusAvatar(state.status) },
        titleSlot = { TangemRowText(text = statusLine(state.status), role = TangemRowTextRole.Subtitle) },
        subtitleSlot = { TangemRowText(text = state.deviceName, role = TangemRowTextRole.Title) },
        endSlot = if (state.isExpiringSoon) {
            {
                TangemBadge(
                    text = resourceReference(R.string.prediction_session_key_badge_expires_soon),
                    status = TangemBadge.Status.Warning,
                    size = TangemBadge.Size.X4,
                )
            }
        } else {
            null
        },
    )
}

@Composable
internal fun PolymarketFastTradingUnknownSignerRow(state: FastTradingUnknownSignerUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Icon(Icons.ic_warning_20, TangemAvatar.Appearance.Warning),
                size = TangemAvatar.Size.X40,
            )
        },
        titleSlot = { TangemRowAddressText(address = state.address, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(
                text = resourceReference(R.string.prediction_session_key_unknown_signer),
                role = TangemRowTextRole.Subtitle,
            )
        },
    )
}

@Composable
private fun KeyStatusAvatar(status: FastTradingKeyStatusUM) {
    val icon = when (status) {
        is FastTradingKeyStatusUM.Active -> TangemAvatar.UiState.Icon(
            Icons.ic_checkmark_20,
            TangemAvatar.Appearance.Success,
        )
        is FastTradingKeyStatusUM.Paused -> TangemAvatar.UiState.Icon(
            Icons.ic_snowflake_20,
            TangemAvatar.Appearance.Info,
        )
        is FastTradingKeyStatusUM.Expired,
        FastTradingKeyStatusUM.ReconnectNeeded,
        -> TangemAvatar.UiState.Icon(Icons.ic_warning_20, TangemAvatar.Appearance.Warning)
        FastTradingKeyStatusUM.ActivationFailed,
        FastTradingKeyStatusUM.Unknown,
        -> TangemAvatar.UiState.Icon(Icons.ic_error_20, TangemAvatar.Appearance.Error)
        FastTradingKeyStatusUM.Activating,
        FastTradingKeyStatusUM.Revoking,
        FastTradingKeyStatusUM.Pausing,
        FastTradingKeyStatusUM.Resuming,
        FastTradingKeyStatusUM.Extending,
        -> null
    }
    if (icon != null) {
        TangemAvatar(state = icon, size = TangemAvatar.Size.X40)
    } else {
        Box(
            modifier = Modifier.size(40.dp).background(TangemTheme.colors3.bg.transparent.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            TangemLoader(size = TangemLoaderSize.X12)
        }
    }
}

@Composable
private fun statusLine(status: FastTradingKeyStatusUM): String {
    val parts = when (status) {
        is FastTradingKeyStatusUM.Active -> listOf(
            R.string.prediction_session_key_status_active to null,
            R.string.prediction_session_key_until to status.until,
        )
        is FastTradingKeyStatusUM.Paused -> listOf(
            R.string.prediction_session_key_status_paused to null,
            R.string.prediction_session_key_active_until to status.activeUntil,
        )
        is FastTradingKeyStatusUM.Expired -> listOfNotNull(
            R.string.prediction_session_key_status_expired to null,
            status.at?.let { R.string.prediction_session_key_expired_at to it },
        )
        FastTradingKeyStatusUM.Activating -> listOf(R.string.prediction_session_key_status_activating to null)
        FastTradingKeyStatusUM.Revoking -> listOf(R.string.prediction_session_key_status_revoking to null)
        FastTradingKeyStatusUM.Pausing -> listOf(R.string.prediction_session_key_status_pausing to null)
        FastTradingKeyStatusUM.Resuming -> listOf(R.string.prediction_session_key_status_resuming to null)
        FastTradingKeyStatusUM.Extending -> listOf(R.string.prediction_session_key_status_extending to null)
        FastTradingKeyStatusUM.ActivationFailed -> listOf(
            R.string.prediction_session_key_status_activation_failed to null,
        )
        FastTradingKeyStatusUM.ReconnectNeeded -> listOf(
            R.string.prediction_session_key_status_reconnect_needed to null,
        )
        FastTradingKeyStatusUM.Unknown -> listOf(R.string.common_error to null)
    }
    return parts
        .map { (id, arg) -> if (arg == null) stringResourceSafe(id) else stringResourceSafe(id, arg) }
        .joinToString(separator = STATUS_SEPARATOR)
}

private const val STATUS_SEPARATOR = " • "