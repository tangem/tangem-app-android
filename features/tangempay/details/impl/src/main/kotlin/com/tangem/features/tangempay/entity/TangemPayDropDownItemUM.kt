package com.tangem.features.tangempay.entity

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference

internal data class TangemPayDropDownItemUM(
    val onClick: () -> Unit,
    val title: TextReference,
    val icon: TangemIconUM,
    val subtitle: TextReference? = null,
    val isEnabled: Boolean = true,
    val titleColor: ColorReference2? = null,
)