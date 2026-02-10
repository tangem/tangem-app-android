package com.tangem.core.ui.ds.row.header

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowUM
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for header row component
 *
 * @param id          Unique id
 * @param title       Title text reference
 * @param subtitle    Subtitle text reference (optional)
 * @param startIconUM Icon UI model (optional)
 * @param endIconRes  Icon UI model (optional)
 * @param isEnabled   Flag indicating if click is enabled
 * @param onItemClick Callback for item click (optional)
 */
@Immutable
data class TangemHeaderRowUM(
    override val id: String,
    val title: TextReference,
    val subtitle: TextReference? = null,
    val startIconUM: TangemIconUM? = null,
    @DrawableRes val endIconRes: Int? = null,
    val isEnabled: Boolean = false,
    val onItemClick: (() -> Unit)? = null,
) : TangemRowUM