package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.core.ui.extensions.stringReference
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2ActionDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2DTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SlideDTO
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2SourceDTO
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ConversionResult.Issue
import com.tangem.utils.converter.Converter

/**
 * Validates a `/v2/stories` response against the contract and keeps whatever survives.
 *
 * Where the contract and the iOS mapper disagree, the contract wins: an unrequested slug rejects the whole response,
 * a missing or duplicate slide id drops the story, and a button set that breaks the rules is hidden as a whole.
 */
internal class StoriesV2ResponseConverter(
    private val requestedSlugs: Set<String>,
) : Converter<StoriesV2Response, RemoteStoryV2ConversionResult> {

    override fun convert(value: StoriesV2Response): RemoteStoryV2ConversionResult {
        val hasUnrequestedSlug = value.values.any { stories ->
            stories.orEmpty().any { story -> story?.validSlug()?.let { it !in requestedSlugs } == true }
        }
        if (hasUnrequestedSlug) return RemoteStoryV2ConversionResult.Rejected(StoryV2ErrorReason.UNKNOWN_SCHEMA)

        val issues = mutableListOf<Issue>()
        val bySlug = linkedMapOf<String, Entry>()
        value.forEach { (placement, stories) ->
            stories?.forEach { story ->
                collect(story = story, placement = placement, bySlug = bySlug, issues = issues)
            }
        }

        val stories = bySlug.values.mapNotNull { entry ->
            val story = entry.story ?: return@mapNotNull null
            if (entry.knownPlacements.isEmpty()) {
                issues += Issue(storyId = story.slug, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_PLACEMENT)
                return@mapNotNull null
            }
            story.copy(placements = entry.knownPlacements)
        }

        return RemoteStoryV2ConversionResult.Converted(stories = stories, issues = issues.toList())
    }

    /**
     * The same story arrives in full under every placement it belongs to. The copies are identical by contract, so
     * the first one decides and the rest only add their placements.
     */
    private fun collect(
        story: StoryV2DTO?,
        placement: String,
        bySlug: MutableMap<String, Entry>,
        issues: MutableList<Issue>,
    ) {
        val slug = story?.validSlug()
        if (story == null || slug == null) {
            issues += Issue(storyId = null, slideId = null, reason = StoryV2ErrorReason.UNKNOWN_SCHEMA)
            return
        }

        val entry = bySlug.getOrPut(slug) { Entry() }
        if (placement in StoryV2Registry.placements) entry.knownPlacements += placement
        if (!entry.isConverted) {
            entry.isConverted = true
            entry.story = StoryConverter(slug = slug, issues = issues).convert(story)
        }
    }

    private class Entry {
        var story: RemoteStoryV2? = null
        var isConverted: Boolean = false
        val knownPlacements = linkedSetOf<String>()
    }
}

/** One story; what it drops on the way is recorded in [issues]. */
private class StoryConverter(private val slug: String, private val issues: MutableList<Issue>) {

    fun convert(story: StoryV2DTO): RemoteStoryV2? {
        val displayPolicy = when (story.displayPolicy) {
            "always" -> RemoteStoryV2.DisplayPolicy.ALWAYS
            "once" -> RemoteStoryV2.DisplayPolicy.ONCE
            else -> return drop(StoryV2ErrorReason.UNKNOWN_POLICY)
        }
        val endBehavior = when (story.endBehaviour) {
            "finish" -> StoryV2EndBehavior.FINISH
            "loop" -> StoryV2EndBehavior.LOOP
            else -> return drop(StoryV2ErrorReason.UNKNOWN_POLICY)
        }

        val slides = story.slides ?: return drop(StoryV2ErrorReason.UNKNOWN_SCHEMA)
        // A missing slide is a slide without an id, which the contract treats as a broken story.
        val ids = slides.map { it?.id?.takeIf(String::isNotBlank) }
        if (ids.any { it == null } || ids.distinct().size != ids.size) return drop(StoryV2ErrorReason.UNKNOWN_SCHEMA)

        val converted = slides.filterNotNull().mapNotNull(::convertSlide)
        if (converted.isEmpty()) return drop(StoryV2ErrorReason.UNKNOWN_ASSET)

        return RemoteStoryV2(
            slug = slug,
            placements = emptySet(),
            displayPolicy = displayPolicy,
            endBehavior = endBehavior,
            actions = convertActions(story.actions.orEmpty()),
            slides = converted,
        )
    }

    private fun convertSlide(slide: StoryV2SlideDTO): RemoteStoryV2Slide? {
        val id = requireNotNull(slide.id)
        val assetType = when (slide.assetType) {
            "video" -> RemoteStoryV2Slide.AssetType.VIDEO
            "lottie" -> RemoteStoryV2Slide.AssetType.LOTTIE
            "image" -> RemoteStoryV2Slide.AssetType.IMAGE
            else -> return dropSlide(id, StoryV2ErrorReason.UNKNOWN_ASSET)
        }
        val contentMode = when (slide.contentMode) {
            "cover" -> StoryV2ContentMode.COVER
            "contain" -> StoryV2ContentMode.CONTAIN
            else -> return dropSlide(id, StoryV2ErrorReason.UNKNOWN_SCHEMA)
        }
        val title = slide.title ?: return dropSlide(id, StoryV2ErrorReason.UNKNOWN_SCHEMA)
        val subtitle = slide.subtitle ?: return dropSlide(id, StoryV2ErrorReason.UNKNOWN_SCHEMA)
        val durationMs = slide.durationMs?.takeIf { it > 0 } ?: return dropSlide(id, StoryV2ErrorReason.UNKNOWN_SCHEMA)
        val source = convertSource(slide.source) ?: return dropSlide(id, StoryV2ErrorReason.NO_COMPATIBLE_SOURCE)

        return RemoteStoryV2Slide(
            id = id,
            title = title,
            subtitle = subtitle,
            assetType = assetType,
            contentMode = contentMode,
            durationMs = durationMs,
            haptics = convertHaptics(
                marks = slide.hapticAtMs.orEmpty().filterNotNull(),
                assetType = assetType,
                durationMs = durationMs,
            ),
            source = source,
        )
    }

