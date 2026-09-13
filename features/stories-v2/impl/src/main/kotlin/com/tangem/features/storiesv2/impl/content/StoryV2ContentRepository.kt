package com.tangem.features.storiesv2.impl.content

import com.tangem.features.storiesv2.StoryV2Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a story type to a composition the player can show right now. The split between bundled and remote lives
 * here and nowhere else: what comes out is always a composition whose assets exist on this device.
 */
internal interface StoryV2ContentRepository {

    /** @return `null` when there is nothing showable, which the host must treat as "continue without the story". */
    suspend fun getComposition(type: StoryV2Type): StoryV2Composition?
}

@Singleton
internal class DefaultStoryV2ContentRepository @Inject constructor(
    private val bundledSource: BundledStoryV2ContentSource,
    private val previewContent: StoryV2PreviewContent,
) : StoryV2ContentRepository {

    /** Bounded by the number of story types, which is why it needs no eviction policy. */
    private val resolved = ConcurrentHashMap<StoryV2Type, StoryV2Composition>()

    override suspend fun getComposition(type: StoryV2Type): StoryV2Composition? {
        previewContent.composition?.let { return StoryV2CompositionValidator.validate(it) }

        resolved[type]?.let { return it }

        val composition = when (type) {
            StoryV2Type.ONBOARDING -> bundledSource.onboarding()
            // Remote delivery resolves here, into the same composition shape: the player already treats a
            // downloaded file exactly like a bundled resource, so it needs no change to gain it.
            StoryV2Type.HARDWARE_UPGRADE -> null
        } ?: return null

        return StoryV2CompositionValidator.validate(composition)?.also { resolved[type] = it }
    }
}