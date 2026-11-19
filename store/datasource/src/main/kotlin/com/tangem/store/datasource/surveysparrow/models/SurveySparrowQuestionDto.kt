package com.tangem.store.datasource.surveysparrow.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** All but the id and the type are optional: the vendor's schema leaves most fields out. */
@JsonClass(generateAdapter = true)
data class SurveySparrowQuestionDto(
    @Json(name = "id")
    val id: Long,
    @Json(name = "type")
    val type: String,
    @Json(name = "rtxt")
    val text: String? = null,
    @Json(name = "is_required")
    val isRequired: Boolean? = null,
    @Json(name = "has_display_logic")
    val hasDisplayLogic: Boolean? = null,
    @Json(name = "position")
    val position: String? = null,
    @Json(name = "section")
    val section: Section? = null,
    @Json(name = "properties")
    val properties: Properties? = null,
) {

    @JsonClass(generateAdapter = true)
    data class Section(
        @Json(name = "position")
        val position: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Properties(
        @Json(name = "data")
        val data: Data? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "start")
        val start: Int? = null,
        @Json(name = "step")
        val step: Int? = null,
    )
}