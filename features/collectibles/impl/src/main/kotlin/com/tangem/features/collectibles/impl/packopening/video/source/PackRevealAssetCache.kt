package com.tangem.features.collectibles.impl.packopening.video.source

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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads reveal media in full into a shared media3 cache, so playback never buffers. */
@OptIn(UnstableApi::class)
@Singleton
internal class PackRevealAssetCache @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val appContext = context.applicationContext
    private val cache = SimpleCache(
        File(appContext.cacheDir, CACHE_DIR_NAME),
        LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
        StandaloneDatabaseProvider(appContext),
    )

    val dataSourceFactory: DataSource.Factory
        field = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext))

    /** Caches [url] in full; cancelled with the caller, throws on any network or storage failure. */
    suspend fun download(url: String) {
        val writer = CacheWriter(
            dataSourceFactory.createDataSourceForDownloading(),
            DataSpec.Builder().setUri(url).build(),
            /* temporaryBuffer = */ null,
            /* progressListener = */ null,
        )

        withContext(dispatchers.io) {
            val cancellation = coroutineContext.job.invokeOnCompletion { writer.cancel() }
            try {
                writer.cache()
            } finally {
                cancellation.dispose()
            }
        }
    }

    fun isFullyCached(url: String): Boolean {
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(url))
        if (length == C.LENGTH_UNSET.toLong()) return false

        return cache.isCached(url, 0L, length)
    }

    companion object {

        private const val CACHE_DIR_NAME = "pack-reveal"
        private const val CACHE_MAX_BYTES = 256L * 1024L * 1024L
    }
}