package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_snowflake_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyActionUM.RevokeSubject
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyRowUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingKeyStatusUM
import com.tangem.features.polymarket.impl.fasttrading.ui.state.FastTradingUnknownSignerUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PolymarketFastTradingKeyRow(
    state: FastTradingKeyRowUM,
    areActionsEnabled: Boolean,
    onActionClick: (FastTradingKeyActionUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { KeyStatusAvatar(state.status) },
        titleSlot = { TangemRowText(text = statusLine(state.status), role = TangemRowTextRole.Subtitle) },
        subtitleSlot = { TangemRowText(text = state.deviceName, role = TangemRowTextRole.Title) },
        endSlot = if (state.isExpiringSoon || state.actions.isNotEmpty()) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.isExpiringSoon) {
                        TangemBadge(
                            text = resourceReference(R.string.prediction_session_key_badge_expires_soon),
                            status = TangemBadge.Status.Warning,
                            size = TangemBadge.Size.X4,
                        )
                    }
                    if (state.actions.isNotEmpty()) {
                        PolymarketFastTradingKeyActionsMenu(
                            actions = state.actions,
                            isEnabled = areActionsEnabled,
                            onActionClick = onActionClick,
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
internal fun PolymarketFastTradingUnknownSignerRow(
    state: FastTradingUnknownSignerUM,
    areActionsEnabled: Boolean,
    onActionClick: (FastTradingKeyActionUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            if (state.isRevoking) {
                KeyStatusAvatar(FastTradingKeyStatusUM.Revoking)
            } else {
                TangemAvatar(
                    state = TangemAvatar.UiState.Icon(Icons.ic_warning_20, TangemAvatar.Appearance.Warning),
                    size = TangemAvatar.Size.X40,
                )
            }
        },
        titleSlot = { TangemRowAddressText(address = state.address, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(
                text = resourceReference(
                    if (state.isRevoking) {
                        R.string.prediction_session_key_status_revoking
                    } else {
                        R.string.prediction_session_key_unknown_signer
                    },
                ),
                role = TangemRowTextRole.Subtitle,
            )
        },
        endSlot = if (state.actions.isNotEmpty()) {
            {
                PolymarketFastTradingKeyActionsMenu(
                    actions = state.actions,
                    isEnabled = areActionsEnabled,
                    onActionClick = onActionClick,
                )
            }
        } else {
            null
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
        FastTradingKeyStatusUM.UnknownSigner,
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
        FastTradingKeyStatusUM.Activating,
        FastTradingKeyStatusUM.Revoking,
        FastTradingKeyStatusUM.Pausing,
        FastTradingKeyStatusUM.Resuming,
        FastTradingKeyStatusUM.Extending,
        FastTradingKeyStatusUM.ActivationFailed,
        FastTradingKeyStatusUM.ReconnectNeeded,
        FastTradingKeyStatusUM.Unknown,
        FastTradingKeyStatusUM.UnknownSigner,
        -> listOf(singleStatusId(status) to null)
    }
    return parts
        .map { (id, arg) -> if (arg == null) stringResourceSafe(id) else stringResourceSafe(id, arg) }
        .joinToString(separator = STATUS_SEPARATOR)
}

@StringRes
private fun singleStatusId(status: FastTradingKeyStatusUM): Int = when (status) {
    FastTradingKeyStatusUM.Activating -> R.string.prediction_session_key_status_activating
    FastTradingKeyStatusUM.Revoking -> R.string.prediction_session_key_status_revoking
    FastTradingKeyStatusUM.Pausing -> R.string.prediction_session_key_status_pausing
    FastTradingKeyStatusUM.Resuming -> R.string.prediction_session_key_status_resuming
    FastTradingKeyStatusUM.Extending -> R.string.prediction_session_key_status_extending
    FastTradingKeyStatusUM.ActivationFailed -> R.string.prediction_session_key_status_activation_failed
    FastTradingKeyStatusUM.ReconnectNeeded -> R.string.prediction_session_key_status_reconnect_needed
    FastTradingKeyStatusUM.UnknownSigner -> R.string.prediction_session_key_unknown_signer
    is FastTradingKeyStatusUM.Active,
    is FastTradingKeyStatusUM.Paused,
    is FastTradingKeyStatusUM.Expired,
    FastTradingKeyStatusUM.Unknown,
    -> R.string.common_error
}

private const val STATUS_SEPARATOR = " • "

@Preview(name = "Key actions", widthDp = 390)
@Preview(name = "Key actions dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingKeyRowActionsPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            KeyRowsPreview(areActionsEnabled = true)
        }
    }
}

@Preview(name = "Key actions disabled", widthDp = 390)
@Preview(name = "Key actions disabled dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingKeyRowDisabledActionsPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            KeyRowsPreview(areActionsEnabled = false)
        }
    }
}

@Composable
private fun KeyRowsPreview(areActionsEnabled: Boolean) {
    PolymarketFastTradingUnknownSignerRow(
        state = FastTradingUnknownSignerUM(
            address = PREVIEW_SIGNER_ADDRESS,
            actions = persistentListOf(
                FastTradingKeyActionUM.Revoke(PREVIEW_SIGNER_ADDRESS, RevokeSubject.UnknownSigner),
            ),
        ),
        areActionsEnabled = areActionsEnabled,
        onActionClick = {},
    )
    PolymarketFastTradingUnknownSignerRow(
        state = FastTradingUnknownSignerUM(
            address = PREVIEW_SIGNER_ADDRESS,
            actions = persistentListOf(),
            isRevoking = true,
        ),
        areActionsEnabled = areActionsEnabled,
        onActionClick = {},
    )
    PolymarketFastTradingKeyRow(
        state = FastTradingKeyRowUM(
            id = "0x1",
            deviceName = stringReference("Pixel 9"),
            status = FastTradingKeyStatusUM.Active(until = "20 Apr 2027"),
            isExpiringSoon = true,
            actions = persistentListOf(
                FastTradingKeyActionUM.Revoke("0x1", RevokeSubject.Device(stringReference("Pixel 9"))),
            ),
        ),
        areActionsEnabled = areActionsEnabled,
        onActionClick = {},
    )
    PolymarketFastTradingKeyRow(
        state = FastTradingKeyRowUM(
            id = "0x2",
            deviceName = stringReference("iPhone 16"),
            status = FastTradingKeyStatusUM.Revoking,
            isExpiringSoon = false,
            actions = persistentListOf(),
        ),
        areActionsEnabled = areActionsEnabled,
        onActionClick = {},
    )
}

private const val PREVIEW_SIGNER_ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"