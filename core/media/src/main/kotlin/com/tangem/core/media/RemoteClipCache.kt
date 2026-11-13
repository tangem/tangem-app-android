package com.tangem.core.media

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger

/** Where a remote clip is downloaded in full before it plays, so playback never buffers. */
@OptIn(UnstableApi::class)
interface RemoteClipCache {

    val dataSourceFactory: DataSource.Factory

    fun isFullyCached(url: String): Boolean

    /** Cancelled with the caller; throws on any failure. */
    suspend fun download(url: String)
}

/** Throws on any failure. */
internal suspend fun RemoteClipCache.downloadIfMissing(url: String) {
    if (!isFullyCached(url)) download(url)
}

/** `false` on a failure, which is logged. */
internal suspend fun RemoteClipCache.ensureCached(url: String, diagnosticsSubject: String): Boolean =
    runSuspendCatching { downloadIfMissing(url) }
        .onFailure { error -> TangemLogger.e("$diagnosticsSubject failed to download", error) }
        .isSuccess