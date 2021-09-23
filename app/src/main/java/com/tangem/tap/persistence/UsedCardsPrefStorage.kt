package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.extensions.replaceBy
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class UsedCardsPrefStorage(
    private val preferences: SharedPreferences,
) {

    private val jsonConverter = MoshiJsonConverter.INSTANCE

    internal fun migrate() {
        val scannedIds = preferences.getString(SCANNED_CARDS_IDS_KEY, null) ?: return

        val usedCardsInfo = scannedIds.split(",").map { UsedCardInfo(it.trim(), true) }
        if (save(usedCardsInfo.toMutableList())) {
            preferences.edit { this.remove(SCANNED_CARDS_IDS_KEY) }
        }
    }

    fun scanned(cardId: String) {
        val restoredList = restore()
        val foundItem = findCardInfo(cardId, restoredList)?.copy(isScanned = true)
                ?: UsedCardInfo(cardId, true)

        save(foundItem, restoredList)
    }

    fun wasScanned(cardId: String): Boolean {
        return findCardInfo(cardId)?.isScanned ?: false
    }

    fun activationStarted(cardId: String) {
        val restoredList = restore()
        val foundItem = findCardInfo(cardId, restoredList)?.copy(isActivationStarted = true)
                ?: UsedCardInfo(cardId, isActivationStarted = true)

        save(foundItem, restoredList)
    }

    fun activationIsStarted(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivationStarted ?: false
    }

    fun activated(cardId: String) {
        val restoredList = restore()
        val foundItem = findCardInfo(cardId, restoredList)?.copy(isActivated = true)
                ?: UsedCardInfo(cardId, isActivated = true)

        save(foundItem, restoredList)
    }

    fun wasActivated(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivated ?: false
    }

    private fun findCardInfo(cardId: String, list: MutableList<UsedCardInfo>? = null): UsedCardInfo? {
        val findInList = list ?: restore()
        return findInList.firstOrNull { it.cardId == cardId }
    }

    private fun save(usedCardInfo: UsedCardInfo?, usedCardsInfo: MutableList<UsedCardInfo>) {
        val info = usedCardInfo ?: return

        usedCardsInfo.replaceBy(info) { it.cardId == info.cardId }
        save(usedCardsInfo)
    }

    private fun save(list: MutableList<UsedCardInfo>): Boolean {
        return try {
            val json = jsonConverter.toJson(list)
            preferences.edit { putString(USED_CARDS_INFO, json) }
            true
        } catch (ex: Exception) {
            Timber.e(ex)
            false
        }
    }

    private fun restore(): MutableList<UsedCardInfo> {
        val json = preferences.getString(USED_CARDS_INFO, null) ?: return mutableListOf()
        return try {
            jsonConverter.fromJson(json)!!
        } catch (ex: Exception) {
            preferences.edit(true) { remove(USED_CARDS_INFO) }
            mutableListOf()
        }
    }
    
    companion object {
        private const val USED_CARDS_INFO = "usedCardsInfo"
        private const val SCANNED_CARDS_IDS_KEY = "scannedCardIds"
    }
}

private data class UsedCardInfo(
    val cardId: String,
    val isScanned: Boolean = false,
    val isActivationStarted: Boolean = false,
    val isActivated: Boolean = false,
)