package com.tangem.features.storiesv2.impl.content.remote

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Fetches one file to [target]. Blocking I/O, so callers stay on the IO dispatcher. */
internal fun interface StoryV2FileDownloader {

    /** @throws IOException on any network or disk failure, [StoryV2HttpException] on a non-2xx answer */
    suspend fun download(url: String, target: File)
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
        .build()

    override suspend fun download(url: String, target: File) {
        val call = client.newCall(Request.Builder().url(url).build())
        // A closed player or a cleared cache cancels the coroutine; the socket must go with it.
        val cancellation = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw StoryV2HttpException(response.code)
                val body = response.body ?: throw IOException("Empty body")
                body.byteStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            }
        } finally {
            cancellation?.dispose()
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}