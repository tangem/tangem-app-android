package com.tangem.data.wallets.hot

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Attempts
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Companion.ATTEMPTS_BEFORE_DELETION
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Companion.COOLDOWN_SECONDS
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Companion.MAX_ATTEMPTS_BEFORE_DELETION
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository.Companion.MAX_FAST_FORWARD_ATTEMPTS
import com.tangem.hot.sdk.model.HotWalletId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("MagicNumber")
class DefaultHotWalletAccessCodeAttemptsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesStore: AppPreferencesStore,
) : HotWalletAccessCodeAttemptsRepository {

    override suspend fun incrementAttempts(id: HotWalletAccessCodeAttemptsRepository.AttemptId) {
        val attemptsKey = PreferencesKeys.getHotWalletUnlockAttemptsKey(id.attemptIdKey())

        appPreferencesStore.editData { preferences ->
            val currentAttempts = preferences[attemptsKey] ?: 0
            val newAttempts = currentAttempts + 1

            preferences[attemptsKey] = newAttempts
            val currentBootCount = currentBootCount()
            preferences[PreferencesKeys.getHotWalletUnlockBootKey(id.attemptIdKey())] = currentBootCount

            if (newAttempts >= MAX_FAST_FORWARD_ATTEMPTS) {
                val currentDeadline = SystemClock.elapsedRealtime() + COOLDOWN_SECONDS * 1000
                preferences[PreferencesKeys.getHotWalletUnlockDeadlineKey(id.attemptIdKey())] = currentDeadline
            }
        }
    }

    override suspend fun resetAttempts(hotWalletId: HotWalletId) {
        val authAttemptId = HotWalletAccessCodeAttemptsRepository.AttemptId(
            hotWalletId = hotWalletId,
            auth = true,
        )
        val noAuthAttemptId = HotWalletAccessCodeAttemptsRepository.AttemptId(
            hotWalletId = hotWalletId,
            auth = false,
        )

        appPreferencesStore.editData {
            it.remove(PreferencesKeys.getHotWalletUnlockAttemptsKey(authAttemptId.attemptIdKey()))
            it.remove(PreferencesKeys.getHotWalletUnlockAttemptsKey(noAuthAttemptId.attemptIdKey()))
            it.remove(PreferencesKeys.getHotWalletUnlockBootKey(authAttemptId.attemptIdKey()))
            it.remove(PreferencesKeys.getHotWalletUnlockBootKey(noAuthAttemptId.attemptIdKey()))
            it.remove(PreferencesKeys.getHotWalletUnlockDeadlineKey(authAttemptId.attemptIdKey()))
            it.remove(PreferencesKeys.getHotWalletUnlockDeadlineKey(noAuthAttemptId.attemptIdKey()))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAttempts(id: HotWalletAccessCodeAttemptsRepository.AttemptId): Flow<Attempts> {
        val flow = appPreferencesStore.data.map {
            AttemptsPersistentData(
                attempts = it[PreferencesKeys.getHotWalletUnlockAttemptsKey(id.attemptIdKey())] ?: 0,
                bootCount = it[PreferencesKeys.getHotWalletUnlockBootKey(id.attemptIdKey())] ?: 0,
                deadline = it[PreferencesKeys.getHotWalletUnlockDeadlineKey(id.attemptIdKey())] ?: 0L,
            )
        }.distinctUntilChanged()

        return flow.transformLatest {
            while (true) {
                emit(toState(id, it.attempts, it.deadline, it.bootCount))
                val remaining = remainingSeconds(it.deadline, it.bootCount)
                if (remaining <= 0) break
                delay(timeMillis = 1000)
            }
        }.distinctUntilChanged()
    }

    override suspend fun getAttemptsSync(id: HotWalletAccessCodeAttemptsRepository.AttemptId): Attempts {
        val prefs = appPreferencesStore.data.first()
        val count = prefs[PreferencesKeys.getHotWalletUnlockAttemptsKey(id.attemptIdKey())] ?: 0
        val boot = prefs[PreferencesKeys.getHotWalletUnlockBootKey(id.attemptIdKey())] ?: 0
        val deadline = prefs[PreferencesKeys.getHotWalletUnlockDeadlineKey(id.attemptIdKey())] ?: 0L
        return toState(id, count, deadline, boot)
    }

    private fun remainingSeconds(deadline: Long, bootStored: Int): Int {
        val now = SystemClock.elapsedRealtime()
        val bootNow = currentBootCount()
        if (bootNow != bootStored) {
            // If the boot happened after the last attempt, we consider timer to start from the beginning
            return maxOf(0, COOLDOWN_SECONDS - (now / 1000).toInt())
        }
        return maxOf(0, ((deadline - now) / 1000).toInt())
    }

    private fun toState(
        id: HotWalletAccessCodeAttemptsRepository.AttemptId,
        count: Int,
        deadlineElapsed: Long,
        bootStored: Int,
    ): Attempts {
        val fast = MAX_FAST_FORWARD_ATTEMPTS
        val attention = ATTEMPTS_BEFORE_DELETION
        val deletion = MAX_ATTEMPTS_BEFORE_DELETION

        return when {
            count < fast -> Attempts.FastForward(count)
            id.auth && count >= deletion -> Attempts.Deletion
            id.auth && count >= attention -> {
                val remaining = remainingSeconds(deadlineElapsed, bootStored)
                Attempts.BeforeDeletion(count, remaining, deletion - count)
            }
            else -> {
                val remaining = remainingSeconds(deadlineElapsed, bootStored)
                Attempts.WithDelay(count, remaining)
            }
        }
    }

    private fun HotWalletAccessCodeAttemptsRepository.AttemptId.attemptIdKey(): String {
        return "${hotWalletId.value}_$auth"
    }

    private fun currentBootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private data class AttemptsPersistentData(
        val attempts: Int,
        val bootCount: Int,
        val deadline: Long,
    )
}