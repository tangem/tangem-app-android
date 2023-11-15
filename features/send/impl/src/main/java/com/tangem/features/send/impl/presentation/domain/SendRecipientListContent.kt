package com.tangem.features.send.impl.presentation.domain

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.PersistentList

@Immutable
internal sealed class SendRecipientListContent {
    data class Item(
        val id: String,
        val title: TextReference,
        val subtitle: TextReference,
        val info: TextReference? = null,
        @DrawableRes val subtitleIconRes: Int? = null,
    ) : SendRecipientListContent()

    data class Wallets(
        val list: PersistentList<Item>,
    ) : SendRecipientListContent()
}
