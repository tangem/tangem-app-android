package com.tangem.features.hotwallet.viewphrase.entity

import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ViewPhraseUM(
    val onBackClick: () -> Unit,
    val words: ImmutableList<EnumeratedTwoColumnGridItem> = persistentListOf(),
)