package com.tangem.feature.tester.presentation.addresses.state

import androidx.annotation.StringRes

/**
 * Content state of addresses info screen
 *
 * @property title               title
 * @property addressesText       addresses in text format
 * @property addressesJson       addresses in JSON format
 * @property onBackClick         the lambda to be invoked when back button is pressed
 */
internal data class AddressesInfoScreenUM(
    @StringRes val title: Int,
    val addressesText: String,
    val addressesJson: String,
    val onBackClick: () -> Unit,
    val onCopyClick: (CopyType) -> Unit,
)

internal enum class CopyType {
    TEXT,
    JSON,
}