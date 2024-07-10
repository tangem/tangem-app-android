package com.tangem.core.ui.components.showcase.model

import com.tangem.core.ui.extensions.TextReference

data class ShowcaseButtonModel(
    val buttonText: TextReference,
    val onClick: () -> Unit,
)