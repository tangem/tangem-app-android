package com.tangem.core.ui.ds.row

import androidx.compose.runtime.Immutable

/**
 * Base interface for all row UI models in the Tangem application. Each row UI model must implement this interface
 */
@Immutable
interface TangemRowUM {

    val id: String
}