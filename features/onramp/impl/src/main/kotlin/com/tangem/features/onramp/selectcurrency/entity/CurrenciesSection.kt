package com.tangem.features.onramp.selectcurrency.entity

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class CurrenciesSection<T : CurrencyItemState>(val title: TextReference, val items: ImmutableList<T>)