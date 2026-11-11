package com.tangem.features.polymarket.impl.fasttrading.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PolymarketFastTradingSettingsUM {

    val onBackClick: () -> Unit

    data class Loading(override val onBackClick: () -> Unit) : PolymarketFastTradingSettingsUM

    data class Error(
        override val onBackClick: () -> Unit,
        val onRetryClick: () -> Unit,
    ) : PolymarketFastTradingSettingsUM

    data class Content(
        override val onBackClick: () -> Unit,
        val keys: FastTradingKeysUM,
        val pullToRefreshConfig: PullToRefreshConfig,
        val onActionClick: (FastTradingKeyActionUM) -> Unit,
    ) : PolymarketFastTradingSettingsUM
}

@Immutable
internal data class FastTradingKeysUM(
    val isUnknownSignersWarningShown: Boolean,
    val unknownSigners: ImmutableList<FastTradingUnknownSignerUM>,
    val thisDevice: FastTradingKeyRowUM?,
    val otherDevices: ImmutableList<FastTradingKeyRowUM>,
    val areActionsEnabled: Boolean,
)

@Immutable
internal data class FastTradingUnknownSignerUM(
    val address: String,
    val actions: ImmutableList<FastTradingKeyActionUM>,
)

@Immutable
internal data class FastTradingKeyRowUM(
    val id: String,
    val deviceName: TextReference,
    val status: FastTradingKeyStatusUM,
    val isExpiringSoon: Boolean,
    val actions: ImmutableList<FastTradingKeyActionUM>,
)

@Immutable
internal sealed interface FastTradingKeyActionUM {

    data class Revoke(val address: String, val subject: RevokeSubject) : FastTradingKeyActionUM

    sealed interface RevokeSubject {

        data class Device(val name: TextReference) : RevokeSubject

        data object UnknownSigner : RevokeSubject
    }
}

@Immutable
internal sealed interface FastTradingKeyStatusUM {

    data class Active(val until: String) : FastTradingKeyStatusUM

    data class Paused(val activeUntil: String) : FastTradingKeyStatusUM

    data class Expired(val at: String?) : FastTradingKeyStatusUM

    data object Activating : FastTradingKeyStatusUM

    data object Revoking : FastTradingKeyStatusUM

    data object Pausing : FastTradingKeyStatusUM

    data object Resuming : FastTradingKeyStatusUM

    data object Extending : FastTradingKeyStatusUM

    data object ActivationFailed : FastTradingKeyStatusUM

    data object ReconnectNeeded : FastTradingKeyStatusUM

    data object UnknownSigner : FastTradingKeyStatusUM

    data object Unknown : FastTradingKeyStatusUM
}