    private fun convertSource(source: StoryV2SourceDTO?): RemoteStoryV2File? {
        source ?: return null
        val url = source.url?.takeIf(HTTPS_FILE_URL::matches) ?: return null
        val sizeBytes = source.sizeBytes?.takeIf { it > 0 } ?: return null
        source.mimetype?.takeIf { it.isNotBlank() } ?: return null

        return RemoteStoryV2File(url = url, sizeBytes = sizeBytes)
    }

    /** A mark outside the slide costs only itself; the slide keeps playing without the vibration it cannot place. */
    private fun convertHaptics(marks: List<Long>, assetType: RemoteStoryV2Slide.AssetType, durationMs: Long) =
        if (assetType == RemoteStoryV2Slide.AssetType.IMAGE) {
            emptyList()
        } else {
            StoryV2HapticClusterizer.cluster(marks.filter { it in 0 until durationMs })
        }

    /** More than two buttons or a repeated style is a broken set: hide all of it, the close button still works. */
    private fun convertActions(actions: List<StoryV2ActionDTO?>): List<StoryV2Action> {
        val styles = actions.mapNotNull { it?.style }
        if (actions.size > MAX_ACTIONS || styles.distinct().size != styles.size) {
            drop(StoryV2ErrorReason.UNKNOWN_SCHEMA)
            return emptyList()
        }

        return actions.mapIndexedNotNull { index, action ->
            if (action == null) return@mapIndexedNotNull dropAction(StoryV2ErrorReason.UNKNOWN_SCHEMA)
            convertAction(action = action, fallbackId = "${slug}_action_${index + 1}")
        }
    }

    private fun convertAction(action: StoryV2ActionDTO, fallbackId: String): StoryV2Action? {
        val label = action.label?.takeIf { it.isNotBlank() } ?: return dropAction(StoryV2ErrorReason.UNKNOWN_SCHEMA)
        val style = when (action.style) {
            "primary" -> StoryV2Action.Style.PRIMARY
            "secondary" -> StoryV2Action.Style.SECONDARY
            else -> return dropAction(StoryV2ErrorReason.UNKNOWN_SCHEMA)
        }
        val deeplink = action.deeplink ?: return dropAction(StoryV2ErrorReason.UNKNOWN_TARGET)

        val target = when {
            deeplink.startsWith(DEEPLINK_SCHEME) -> {
                val host = deeplink.removePrefix(DEEPLINK_SCHEME).takeWhile { it !in URL_DELIMITERS }
                if (host !in StoryV2Registry.deeplinkHosts) return dropAction(StoryV2ErrorReason.UNKNOWN_TARGET)
                StoryV2Action.Target.Deeplink(uri = deeplink)
            }
            deeplink.startsWith(HTTPS_SCHEME) -> {
                // The whole authority is compared, so a port or user info never matches an allowed host.
                val authority = deeplink.removePrefix(HTTPS_SCHEME).takeWhile { it !in URL_DELIMITERS }
                if (authority !in StoryV2Registry.webHosts) return dropAction(StoryV2ErrorReason.FORBIDDEN_TARGET)
                StoryV2Action.Target.Web(url = deeplink)
            }
            else -> return dropAction(StoryV2ErrorReason.UNKNOWN_TARGET)
        }

        return StoryV2Action(
            id = action.id?.takeIf { it.isNotBlank() } ?: fallbackId,
            label = stringReference(label),
            style = style,
            target = target,
        )
    }

    private fun drop(reason: StoryV2ErrorReason): RemoteStoryV2? {
        issues += Issue(storyId = slug, slideId = null, reason = reason)
        return null
    }

    private fun dropSlide(slideId: String, reason: StoryV2ErrorReason): RemoteStoryV2Slide? {
        issues += Issue(storyId = slug, slideId = slideId, reason = reason)
        return null
    }

    private fun dropAction(reason: StoryV2ErrorReason): StoryV2Action? {
        issues += Issue(storyId = slug, slideId = null, reason = reason)
        return null
    }

    private companion object {
        const val MAX_ACTIONS = 2
        const val DEEPLINK_SCHEME = "tangem://"
        const val HTTPS_SCHEME = "https://"
        val URL_DELIMITERS = charArrayOf('/', '?', '#')
        val HTTPS_FILE_URL = Regex("^https://[^\\s/@?#]+/[^\\s?#]+$")
    }
}

private fun StoryV2DTO.validSlug(): String? = slug?.takeIf { it.isNotBlank() }