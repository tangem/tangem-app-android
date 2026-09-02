@file:OptIn(ExperimentalStdlibApi::class)

package com.tangem.core.local.assets

import com.tangem.datasource.utils.listTypes
import com.tangem.datasource.utils.mapWithStringKeyTypes
import java.lang.reflect.Type
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Asset file loader
 *
 * @see <a href = "https://www.notion.so/tangem/Assets-e045dd890413413faf34ce07ae47ff56">Documentation</a>
 */
interface AssetLoader {

    /** Load content [T] of asset file [fileName] parsed with [type] */
    suspend fun <T> load(fileName: String, type: KType): T?

    /** Load list [T] values of asset file [fileName] parsed with [type] */
    suspend fun <T> loadList(fileName: String, type: Type): List<T>

    /** Load map [String] keys and [T] values of asset file [fileName] parsed with [type] */
    suspend fun <T> loadMap(fileName: String, type: Type): Map<String, T>
}

/** Load content [T] of asset file [fileName] */
suspend inline fun <reified T> AssetLoader.load(fileName: String): T? = load(fileName, typeOf<T>())

/** Load list [T] values of asset file [fileName] */
suspend inline fun <reified T> AssetLoader.loadList(fileName: String): List<T> = loadList(fileName, listTypes<T>())

/** Load map [String] keys and [T] values of asset file [fileName] */
suspend inline fun <reified T> AssetLoader.loadMap(fileName: String): Map<String, T> =
    loadMap(fileName, mapWithStringKeyTypes<T>())