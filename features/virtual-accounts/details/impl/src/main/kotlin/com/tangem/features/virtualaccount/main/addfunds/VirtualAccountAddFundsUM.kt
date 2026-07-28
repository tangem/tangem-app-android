package com.tangem.features.virtualaccount.main.addfunds

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class VirtualAccountAddFundsUM(
    val onDismiss: () -> Unit,
    val content: Content,
) {

    @Immutable
    sealed interface Content {

        data class Intro(
            val onShowDetailsClick: () -> Unit,
        ) : Content

        data class Details(
            val items: ImmutableList<DetailItem>,
            val dailyLimit: String,
            val onShareClick: () -> Unit,
        ) : Content
    }

    @Immutable
    data class DetailItem(
        val label: TextReference,
        val value: String,
        val onCopyClick: () -> Unit,
    )
}