package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface StoryV2AssetResult {

    data class Ready(val file: File) : StoryV2AssetResult

    data class Failed(val reason: StoryV2ErrorReason, val httpCode: Int? = null) : StoryV2AssetResult
}

/**
 * Downloaded story files, keyed by url: the backend never changes the bytes behind a published url, so a new file
 * always comes with a new url and an unchanged url is never fetched twice.
 */
internal interface StoryV2AssetStore {

    /**
     * @param keep urls of the story being resolved, which making room must never evict
     * @return the local file, downloaded first when it is not on the device yet
     */
    suspend fun resolve(file: RemoteStoryV2File, keep: Set<String>): StoryV2AssetResult

    /**
     * Deletes every file whose url is not in [urls]: the stories that left a successful response. A file resolved in
     * this process stays until the next start, since a player may still be about to open it.
     */
    suspend fun retainOnly(urls: Set<String>)

    suspend fun clear()
}

@Singleton
internal class DefaultStoryV2AssetStore @Inject constructor(
    private val storage: StoryV2Storage,
    private val downloader: StoryV2FileDownloader,
    private val dispatchers: CoroutineDispatcherProvider,
) : StoryV2AssetStore {

    /**
     * One lock per url: the prefetch and a tap on the same story wait for one download instead of racing, which
     * would otherwise let the second rename a file over the one the player already reads.
     */
    private val locks = ConcurrentHashMap<String, Mutex>()

    /**
     * Urls handed out in this process. The repository keeps their compositions until the process ends and a player
     * may open them at any moment, so neither cleanup nor eviction deletes them before a restart.
     */
    private val inUse: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override suspend fun resolve(file: RemoteStoryV2File, keep: Set<String>): StoryV2AssetResult {
        // Marked before the size check: a cleanup running meanwhile either spares the file or deletes it before the
        // check, which then downloads it again.
        inUse.add(file.url)
        return withContext(dispatchers.io) {
            locks.computeIfAbsent(file.url) { Mutex() }.withLock {
                val target = File(storage.assets, fileNameOf(file.url))
                if (target.length() == file.sizeBytes) {
                    // Touched on every use, so eviction goes by last use rather than by download date.
                    target.setLastModified(System.currentTimeMillis())
                    StoryV2AssetResult.Ready(target)
                } else {
                    target.delete()
                    download(file = file, target = target, keep = keep)
                }
            }
        }
    }

    override suspend fun retainOnly(urls: Set<String>) = withContext(dispatchers.io) {
        val names = protectedNames(urls)
        storedFiles().filterNot { it.name in names }.forEach(File::delete)
    }

    override suspend fun clear() {
        inUse.clear()
        withContext(dispatchers.io) { storage.assets.listFiles()?.forEach(File::delete) }
    }

    /** Out of space means one cleanup and one more attempt, never a retry loop. */
    private suspend fun download(file: RemoteStoryV2File, target: File, keep: Set<String>): StoryV2AssetResult {
        storage.assets.mkdirs()
        evictToFit(incomingBytes = file.sizeBytes, keep = keep)

        val first = write(file = file, target = target)
        if (first !is WriteResult.OutOfSpace) return first.toAssetResult(target)

        evictAll(keep = keep)
        return write(file = file, target = target).toAssetResult(target)
    }

    /** Ready only once fully written, matching the declared size and renamed into place. */
    private suspend fun write(file: RemoteStoryV2File, target: File): WriteResult {
        val part = File(target.parentFile, "${target.name}$PART_SUFFIX")
        return try {
            downloader.download(url = file.url, target = part)
            when {
                part.length() != file.sizeBytes -> {
                    part.delete()
                    WriteResult.Failed(StoryV2ErrorReason.INTEGRITY_MISMATCH)
                }
                part.renameTo(target) -> WriteResult.Written
                else -> {
                    part.delete()
                    WriteResult.Failed(StoryV2ErrorReason.INTEGRITY_MISMATCH)
                }
            }
        } catch (e: StoryV2HttpException) {
            part.delete()
            WriteResult.Failed(reason = StoryV2ErrorReason.HTTP_FAILURE, httpCode = e.code)
        } catch (e: IOException) {
            part.delete()
            if (e.isOutOfSpace()) {
                WriteResult.OutOfSpace
            } else {
                TangemLogger.e("Stories V2: failed to download ${file.url}", e)
                WriteResult.Failed(StoryV2ErrorReason.NETWORK)
            }
        }
    }

    private fun evictToFit(incomingBytes: Long, keep: Set<String>) {
        val keepNames = protectedNames(keep)
        val files = storedFiles()
        var total = files.sumOf(File::length)
        if (total + incomingBytes <= BUDGET_BYTES) return

        files.filterNot { it.name in keepNames }
            .sortedBy(File::lastModified)
            .forEach { file ->
                if (total + incomingBytes <= BUDGET_BYTES) return
                total -= file.length()
                file.delete()
            }
    }

    private fun evictAll(keep: Set<String>) {
        val keepNames = protectedNames(keep)
        storage.assets.listFiles()?.filterNot { it.name in keepNames }?.forEach(File::delete)
    }

    private fun protectedNames(urls: Set<String>): Set<String> {
        return (urls + inUse).mapTo(mutableSetOf(), ::fileNameOf)
    }

    /** Finished files only; a `.part` belongs to a download still in flight. */
    private fun storedFiles(): List<File> {
        return storage.assets.listFiles()?.filterNot { it.name.endsWith(PART_SUFFIX) }.orEmpty()
    }

    private fun WriteResult.toAssetResult(target: File): StoryV2AssetResult = when (this) {
        WriteResult.Written -> StoryV2AssetResult.Ready(target)
        // The taxonomy has no storage reason; a file that could not be written is a file that is not intact.
        WriteResult.OutOfSpace -> StoryV2AssetResult.Failed(StoryV2ErrorReason.INTEGRITY_MISMATCH)
        is WriteResult.Failed -> StoryV2AssetResult.Failed(reason = reason, httpCode = httpCode)
    }

    private sealed interface WriteResult {
        data object Written : WriteResult
        data object OutOfSpace : WriteResult
        data class Failed(val reason: StoryV2ErrorReason, val httpCode: Int? = null) : WriteResult
    }

    companion object {

        /** The same ceiling the iOS cache uses; four upgrade videos take about a twentieth of it. */
        const val BUDGET_BYTES = 200L * 1024 * 1024

        private const val PART_SUFFIX = ".part"
        private const val MAX_EXTENSION_LENGTH = 5

        /** A digest of the url, keeping the extension so the decoders can tell the format from the name. */
        fun fileNameOf(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(url.toByteArray())
                .joinToString(separator = "") { "%02x".format(it) }
            val extension = url.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "")
                .takeIf { it.isNotEmpty() && it.length <= MAX_EXTENSION_LENGTH && it.all(Char::isLetterOrDigit) }
            return if (extension != null) "$digest.$extension" else digest
        }
    }
}

private fun IOException.isOutOfSpace(): Boolean {
    val text = generateSequence<Throwable>(this) { it.cause }.mapNotNull { it.message }.joinToString()
    return text.contains("ENOSPC") || text.contains("No space left", ignoreCase = true)
}