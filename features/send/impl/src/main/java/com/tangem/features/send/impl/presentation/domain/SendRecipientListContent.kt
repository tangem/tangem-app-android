package com.tangem.features.send.impl.presentation.domain

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

data class SendRecipientListContent(
    val id: String,
    val title: TextReference = TextReference.EMPTY,
    val subtitle: TextReference = TextReference.EMPTY,
    val timestamp: TextReference? = null,
    val subtitleEndOffset: Int = 0,
    @DrawableRes val subtitleIconRes: Int? = null,
    val isVisible: Boolean = true,
    val isLoading: Boolean = false,
)