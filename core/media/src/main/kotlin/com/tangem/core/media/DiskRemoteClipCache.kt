package com.tangem.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InterruptedIOException

/**
 * Clips downloaded in full into a media3 cache of its own, kept in the app cache directory.
 *
 * A directory is locked by one [SimpleCache], so every instance needs a [dirName] of its own.
 * Create one instance per `dirName` per process and keep it for the process lifetime: a second instance on the same
 * directory throws at construction, and nothing releases the cache.
 */
@OptIn(UnstableApi::class)
class DiskRemoteClipCache(
    context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
    dirName: String,
    maxBytes: Long,
) : RemoteClipCache {

    private val appContext = context.applicationContext
    private val cache = SimpleCache(
        File(appContext.cacheDir, dirName),
        LeastRecentlyUsedCacheEvictor(maxBytes),
        StandaloneDatabaseProvider(appContext),
    )

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext))

    override val dataSourceFactory: DataSource.Factory = cacheDataSourceFactory

    override suspend fun download(url: String) {
        val writer = CacheWriter(
            cacheDataSourceFactory.createDataSourceForDownloading(),
            DataSpec.Builder().setUri(url).build(),
            /* temporaryBuffer = */ null,
            /* progressListener = */ null,
        )

        withContext(dispatchers.io) {
            // invokeOnCompletion fires only once the blocking cache() has returned, i.e. after the whole download.
            // UNDISPATCHED: a cancellation right after the launch must not skip the child's body.
            val canceller = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    writer.cancel()
                }
            }
            try {
                writer.cache()
            } catch (e: InterruptedIOException) {
                // The canceller stopped the writer: the caller's cancellation, not a failed download.
                ensureActive()
                throw e
            } finally {
                canceller.cancel()
            }
        }
    }

    override fun isFullyCached(url: String): Boolean {
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(url))
        if (length == C.LENGTH_UNSET.toLong()) return false

        return cache.isCached(url, 0L, length)
    }
}