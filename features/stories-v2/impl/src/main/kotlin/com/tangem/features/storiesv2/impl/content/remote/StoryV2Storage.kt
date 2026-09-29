package com.tangem.features.storiesv2.impl.content.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where remote stories live on the device. Files rather than cache: the contract wants downloaded assets and the last
 * response to survive a restart, and the system must not clear a file from under the player.
 */
@Singleton
internal class StoryV2Storage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val root: File get() = File(context.filesDir, ROOT_DIRECTORY)

    val assets: File get() = File(root, ASSETS_DIRECTORY)

    private companion object {
        const val ROOT_DIRECTORY = "stories_v2"
        const val ASSETS_DIRECTORY = "assets"
    }
}