package com.tangem.features.managetokens.entity.customtoken

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class CustomTokenFormUM(
    val networkName: ClickableFieldUM,
    val derivationPath: ClickableFieldUM,
    val tokenForm: TokenFormUM?,
    val notifications: ImmutableList<NotificationUM> = persistentListOf(),
    val canAddToken: Boolean = false,
    val isValidating: Boolean = false,
    val saveToken: () -> Unit,
) {

    data class TokenFormUM(
        val contractAddress: TextInputFieldUM,
        val name: TextInputFieldUM,
        val symbol: TextInputFieldUM,
        val decimals: TextInputFieldUM,
    )

    data class NotificationUM(
        val id: String,
        val config: NotificationConfig,
    )
}

internal data class TextInputFieldUM(
    val label: TextReference,
    val placeholder: TextReference,
    val value: String = "",
    val error: TextReference? = null,
    val isEnabled: Boolean = true,
    val onValueChange: (String) -> Unit,
)

internal data class ClickableFieldUM(
    val label: TextReference,
    val value: TextReference,
    val onClick: () -> Unit,
)
