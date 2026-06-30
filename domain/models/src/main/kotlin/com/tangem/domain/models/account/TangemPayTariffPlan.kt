package com.tangem.domain.models.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class TangemPayTariffPlan(
    @SerialName("type") val type: Type,
    @SerialName("name") val name: String,
    @SerialName("description_items") val descriptionItems: List<DescriptionItem>,
) {
    @Serializable
    data class DescriptionItem(
        @SerialName("section") val section: Section,
        @SerialName("order") val order: Int,
        @SerialName("title") val title: String,
        @SerialName("body") val body: String,
    )

    @Serializable
    enum class Type {
        @SerialName("BASIC")
        BASIC,

        @SerialName("PLUS")
        PLUS,

        @SerialName("PLUS_FF")
        PLUS_FF,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "BASIC" -> BASIC
                "PLUS" -> PLUS
                "PLUS_FF" -> PLUS_FF
                else -> UNKNOWN
            }
        }
    }

    @Serializable
    enum class Section {
        @SerialName("CARD_RELATED")
        CARD_RELATED,

        @SerialName("PLAN_RELATED")
        PLAN_RELATED,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "CARD_RELATED" -> CARD_RELATED
                "PLAN_RELATED" -> PLAN_RELATED
                else -> UNKNOWN
            }
        }
    }
}