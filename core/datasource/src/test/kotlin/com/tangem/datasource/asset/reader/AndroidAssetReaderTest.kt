package com.tangem.datasource.asset.reader

import android.content.res.AssetManager
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
[REDACTED_AUTHOR]
 */
internal class AndroidAssetReaderTest {

    private val assetManager = mockk<AssetManager>()
    private val assetReader = AndroidAssetReader(assetManager)

    @Test
    fun read_content() = runTest {
        every { assetManager.open(FILE_NAME) } returns json.byteInputStream()

        val actual = assetReader.read(fullFileName = FILE_NAME)

        Truth.assertThat(actual).isEqualTo(json)
    }

    @Test
    fun read_error() = runTest {
        val exception = IOException("Error")
        every { assetManager.open(FILE_NAME) } throws exception

        runCatching { assetReader.read(fullFileName = FILE_NAME) }
            .onSuccess { throw IllegalStateException("Error should be thrown") }
            .onFailure {
                Truth.assertThat(it).isEqualTo(exception)
            }
    }

    private companion object {

        const val FILE_NAME = "file.json"

        val json = """
            {
                "key": "value"
            }
        """.trimIndent()
    }
}