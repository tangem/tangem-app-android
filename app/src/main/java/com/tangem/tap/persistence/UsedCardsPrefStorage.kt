package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.extensions.replaceByOrAdd
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 17/09/2021.
 */
class UsedCardsPrefStorage(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val migrationList = mutableListOf(
        UserCardInfoToV2(this),
    )

    internal fun migrate() {
        migrationList.forEach { it.migrate() }
        migrationList.clear()
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

    fun activationFinished(cardId: String) {
        val restoredList = restore()
        var foundItem = findCardInfo(cardId, restoredList) ?: UsedCardInfo(cardId)
        foundItem = foundItem.copy(
            isActivationStarted = true,
            isActivationFinished = true,
        )

        save(foundItem, restoredList)
    }

    fun isActivationStarted(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivationStarted ?: false
    }

    fun isActivationFinished(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivationFinished ?: false
    }

    fun isActivationInProgress(cardId: String): Boolean {
        val cardInfo = findCardInfo(cardId) ?: return false
        return cardInfo.isActivationStarted && !cardInfo.isActivationFinished
    }

    private fun findCardInfo(cardId: String, list: MutableList<UsedCardInfo>? = null): UsedCardInfo? {
        val findInList = list ?: restore()
        return findInList.firstOrNull { it.cardId == cardId }
    }

    private fun save(usedCardInfo: UsedCardInfo?, usedCardsInfo: MutableList<UsedCardInfo>) {
        val info = usedCardInfo ?: return

        usedCardsInfo.replaceByOrAdd(info) { it.cardId == info.cardId }
        save(usedCardsInfo)
    }

    private fun save(list: MutableList<UsedCardInfo>): Boolean {
        return try {
            val json = jsonConverter.toJson(list)
            preferences.edit { putString(USED_CARDS_INFO_V2, json) }
            true
        } catch (ex: Exception) {
            Timber.e(ex)
            false
        }
    }

    private fun restore(): MutableList<UsedCardInfo> {
        val json = preferences.getString(USED_CARDS_INFO_V2, null) ?: return mutableListOf()
        return try {
            jsonConverter.fromJson(json, jsonConverter.typedList(UsedCardInfo::class.java))!!
        } catch (ex: Exception) {
            preferences.edit(true) { remove(USED_CARDS_INFO_V2) }
            mutableListOf()
        }
    }

    companion object {
        private const val USED_CARDS_INFO_V2 = "usedCardsInfo_v2"
        private const val USED_CARDS_INFO = "usedCardsInfo"
    }

    private class UserCardInfoToV2(
        private val storage: UsedCardsPrefStorage,
    ) : Migration {
        override fun migrate() {
            val restoredCardsInfo = restore()
            if (restoredCardsInfo.isEmpty()) return

            val newCardsInfo = restoredCardsInfo.map { cardInfo ->
                UsedCardInfo(
                    cardId = cardInfo.cardId,
                    isScanned = cardInfo.isScanned,
                    isActivationStarted = true,
                    isActivationFinished = !cardInfo.isActivationStarted,
                )
            }.toMutableList()
            storage.save(newCardsInfo)
        }

        private fun restore(): MutableList<UsedCardInfoOld> {
            val json = storage.preferences.getString(USED_CARDS_INFO, null) ?: return mutableListOf()
            return try {
                storage.jsonConverter.fromJson(json, storage.jsonConverter.typedList(UsedCardInfoOld::class.java))!!
            } catch (ex: Exception) {
                mutableListOf()
            } finally {
                storage.preferences.edit(true) { remove(USED_CARDS_INFO) }
            }
        }
    }

    private data class UsedCardInfo(
        val cardId: String,
        val isScanned: Boolean = false,
        val isActivationStarted: Boolean = false,
        val isActivationFinished: Boolean = false,
    )

    private data class UsedCardInfoOld(
        val cardId: String,
        val isScanned: Boolean = false,
        val isActivationStarted: Boolean = false,
    )
}

private interface Migration {
    fun migrate()
}
