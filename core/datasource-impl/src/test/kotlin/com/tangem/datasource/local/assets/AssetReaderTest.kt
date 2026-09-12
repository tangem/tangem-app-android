package com.tangem.datasource.local.assets

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

internal class AssetReaderTest {

    private val assetManager = mockk<AssetManager>()
    private val context = mockk<Context> {
        every { assets } returns assetManager
    }
    private val assetReader = AssetReader(context)

    @Test
    fun read_content() = runTest {
        every { assetManager.open(FILE_NAME, AssetManager.ACCESS_BUFFER) } returns json.byteInputStream()

        val actual = assetReader.read(fullFileName = FILE_NAME)

        Truth.assertThat(actual).isEqualTo(json)
    }

    @Test
    fun read_error() = runTest {
        val exception = IOException("Error")
        every { assetManager.open(FILE_NAME, AssetManager.ACCESS_BUFFER) } throws exception

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