package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStoryV2AssetStoreTest {

    private val storage: StoryV2Storage = mockk()
    private val downloader = FakeStoryV2FileDownloader()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private lateinit var assetsDir: File
    private lateinit var store: DefaultStoryV2AssetStore

    @BeforeEach
    fun setUp(@TempDir tempDir: File) {
        clearMocks(storage)
        downloader.reset()
        assetsDir = File(tempDir, "assets")
        every { storage.root } returns tempDir
        every { storage.assets } returns assetsDir
        store = DefaultStoryV2AssetStore(storage = storage, downloader = downloader, dispatchers = dispatchers)
    }

    @Test
    fun `GIVEN a file not on the device WHEN resolve THEN it is downloaded and ready`() = runTest {
        // Arrange
        downloader.respond(writes(SIZE_BYTES))

        // Act
        val actual = store.resolve(file = FILE, keep = setOf(URL))

        // Assert
        assertThat(actual).isEqualTo(StoryV2AssetResult.Ready(targetOf(URL)))
        assertThat(storedFiles()).containsExactly(nameOf(URL) to SIZE_BYTES)
    }

    @Test
    fun `GIVEN the file already on the device with the declared size WHEN resolve THEN nothing is downloaded`() =
        runTest {
            // Arrange
            putOnDevice(url = URL, sizeBytes = SIZE_BYTES)
            downloader.respond(writes(SIZE_BYTES))

            // Act
            val actual = store.resolve(file = FILE, keep = setOf(URL))

            // Assert
            assertThat(actual).isEqualTo(StoryV2AssetResult.Ready(targetOf(URL)))
            assertThat(downloader.calls).isEqualTo(0)
        }

    @Test
    fun `GIVEN a stale file of another size WHEN resolve THEN it is downloaded again`() = runTest {
        // Arrange
        putOnDevice(url = URL, sizeBytes = SIZE_BYTES - 1)
        downloader.respond(writes(SIZE_BYTES))

        // Act
        val actual = store.resolve(file = FILE, keep = setOf(URL))

        // Assert
        assertThat(actual).isEqualTo(StoryV2AssetResult.Ready(targetOf(URL)))
        assertThat(storedFiles()).containsExactly(nameOf(URL) to SIZE_BYTES)
    }

    @Test
    fun `GIVEN a download of the wrong size WHEN resolve THEN it fails as integrity mismatch and leaves no file`() =
        runTest {
            // Arrange
            downloader.respond(writes(SIZE_BYTES - 1))

            // Act
            val actual = store.resolve(file = FILE, keep = setOf(URL))

            // Assert
            assertThat(actual).isEqualTo(StoryV2AssetResult.Failed(StoryV2ErrorReason.INTEGRITY_MISMATCH))
            assertThat(storedFiles()).isEmpty()
        }

    @Test
    fun `GIVEN an HTTP error WHEN resolve THEN it fails with the status code and leaves no file`() = runTest {
        // Arrange
        downloader.respond(fails(StoryV2HttpException(code = 404)))

        // Act
        val actual = store.resolve(file = FILE, keep = setOf(URL))

        // Assert
        assertThat(actual).isEqualTo(
            StoryV2AssetResult.Failed(reason = StoryV2ErrorReason.HTTP_FAILURE, httpCode = 404),
        )
        assertThat(storedFiles()).isEmpty()
    }

    @Test
    fun `GIVEN a network error WHEN resolve THEN it fails without a retry`() = runTest {
        // Arrange
        downloader.respond(fails(IOException("unexpected end of stream")))

        // Act
        val actual = store.resolve(file = FILE, keep = setOf(URL))

        // Assert
        assertThat(actual).isEqualTo(StoryV2AssetResult.Failed(StoryV2ErrorReason.NETWORK))
        assertThat(downloader.calls).isEqualTo(1)
    }

    @Test
    fun `GIVEN the disk runs out of space once WHEN resolve THEN other files are evicted and the retry succeeds`() =
        runTest {
            // Arrange
            putOnDevice(url = OTHER_URL, sizeBytes = SIZE_BYTES)
            putOnDevice(url = KEEP_URL, sizeBytes = SIZE_BYTES)
            downloader.respond(fails(outOfSpace()), writes(SIZE_BYTES))

            // Act
            val actual = store.resolve(file = FILE, keep = setOf(URL, KEEP_URL))

            // Assert
            assertThat(actual).isEqualTo(StoryV2AssetResult.Ready(targetOf(URL)))
            assertThat(storedFiles()).containsExactly(nameOf(KEEP_URL) to SIZE_BYTES, nameOf(URL) to SIZE_BYTES)
        }

    @Test
    fun `GIVEN the disk stays out of space WHEN resolve THEN it fails after a single retry`() = runTest {
        // Arrange
        downloader.respond(fails(outOfSpace()))

        // Act
        val actual = store.resolve(file = FILE, keep = setOf(URL))

        // Assert
        assertThat(actual).isEqualTo(StoryV2AssetResult.Failed(StoryV2ErrorReason.INTEGRITY_MISMATCH))
        assertThat(downloader.calls).isEqualTo(2)
    }

    @Test
    fun `GIVEN stored files WHEN retainOnly THEN only files of other urls are deleted`() = runTest {
        // Arrange
        putOnDevice(url = URL, sizeBytes = SIZE_BYTES)
        putOnDevice(url = KEEP_URL, sizeBytes = SIZE_BYTES)
        putOnDevice(url = OTHER_URL, sizeBytes = SIZE_BYTES)
        // A download still in flight is not a stored file yet.
        val inFlight = File(assetsDir, "${nameOf(OTHER_URL)}.part").apply { writeBytes(ByteArray(size = 1)) }

        // Act
        store.retainOnly(urls = setOf(URL, KEEP_URL))

        // Assert
        assertThat(storedFiles()).containsExactly(
            nameOf(URL) to SIZE_BYTES,
            nameOf(KEEP_URL) to SIZE_BYTES,
            inFlight.name to 1L,
        )
    }

    @Test
    fun `GIVEN a file resolved in this process WHEN retainOnly without its url THEN the file stays`() = runTest {
        // Arrange
        putOnDevice(url = URL, sizeBytes = SIZE_BYTES)
        putOnDevice(url = OTHER_URL, sizeBytes = SIZE_BYTES)
        store.resolve(file = FILE, keep = setOf(URL))

        // Act
        store.retainOnly(urls = emptySet())

        // Assert
        assertThat(storedFiles()).containsExactly(nameOf(URL) to SIZE_BYTES)
    }

    @Test
    fun `GIVEN a file resolved in this process WHEN another story runs out of space THEN the file is not evicted`() =
        runTest {
            // Arrange
            putOnDevice(url = URL, sizeBytes = SIZE_BYTES)
            putOnDevice(url = OTHER_URL, sizeBytes = SIZE_BYTES)
            store.resolve(file = FILE, keep = setOf(URL))
            downloader.respond(fails(outOfSpace()), writes(SIZE_BYTES))

            // Act
            val actual = store.resolve(file = KEEP_FILE, keep = setOf(KEEP_URL))

            // Assert
            assertThat(actual).isEqualTo(StoryV2AssetResult.Ready(targetOf(KEEP_URL)))
            assertThat(storedFiles()).containsExactly(nameOf(URL) to SIZE_BYTES, nameOf(KEEP_URL) to SIZE_BYTES)
        }

    @Test
    fun `GIVEN a resolved file and then a clear WHEN retainOnly without its url THEN the file is deleted`() = runTest {
        // Arrange
        putOnDevice(url = URL, sizeBytes = SIZE_BYTES)
        store.resolve(file = FILE, keep = setOf(URL))
        store.clear()
        putOnDevice(url = URL, sizeBytes = SIZE_BYTES)

        // Act
        store.retainOnly(urls = emptySet())

        // Assert
        assertThat(storedFiles()).isEmpty()
    }

    @Test
    fun `GIVEN two resolves of one url at once WHEN the download completes THEN the file is downloaded once`() =
        runTest {
            // Arrange
            val gate = CompletableDeferred<Unit>()
            downloader.respond(
                { target ->
                    gate.await()
                    target.writeBytes(ByteArray(SIZE_BYTES.toInt()))
                },
            )
            val first = async { store.resolve(file = FILE, keep = setOf(URL)) }
            val second = async { store.resolve(file = FILE, keep = setOf(URL)) }
            runCurrent()

            // Act
            gate.complete(Unit)
            val actual = awaitAll(first, second)

            // Assert
            val ready = StoryV2AssetResult.Ready(targetOf(URL))
            assertThat(actual).containsExactly(ready, ready)
            assertThat(downloader.calls).isEqualTo(1)
        }

    @Test
    fun `GIVEN the same url WHEN fileNameOf THEN the name is the same every time`() {
        // Act
        val names = List(size = 3) { DefaultStoryV2AssetStore.fileNameOf(URL) }

        // Assert
        assertThat(names.distinct()).containsExactly(DefaultStoryV2AssetStore.fileNameOf(URL))
    }

    @Test
    fun `GIVEN an mp4 url WHEN fileNameOf THEN the digest keeps the mp4 extension`() {
        // Act
        val actual = DefaultStoryV2AssetStore.fileNameOf(URL)

        // Assert
        assertThat(actual).matches("[0-9a-f]{64}\\.mp4")
    }

    private fun putOnDevice(url: String, sizeBytes: Long) {
        assetsDir.mkdirs()
        targetOf(url).writeBytes(ByteArray(sizeBytes.toInt()))
    }

    private fun storedFiles(): List<Pair<String, Long>> {
        return assetsDir.listFiles().orEmpty().map { it.name to it.length() }
    }

    private fun targetOf(url: String) = File(assetsDir, nameOf(url))

    private fun nameOf(url: String) = DefaultStoryV2AssetStore.fileNameOf(url)

    private fun writes(bytes: Long): suspend (File) -> Unit = { target -> target.writeBytes(ByteArray(bytes.toInt())) }

    private fun fails(error: IOException): suspend (File) -> Unit = { throw error }

    private fun outOfSpace() = IOException("write failed: ENOSPC (No space left on device)")

    /** Plays one step per attempt; the last step repeats for every attempt after it. */
    private class FakeStoryV2FileDownloader : StoryV2FileDownloader {

        var calls = 0
            private set

        private var steps: List<suspend (File) -> Unit> = emptyList()

        fun reset() {
            calls = 0
            steps = emptyList()
        }

        fun respond(vararg steps: suspend (File) -> Unit) {
            this.steps = steps.toList()
        }

        override suspend fun download(url: String, target: File) {
            val step = steps[minOf(calls, steps.lastIndex)]
            calls++
            step(target)
        }
    }

    private companion object {
        const val URL = "https://cdn.tangem.com/stories/slide_1.mp4"
        const val KEEP_URL = "https://cdn.tangem.com/stories/slide_2.mp4"
        const val OTHER_URL = "https://cdn.tangem.com/stories/other.mp4"
        const val SIZE_BYTES = 2_048L
        val FILE = RemoteStoryV2File(url = URL, sizeBytes = SIZE_BYTES)
        val KEEP_FILE = RemoteStoryV2File(url = KEEP_URL, sizeBytes = SIZE_BYTES)
    }
}