package com.tangem.features.storiesv2.impl.content.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

/** Fetches one file to [target]. */
internal fun interface StoryV2FileDownloader {

    /**
     * Reads at most one byte past [expectedBytes]: enough for the caller to see that the body is longer than declared,
     * never enough to fill the disk.
     *
     * @throws IOException on any network or disk failure, [StoryV2HttpException] on a non-2xx answer
     */
    suspend fun download(url: String, target: File, expectedBytes: Long)
}

internal class StoryV2HttpException(val code: Int) : IOException("HTTP $code")

/**
 * A plain client of its own, the files sit in a public bucket, so none of the gateway's
 * headers, keys or pinning apply, and the bodies are too large to log.
 */
@Singleton
internal class OkHttpStoryV2FileDownloader @Inject constructor() : StoryV2FileDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // Sources are accepted as https only; a redirect must not quietly downgrade one to plain http.
        .followSslRedirects(false)
        .build()

    override suspend fun download(url: String, target: File, expectedBytes: Long) {
        val call = client.newCall(Request.Builder().url(url).build())
        suspendCancellableCoroutine { continuation ->
            // Cancelling the call closes the socket, so a closed player or a cleared cache stops the transfer where
            // it is rather than after its last byte.
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = runCatching { response.use { save(it, target, expectedBytes) } }
                        continuation.resumeWith(result)
                    }
                },
            )
        }
    }

    private fun save(response: Response, target: File, expectedBytes: Long) {
        if (!response.isSuccessful) throw StoryV2HttpException(response.code)
        val body = response.body ?: throw IOException("Empty body")
        body.byteStream().use { input ->
            target.outputStream().use { output -> input.copyAtMost(output, limit = expectedBytes + 1) }
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}

internal fun InputStream.copyAtMost(output: OutputStream, limit: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (copied < limit) {
        val read = read(buffer, 0, minOf(buffer.size.toLong(), limit - copied).toInt())
        if (read == -1) return
        output.write(buffer, 0, read)
        copied += read
    }
}