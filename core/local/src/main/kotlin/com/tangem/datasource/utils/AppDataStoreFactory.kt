package com.tangem.datasource.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

/**
 * Creates [DataStore]s that recover from on-disk corruption instead of crashing, regardless of the
 * [Serializer] backing them.
 *
 * The concrete implementation is internal and supplied via DI.
 */
interface AppDataStoreFactory {

    /**
     * Whether stores move from the Moshi serializer to kotlinx. While off, migrated call sites keep their Moshi
     * serializer and the fileName [create] keeps the default kotlinx config. The gated Moshi stores use compatible
     * JSON in both modes; switching fileName stores back off may lose values that rely on an omitted nullable field.
     */
    @RemoveWithToggle("AND_18288_KOTLINX_DATASTORE_ENABLED")
    val isKotlinxDataStoreEnabled: Boolean

    /**
     * Creates a [DataStore] backed by [serializer], running on [scope] and persisting to the file returned by
     * [produceFile]. If the persisted file is corrupted it is replaced with [Serializer.defaultValue] instead of
     * crashing.
     *
     * @param serializer serializer that encodes/decodes the stored value and provides the fallback default.
     * @param scope coroutine scope the store runs its work on; should outlive every reader (typically the app scope).

     * @param migrations run once before the first read or write, e.g. to drop the file of an earlier format.
     */
    fun <T> create(
        serializer: Serializer<T>,
        scope: CoroutineScope,
        produceFile: () -> File,
        migrations: List<DataMigration<T>> = emptyList(),
    ): DataStore<T>

    /**
     * Persists [T] with kotlinx-serialization in the DataStore file named [fileName]. Runs on the application scope,
     * so callers pass neither a scope nor a file. While [isKotlinxDataStoreEnabled], uses the network JSON config:
     * `null` fields are not written and a missing nullable field reads as `null`, matching files written by the Moshi
     * DataStore serializer.
     *
     * Prefer the reified [create] extension, which derives the [serializer] for `@Serializable` types.
     *
     * @param defaultValue value returned before anything is written and after a corrupted file is discarded.
     * @param serializer kotlinx [KSerializer] for [T]; for `@Serializable` types use `T.serializer()`.
     * @param fileName name of the DataStore file, resolved under the app's DataStore directory.
     */
    fun <T> create(defaultValue: T, serializer: KSerializer<T>, fileName: String): DataStore<T>
}

/**
 * Convenience for `@Serializable` [T]: derives the [KSerializer] via reified [serializer], so callers pass only the
 * default value and [fileName].
 */
inline fun <reified T> AppDataStoreFactory.create(defaultValue: T, fileName: String): DataStore<T> =
    create(defaultValue = defaultValue, serializer = serializer(), fileName = fileName)