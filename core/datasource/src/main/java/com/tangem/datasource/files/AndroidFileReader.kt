package com.tangem.datasource.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidFileReader @Inject constructor(@ApplicationContext private val context: Context) : FileReader {

    override fun readFile(fileName: String): String {
        return context.openFileInput(fileName).use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    override fun rewriteFile(content: String, fileName: String) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { stream ->
            stream.write(content.toByteArray(), 0, content.length)
        }
    }
}