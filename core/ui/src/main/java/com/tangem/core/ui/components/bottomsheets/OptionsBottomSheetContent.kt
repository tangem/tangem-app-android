package com.tangem.core.ui.components.bottomsheets

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @param key Unique identifier for the option
 * @param label Display text for the option
 */
data class BottomSheetOption(
    val key: String,
    val label: TextReference,
)

/**
 * @param options List of options to display
 * @param onOptionClick Callback when an option is clicked, receives the option key
 */
data class OptionsBottomSheetContent(
    val options: ImmutableList<BottomSheetOption> = persistentListOf(),
    val onOptionClick: (String) -> Unit = {},
) : TangemBottomSheetConfigContent