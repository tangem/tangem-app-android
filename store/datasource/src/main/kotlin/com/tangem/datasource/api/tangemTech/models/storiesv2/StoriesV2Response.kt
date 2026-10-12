package com.tangem.datasource.api.tangemTech.models.storiesv2

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET api/v2/stories`: placement → the stories shown there, in display order. A placement with nothing to show is
 * absent.
 *
 * Fields the contract marks required are nullable here on purpose: the client drops the one story or slide that
 * misses them and keeps the rest of the answer. List elements are nullable for the same reason, and because Moshi
 * does not check the nullability of type arguments: a `null` element would otherwise reach the client as non-null.
 */
typealias StoriesV2Response = Map<String, List<StoryV2DTO?>?>

@JsonClass(generateAdapter = true)
data class StoryV2DTO(
    @Json(name = "slug")
    val slug: String?,
    @Json(name = "language")
    val language: String?,
    @Json(name = "priority")
    val priority: Int?,
    @Json(name = "displayPolicy")
    val displayPolicy: String?,
    @Json(name = "endBehaviour")
    val endBehaviour: String?,
    @Json(name = "slides")
    val slides: List<StoryV2SlideDTO?>?,
    @Json(name = "actions")
    val actions: List<StoryV2ActionDTO?>?,
)

/** @property id a string by contract; a number from the backend is read as its string form */
@JsonClass(generateAdapter = true)
data class StoryV2SlideDTO(
    @Json(name = "id")
    val id: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "subtitle")
    val subtitle: String?,
    @Json(name = "assetType")
    val assetType: String?,
    @Json(name = "contentMode")
    val contentMode: String?,
    @Json(name = "durationMs")
    val durationMs: Long?,
    @Json(name = "hapticAtMs")
    val hapticAtMs: List<Long?>?,
    @Json(name = "source")
    val source: StoryV2SourceDTO?,
)

@JsonClass(generateAdapter = true)
data class StoryV2SourceDTO(
    @Json(name = "url")
    val url: String?,
    @Json(name = "mimetype")
    val mimetype: String?,
    @Json(name = "codec")
    val codec: String?,
    @Json(name = "sizeBytes")
    val sizeBytes: Long?,
)

@JsonClass(generateAdapter = true)
data class StoryV2ActionDTO(
    @Json(name = "id")
    val id: String?,
    @Json(name = "label")
    val label: String?,
    @Json(name = "style")
    val style: String?,
    @Json(name = "deeplink")
    val deeplink: String?,
)