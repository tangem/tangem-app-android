package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class TangemPayTariffPlan(
    @SerialName("id") val id: String,
    @SerialName("tier_id") val tierId: String,
    @SerialName("is_basic_tier") val isBasicTier: Boolean,
    @SerialName("name") val name: String,
    @SerialName("program_name") val programName: String,
    @SerialName("description_items") val descriptionItems: List<DescriptionItem>,
    @SerialName("images") val images: List<Image> = emptyList(),
    @SerialName("fees") val fees: List<Fee> = emptyList(),
) {
    @Serializable
    data class Fee(
        @SerialName("type") val type: Type,
        @SerialName("amount") val amount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
        @SerialName("description") val description: String,
        @SerialName("period") val period: Period?,
    ) {
        @Serializable
        enum class Type {
            @SerialName("FREE")
            FREE,

            @SerialName("RECURRING")
            RECURRING,

            @SerialName("UNKNOWN")
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                    "FREE" -> FREE
                    "RECURRING" -> RECURRING
                    else -> UNKNOWN
                }
            }
        }

        @Serializable
        enum class Period {
            @SerialName("MONTH")
            MONTH,

            @SerialName("UNKNOWN")
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                    "MONTH" -> MONTH
                    else -> UNKNOWN
                }
            }
        }
    }

    @Serializable
    data class DescriptionItem(
        @SerialName("section") val section: Section,
        @SerialName("order") val order: Int,
        @SerialName("title") val title: String,
        @SerialName("body") val body: String,
    )

    @Serializable
    data class Image(
        @SerialName("type") val type: Type,
        @SerialName("url") val url: String,
    ) {
        @Serializable
        enum class Type {
            @SerialName("THUMBNAIL")
            THUMBNAIL,

            @SerialName("MAIN")
            MAIN,

            @SerialName("BANNER")
            BANNER,

            @SerialName("BACKGROUND")
            BACKGROUND,

            @SerialName("UNKNOWN")
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                    "THUMBNAIL" -> THUMBNAIL
                    "MAIN" -> MAIN
                    "BANNER" -> BANNER
                    "BACKGROUND" -> BACKGROUND
                    else -> UNKNOWN
                }
            }
        }
    }

    @Serializable
    enum class Section {
        @SerialName("CARD_RELATED")
        CARD_RELATED,

        @SerialName("PLAN_RELATED")
        PLAN_RELATED,

        @SerialName("ONBOARDING_RELATED")
        ONBOARDING_RELATED,

        @SerialName("UNKNOWN")
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
                "CARD_RELATED" -> CARD_RELATED
                "PLAN_RELATED" -> PLAN_RELATED
                "ONBOARDING_RELATED" -> ONBOARDING_RELATED
                else -> UNKNOWN
            }
        }
    }
}

fun TangemPayTariffPlan.feeCurrencyOrDefault(defaultCurrencyCode: String = "USD"): String {
    return fees.firstOrNull()?.currency ?: defaultCurrencyCode
}