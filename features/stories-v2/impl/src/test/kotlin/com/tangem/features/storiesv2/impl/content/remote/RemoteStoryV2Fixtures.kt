package com.tangem.features.storiesv2.impl.content.remote

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2ActionDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2DTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SlideDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SourceDTO
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2Haptic

internal const val SLUG = "hardware_upgrade"
internal const val MAIN_PLACEMENT = "main_upgrade"
internal const val SETTINGS_PLACEMENT = "wallet_settings_upgrade"
internal const val HARDWARE_WALLET_DEEPLINK = "tangem://hardware_wallet"
internal const val SLIDE_DURATION_MS = 5_000L
internal const val SLIDE_SIZE_BYTES = 1_024L
internal const val DEV_RESPONSE_PATH = "stories_v2/hardware_upgrade_dev.json"

/** Decodes responses with the generated DTO adapters, the way the network layer and the local store read them. */
internal object StoryV2Json {

    val moshi: Moshi = Moshi.Builder().build()

    private val responseAdapter: JsonAdapter<StoriesV2Response> = moshi.adapter(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(List::class.java, StoryV2DTO::class.java),
        ),
    )

    fun decode(json: String): StoriesV2Response = requireNotNull(responseAdapter.fromJson(json))

    fun resource(path: String): String {
        return requireNotNull(javaClass.classLoader?.getResource(path)) { "Missing test resource $path" }.readText()
    }
}

internal fun slideUrl(id: String?): String = "https://cdn.tangem.com/stories/slide_$id.mp4"

internal fun storyDto(
    slug: String? = SLUG,
    language: String? = null,
    priority: Int? = null,
    displayPolicy: String? = "always",
    endBehaviour: String? = "loop",
    slides: List<StoryV2SlideDTO?>? = listOf(slideDto()),
    actions: List<StoryV2ActionDTO?>? = null,
) = StoryV2DTO(
    slug = slug,
    language = language,
    priority = priority,
    displayPolicy = displayPolicy,
    endBehaviour = endBehaviour,
    slides = slides,
    actions = actions,
)

internal fun slideDto(
    id: String? = "1",
    title: String? = "Title",
    subtitle: String? = "Subtitle",
    assetType: String? = "video",
    contentMode: String? = "contain",
    durationMs: Long? = SLIDE_DURATION_MS,
    hapticAtMs: List<Long?>? = null,
    source: StoryV2SourceDTO? = sourceDto(url = slideUrl(id)),
) = StoryV2SlideDTO(
    id = id,
    title = title,
    subtitle = subtitle,
    assetType = assetType,
    contentMode = contentMode,
    durationMs = durationMs,
    hapticAtMs = hapticAtMs,
    source = source,
)

internal fun sourceDto(
    url: String? = slideUrl(id = "1"),
    mimetype: String? = "video/mp4",
    codec: String? = "h264",
    sizeBytes: Long? = SLIDE_SIZE_BYTES,
) = StoryV2SourceDTO(url = url, mimetype = mimetype, codec = codec, sizeBytes = sizeBytes)

internal fun actionDto(
    id: String? = null,
    label: String? = "Get Tangem",
    style: String? = "primary",
    deeplink: String? = HARDWARE_WALLET_DEEPLINK,
) = StoryV2ActionDTO(id = id, label = label, style = style, deeplink = deeplink)

internal fun remoteStory(
    slug: String = SLUG,
    placements: Set<String> = setOf(MAIN_PLACEMENT),
    displayPolicy: RemoteStoryV2.DisplayPolicy = RemoteStoryV2.DisplayPolicy.ALWAYS,
    endBehavior: StoryV2EndBehavior = StoryV2EndBehavior.LOOP,
    actions: List<StoryV2Action> = emptyList(),
    slides: List<RemoteStoryV2Slide> = listOf(remoteSlide()),
) = RemoteStoryV2(
    slug = slug,
    placements = placements,
    displayPolicy = displayPolicy,
    endBehavior = endBehavior,
    actions = actions,
    slides = slides,
)

internal fun remoteSlide(
    id: String = "1",
    title: String = "Title",
    subtitle: String = "Subtitle",
    assetType: RemoteStoryV2Slide.AssetType = RemoteStoryV2Slide.AssetType.VIDEO,
    contentMode: StoryV2ContentMode = StoryV2ContentMode.CONTAIN,
    durationMs: Long = SLIDE_DURATION_MS,
    haptics: List<StoryV2Haptic> = emptyList(),
    source: RemoteStoryV2File = RemoteStoryV2File(url = slideUrl(id), sizeBytes = SLIDE_SIZE_BYTES),
) = RemoteStoryV2Slide(
    id = id,
    title = title,
    subtitle = subtitle,
    assetType = assetType,
    contentMode = contentMode,
    durationMs = durationMs,
    haptics = haptics,
    source = source,
)

internal fun storyAction(
    id: String = "${SLUG}_action_1",
    label: String = "Get Tangem",
    style: StoryV2Action.Style = StoryV2Action.Style.PRIMARY,
    target: StoryV2Action.Target = StoryV2Action.Target.Deeplink(uri = HARDWARE_WALLET_DEEPLINK),
) = StoryV2Action(id = id, label = stringReference(label), style = style, target = target)

internal fun tick(atMs: Long, pulses: Int = 1, pulseIntervalMs: Long = 0L) = StoryV2Haptic(
    atMs = atMs,
    effect = TangemHapticEffect.OneTime.Tick,
    pulses = pulses,
    pulseIntervalMs = pulseIntervalMs,
)