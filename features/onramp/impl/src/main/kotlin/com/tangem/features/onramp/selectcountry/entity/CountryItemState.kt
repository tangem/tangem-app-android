package com.tangem.features.onramp.selectcountry.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface CountryItemState {
    /** Unique id */
    val id: String

    data class Loading(override val id: String) : CountryItemState

    sealed interface WithContent : CountryItemState {
        data class Unavailable(
            override val id: String,
            val flagUrl: String,
            val countryName: String,
        ) : WithContent

        data class Content(
            override val id: String,
            val flagUrl: String,
            val countryName: String,
            val isSelected: Boolean,
            val onClick: () -> Unit,
        ) : WithContent
    }
}