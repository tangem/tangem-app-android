package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class CustomTokenFormUM(
    val networkName: ClickableFieldUM,
    val contractAddress: TextInputFieldUM,
    val tokenName: TextInputFieldUM,
    val tokenSymbol: TextInputFieldUM,
    val tokenDecimals: TextInputFieldUM,
    val derivationPath: ClickableFieldUM,
    val notifications: ImmutableList<NotificationUM>,
    val canAddToken: Boolean,
    val onNetworkClick: () -> Unit,
    val onDerivationPathClick: () -> Unit,
    val onAddClick: () -> Unit,
) {

    @Immutable
    data class NotificationUM(
        val id: String,
        val config: NotificationConfig,
    )
}

@Immutable
internal data class TextInputFieldUM(
    val label: TextReference,
    val placeholder: TextReference,
    val value: String,
    val onValueChange: (String) -> Unit,
    val error: TextReference? = null,
)

@Immutable
internal data class ClickableFieldUM(
    val label: TextReference,
    val value: TextReference,
    val onClick: () -> Unit,
)