package com.tangem.features.storiesv2.impl.content.remote

import androidx.datastore.core.DataStore
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoryV2DTO
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What remote stories keep across restarts: the last response that went through, so a story still opens offline or
 * while the backend answers 5xx, and the `once` stories already shown.
 */
@Singleton
internal class StoryV2LocalStore @Inject constructor(
    dataStoreFactory: AppDataStoreFactory,
    @NetworkMoshi moshi: Moshi,
) {

    private val dataStore: DataStore<StoryV2LocalState> =
        dataStoreFactory.create(defaultValue = StoryV2LocalState(), fileName = FILE_NAME)

    /** The same typed adapter the network response goes through, so a saved answer is read by the same rules. */
    private val responseAdapter = moshi.adapter<StoriesV2Response>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(List::class.java, StoryV2DTO::class.java),
        ),
    )

    /** Only an answer to the very same question counts, so a language change never shows the old texts. */
    suspend fun savedResponse(language: String, slugs: List<String>): StoriesV2Response? {
        val saved = dataStore.data.first().response ?: return null
        if (saved.language != language || saved.slugs != slugs) return null

        return try {
            responseAdapter.fromJson(saved.json)
        } catch (e: JsonDataException) {
            TangemLogger.e("Stories V2: saved response is unreadable", e)
            null
        } catch (e: JsonEncodingException) {
            TangemLogger.e("Stories V2: saved response is unreadable", e)
            null
        } catch (e: IOException) {
            TangemLogger.e("Stories V2: saved response is unreadable", e)
            null
        }
    }

    suspend fun saveResponse(language: String, slugs: List<String>, response: StoriesV2Response) {
        val saved = StoryV2LocalState.SavedResponse(
            language = language,
            slugs = slugs,
            json = responseAdapter.toJson(response),
        )
        dataStore.updateData { it.copy(response = saved) }
    }

    suspend fun shownSlugs(): Set<String> = dataStore.data.first().shownSlugs

    suspend fun markShown(slug: String) {
        dataStore.updateData { it.copy(shownSlugs = it.shownSlugs + slug) }
    }

    suspend fun clear() {
        dataStore.updateData { StoryV2LocalState() }
    }

    private companion object {
        const val FILE_NAME = "stories_v2_state"
    }
}

/**
 * @property response the response is kept as the backend sent it, not as the stories built from it: the converter,
 *                    not the file, decides what is valid, so a later release rereads an old answer by its own rules
 */
@Serializable
internal data class StoryV2LocalState(
    val response: SavedResponse? = null,
    val shownSlugs: Set<String> = emptySet(),
) {

    /** @property slugs what was asked for; an answer to a different question is no fallback for this one */
    @Serializable
    data class SavedResponse(val language: String, val slugs: List<String>, val json: String)
}