package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.extensions.stringReference
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.features.storiesv2.impl.analytics.StoriesV2Events
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorStage
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import com.tangem.features.storiesv2.impl.content.StoryV2Origin
import com.tangem.features.storiesv2.impl.content.StoryV2Slide
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface RemoteStoryV2Load {

    data class Ready(val composition: StoryV2Composition) : RemoteStoryV2Load

    /** The backend has nothing for this slug. */
    data object NotFound : RemoteStoryV2Load

    /** A `once` story the viewer has already seen. */
    data object AlreadyShown : RemoteStoryV2Load

    /** Neither the network nor the device could produce the story; a retry may. */
    data object Failed : RemoteStoryV2Load
}

/**
 * Remote stories end to end: one request for every slug the release knows, the last good answer kept for when the
 * network is not there, then the files of the requested story, so what leaves is a composition like a bundled one.
 *
 * Failures are reported here, where the stage is known. The entry point is not, so they go out without a source.
 */
@Singleton
internal class RemoteStoryV2ContentSource @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val localStore: StoryV2LocalStore,
    private val assetStore: StoryV2AssetStore,
    private val analytics: AnalyticsEventHandler,
) {

    private val catalogLock = Mutex()

    /** Only a network answer is kept here: a fallback to the saved one must not stop the next open retrying. */
    private var catalog: Catalog? = null

    /** Policies of the stories last produced, from the network or the saved answer alike. */
    private val displayPolicies = ConcurrentHashMap<String, RemoteStoryV2.DisplayPolicy>()

    suspend fun load(slug: String, language: String): RemoteStoryV2Load {
        val stories = catalog(language) ?: return RemoteStoryV2Load.Failed
        val story = stories.firstOrNull { it.slug == slug } ?: return RemoteStoryV2Load.NotFound

        if (story.displayPolicy == RemoteStoryV2.DisplayPolicy.ONCE && slug in localStore.shownSlugs()) {
            return RemoteStoryV2Load.AlreadyShown
        }

        return download(story)?.let(RemoteStoryV2Load::Ready) ?: RemoteStoryV2Load.Failed
    }

    /** @return whether the showing was recorded, i.e. whether the story may not be shown again */
    suspend fun markShown(slug: String): Boolean {
        if (displayPolicies[slug] != RemoteStoryV2.DisplayPolicy.ONCE) return false
        localStore.markShown(slug)
        return true
    }

    suspend fun clear() = catalogLock.withLock {
        catalog = null
        displayPolicies.clear()
        localStore.clear()
        assetStore.clear()
    }

    private suspend fun catalog(language: String): List<RemoteStoryV2>? = catalogLock.withLock {
        catalog?.takeIf { it.language == language }?.let { return@withLock it.stories }

        val slugs = StoryV2Registry.slugs
        val fetched = fetch(slugs = slugs, language = language)
        if (fetched != null) catalog = Catalog(language = language, stories = fetched)

        (fetched ?: savedStories(slugs = slugs, language = language))
            ?.also { stories -> stories.forEach { displayPolicies[it.slug] = it.displayPolicy } }
    }

    private suspend fun fetch(slugs: List<String>, language: String): List<RemoteStoryV2>? {
        return when (val response = tangemTechApi.getStoriesV2(slugs = slugs, language = language)) {
            is ApiResponse.Success -> accept(body = response.data, slugs = slugs, language = language)
            is ApiResponse.Error -> {
                val (reason, httpCode) = response.cause.toErrorReason()
                reportError(stage = StoryV2ErrorStage.REQUEST, reason = reason, httpCode = httpCode)
                null
            }
        }
    }

    private suspend fun accept(body: StoriesV2Response, slugs: List<String>, language: String): List<RemoteStoryV2>? {
        val converted = when (val result = StoriesV2ResponseConverter(requestedSlugs = slugs.toSet()).convert(body)) {
            is RemoteStoryV2ConversionResult.Converted -> result
            is RemoteStoryV2ConversionResult.Rejected -> {
                reportError(stage = StoryV2ErrorStage.CONTENT_VALIDATION, reason = result.reason)
                return null
            }
        }
        converted.issues.forEach { issue ->
            reportError(
                stage = StoryV2ErrorStage.CONTENT_VALIDATION,
                reason = issue.reason,
                storyId = issue.storyId,
                slideId = issue.slideId,
            )
        }
        localStore.saveResponse(language = language, slugs = slugs, response = body)
        // Stories gone from a successful answer take their files with them; a failed request never does.
        val urls = converted.stories.flatMapTo(mutableSetOf()) { story -> story.slides.map { it.source.url } }
        assetStore.retainOnly(urls)

        return converted.stories
    }

    /** The saved answer stands in only for the very same question, so a language change never shows old texts. */
    private suspend fun savedStories(slugs: List<String>, language: String): List<RemoteStoryV2>? {
        val saved = localStore.savedResponse(language = language, slugs = slugs) ?: return null
        val result = StoriesV2ResponseConverter(requestedSlugs = slugs.toSet()).convert(saved)
        return (result as? RemoteStoryV2ConversionResult.Converted)?.stories
    }

    /**
     * Every file is fetched before the story opens, as on iOS. A slide whose file failed is dropped, except the
     * first: the story opens on it, so without it there is nothing to open.
     */
    private suspend fun download(story: RemoteStoryV2): StoryV2Composition? {
        val keep = story.slides.mapTo(mutableSetOf()) { it.source.url }
        val slides = story.slides.mapIndexedNotNull { index, slide ->
            when (val result = assetStore.resolve(file = slide.source, keep = keep)) {
                is StoryV2AssetResult.Ready -> slide.toSlide(result.file)
                is StoryV2AssetResult.Failed -> {
                    reportError(
                        stage = StoryV2ErrorStage.ASSET_PREFETCH,
                        reason = result.reason,
                        storyId = story.slug,
                        slideId = slide.id,
                        httpCode = result.httpCode,
                    )
                    if (index == 0) return null
                    null
                }
            }
        }

        return StoryV2Composition(
            id = story.slug,
            origin = StoryV2Origin.REMOTE,
            endBehavior = story.endBehavior,
            actions = story.actions,
            slides = slides,
        )
    }

    private fun RemoteStoryV2Slide.toSlide(file: File): StoryV2Slide {
        val source = StoryV2MediaRef.LocalFile(path = file.path)
        return StoryV2Slide(
            id = id,
            title = title.takeIf { it.isNotEmpty() }?.let(::stringReference),
            subtitle = subtitle.takeIf { it.isNotEmpty() }?.let(::stringReference),
            asset = when (assetType) {
                RemoteStoryV2Slide.AssetType.VIDEO -> StoryV2Asset.Video(
                    source = source,
                    poster = null,
                    durationMs = durationMs,
                    contentMode = contentMode,
                )
                RemoteStoryV2Slide.AssetType.LOTTIE -> StoryV2Asset.VectorAnimation(
                    source = source,
                    poster = null,
                    durationMs = durationMs,
                    contentMode = contentMode,
                )
                RemoteStoryV2Slide.AssetType.IMAGE -> StoryV2Asset.Image(
                    source = source,
                    durationMs = durationMs,
                    contentMode = contentMode,
                )
            },
            haptics = haptics,
        )
    }

    private fun reportError(
        stage: StoryV2ErrorStage,
        reason: StoryV2ErrorReason,
        storyId: String? = null,
        slideId: String? = null,
        httpCode: Int? = null,
    ) {
        analytics.send(
            StoriesV2Events.StoryErrorOccurred(
                storyId = storyId,
                slideId = slideId,
                errorStage = stage.analyticsValue,
                errorReason = reason.analyticsValue,
                source = null,
                httpCode = httpCode,
            ),
        )
    }

    private data class Catalog(val language: String, val stories: List<RemoteStoryV2>)
}

private fun ApiResponseError.toErrorReason(): Pair<StoryV2ErrorReason, Int?> = when (this) {
    is ApiResponseError.HttpException -> {
        val reason = if (code == ApiResponseError.HttpException.Code.NOT_FOUND) {
            StoryV2ErrorReason.NOT_FOUND
        } else {
            StoryV2ErrorReason.HTTP_FAILURE
        }
        reason to code.numericCode
    }
    is ApiResponseError.NetworkException -> StoryV2ErrorReason.NETWORK to null
    is ApiResponseError.TimeoutException -> StoryV2ErrorReason.TIMEOUT to null
    // A body whose types do not match the contract lands here; malformed JSON the network layer already reports as a
    // network error.
    is ApiResponseError.UnknownException -> StoryV2ErrorReason.DECODE_FAILURE to null
}