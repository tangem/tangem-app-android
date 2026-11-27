package com.tangem.features.tangempay.common

import androidx.annotation.DrawableRes
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference

internal data class TangemPayDropDownItemUM(
    val onClick: () -> Unit,
    val title: TextReference,
    val icon: TangemIconUM,
    @DrawableRes val iconRes: Int? = null,
    val subtitle: TextReference? = null,
    val isEnabled: Boolean = true,
    val titleColor: ColorReference2? = null,
    val subtitleColor: ColorReference2? = null,
)