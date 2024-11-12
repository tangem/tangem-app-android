package com.tangem.features.managetokens.entity.customtoken

import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf

internal data class CustomTokenFormUM(
    val networkName: ClickableFieldUM,
    val derivationPath: ClickableFieldUM,
    val tokenForm: TokenFormUM?,
    val notifications: PersistentList<NotificationUM> = persistentListOf(),
    val canAddToken: Boolean = false,
    val isValidating: Boolean = false,
    val needToAddDerivation: Boolean = false,
    val saveToken: () -> Unit,
) {

    data class TokenFormUM(
        val fields: PersistentMap<Field, TextInputFieldUM>,
        val wasFilled: Boolean = false,
    ) {

        enum class Field {
            CONTRACT_ADDRESS,
            NAME,
            SYMBOL,
            DECIMALS,
        }
    }

    data class NotificationUM(
        val id: String,
        val config: NotificationConfig,
    )
}

internal data class TextInputFieldUM(
    val label: TextReference,
    val placeholder: TextReference,
    val keyboardOptions: KeyboardOptions,
    val value: String = "",
    val isFocused: Boolean = false,
    val error: TextReference? = null,
    val isEnabled: Boolean = true,
    val onValueChange: (String) -> Unit,
    val onFocusChange: (Boolean) -> Unit,
)

internal data class ClickableFieldUM(
    val label: TextReference,
    val value: TextReference,
    val onClick: () -> Unit,
)
