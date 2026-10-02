package com.tangem.features.storiesv2.impl.content.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where remote story files live on the device. Cache rather than files: nothing else removes them once a device stops
 * asking for the story, so the system may reclaim them when space runs low. A file gone that way is downloaded again on
 * the next open; the last response and the shown stories are kept in DataStore and stay.
 */
@Singleton
internal class StoryV2Storage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val root: File get() = File(context.cacheDir, ROOT_DIRECTORY)

    val assets: File get() = File(root, ASSETS_DIRECTORY)

    private companion object {
        const val ROOT_DIRECTORY = "stories_v2"
        const val ASSETS_DIRECTORY = "assets"
    }
}