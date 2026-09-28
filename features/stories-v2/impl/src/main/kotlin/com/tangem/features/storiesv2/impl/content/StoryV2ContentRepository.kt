package com.tangem.features.storiesv2.impl.content

import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ContentSource
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2Load
import com.tangem.features.storiesv2.impl.content.remote.StoryV2Registry
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** What resolving a story came to. */
internal sealed interface StoryV2ContentLoad {

    data class Ready(val composition: StoryV2Composition) : StoryV2ContentLoad

    /** Nothing showable, which the host treats as "continue without the story". */
    data object Unavailable : StoryV2ContentLoad

    /** Delivery failed — the network or the files — and trying again may succeed. */
    data object Failed : StoryV2ContentLoad
}

/**
 * Resolves a story type to a composition the player can show right now. The split between bundled and remote lives
 * here and nowhere else: what comes out is always a composition whose assets exist on this device.
 */
internal interface StoryV2ContentRepository {

    suspend fun load(type: StoryV2Type): StoryV2ContentLoad

    /** What a `once` display policy counts. */
    suspend fun markShown(type: StoryV2Type)

    /** Forgets every remote response and file, so the tester showcase can replay a cold start. */
    suspend fun clearRemoteContent()
}

@Singleton
internal class DefaultStoryV2ContentRepository @Inject constructor(
    private val bundledSource: BundledStoryV2ContentSource,
    private val remoteSource: RemoteStoryV2ContentSource,
    private val previewContent: StoryV2PreviewContent,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : StoryV2ContentRepository {

    /**
     * One load per story and language, shared: a prefetch and a tap on the same story wait for the same work instead
     * of racing. It runs on the application scope, so a player closed mid-load still leaves the files for next time.
     */
    private val loads = ConcurrentHashMap<LoadKey, Deferred<StoryV2ContentLoad>>()

    /**
     * Bundled content resolves in place, without the background load: it is on the device already, and the player,
     * started on the main-immediate dispatcher, then has the story before its first frame instead of a loading one.
     */
    private val bundledOnboarding by lazy { bundledSource.onboarding().toLoad() }

    override suspend fun load(type: StoryV2Type): StoryV2ContentLoad {
        previewContent.composition?.let { return it.toLoad() }
        if (type == StoryV2Type.ONBOARDING) return bundledOnboarding

        // Without the Unicode extensions: the system appends regional preferences such as `-u-fw-mon-mu-celsius`,
        // which the backend rejects and which say nothing about the language of the texts anyway.
        val key = LoadKey(type = type, language = Locale.getDefault().stripExtensions().toLanguageTag())
        val load = loads.computeIfAbsent(key) { appScope.async(dispatchers.io) { resolve(key) } }
        val result = runSuspendCatching { load.await() }.getOrElse { error ->
            TangemLogger.e("Stories V2: failed to load $type", error)
            StoryV2ContentLoad.Failed
        }

        // Only a ready story is worth keeping; anything else is tried again by whoever asks next.
        if (result !is StoryV2ContentLoad.Ready) loads.remove(key, load)
        return result
    }

    /** A recorded `once` showing makes the cached story stale: the next open has to learn it is gone. */
    override suspend fun markShown(type: StoryV2Type) {
        val slug = StoryV2Registry.slugOf(type) ?: return
        if (remoteSource.markShown(slug)) loads.keys.removeAll { it.type == type }
    }

    override suspend fun clearRemoteContent() {
        loads.clear()
        remoteSource.clear()
    }

    private suspend fun resolve(key: LoadKey): StoryV2ContentLoad = when (key.type) {
        StoryV2Type.ONBOARDING -> bundledOnboarding
        StoryV2Type.HARDWARE_UPGRADE -> resolveRemote(key)
    }

    private suspend fun resolveRemote(key: LoadKey): StoryV2ContentLoad {
        val slug = StoryV2Registry.slugOf(key.type) ?: return StoryV2ContentLoad.Unavailable
        return when (val load = remoteSource.load(slug = slug, language = key.language)) {
            is RemoteStoryV2Load.Ready -> load.composition.toLoad()
            RemoteStoryV2Load.Unavailable -> StoryV2ContentLoad.Unavailable
            RemoteStoryV2Load.Failed -> StoryV2ContentLoad.Failed
        }
    }

    private fun StoryV2Composition.toLoad(): StoryV2ContentLoad {
        return StoryV2CompositionValidator.validate(this)?.let(StoryV2ContentLoad::Ready)
            ?: StoryV2ContentLoad.Unavailable
    }

    /** The language is part of the key: remote texts arrive translated, so a language change is a different story. */
    private data class LoadKey(val type: StoryV2Type, val language: String)
}