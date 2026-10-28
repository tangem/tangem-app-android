package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter

internal class CollectiblesRelinkConverter(
    private val onRelinkClick: () -> Unit,
) : Converter<CollectiblesRelinkConverter.Input, CollectiblesMainUM.RelinkUM> {

    override fun convert(value: Input): CollectiblesMainUM.RelinkUM {
        return if (value.record is CollectiblesAccountStatusValue.UpdateRequired) {
            CollectiblesMainUM.RelinkUM.Shown(isLoading = value.isLoading, onRelinkClick = onRelinkClick)
        } else {
            CollectiblesMainUM.RelinkUM.Hidden
        }
    }

    data class Input(val record: CollectiblesAccountStatusValue?, val isLoading: Boolean)
}