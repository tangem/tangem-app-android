package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.domain.settings.UsedeskTokenTtlManager
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.HideAllCurrenciesUM
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.ToggleHotWalletRestrictionUM
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.UsedeskTokenTtlUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
internal class TesterActionsViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsSaver: WalletAccountsSaver,
    private val hotWalletRestrictionManager: HotWalletRestrictionManager,
    private val usedeskTokenTtlManager: UsedeskTokenTtlManager,
) : ViewModel() {

    var uiState: TesterActionsContentState by mutableStateOf(initialState)
        private set

    private val initialState: TesterActionsContentState
        get() = TesterActionsContentState(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this::hideAllCurrencies),
            toggleHotWalletRestrictionUM = ToggleHotWalletRestrictionUM(
                isEnabled = hotWalletRestrictionManager.isCreationEnabledSync(),
                onClick = this::toggleHotWalletRestriction,
            ),
            usedeskTokenTtlUM = UsedeskTokenTtlUM(
                currentLabel = formatTtl(usedeskTokenTtlManager.getTokenTtlMillisSync()),
                presets = TTL_PRESETS,
                onPresetSelected = this::setUsedeskTokenTtl,
                onCustomMinutesSelected = { minutes -> setUsedeskTokenTtl(minutes.minutes.inWholeMilliseconds) },
            ),
            shareLogsUM = TesterActionsContentState.ShareLogsUM(file = feedbackRepository.getLogFile()),
            onBackClick = { /* no-op */ },
        )

    init {
        bootstrapHotWalletRestrictionUpdates()
        bootstrapUsedeskTokenTtlUpdates()
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState = uiState.copy(onBackClick = router::back)
    }

    private fun hideAllCurrencies() = viewModelScope.launch {
        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Progress,
        )

        val userWalletId = userWalletsListRepository.selectedUserWalletSync()?.walletId

        if (userWalletId != null) {
            walletAccountsSaver.update(userWalletId = userWalletId) { response ->
                response ?: return@update response

                response.copy(
                    accounts = response.accounts.map { accountDTO ->
                        accountDTO.copy(tokens = emptyList())
                    },
                )
            }
        }

        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this@TesterActionsViewModel::hideAllCurrencies),
        )
    }

    private fun toggleHotWalletRestriction() = viewModelScope.launch {
        hotWalletRestrictionManager.toggleCreationEnabled()
    }

    private fun setUsedeskTokenTtl(millis: Long) = viewModelScope.launch {
        usedeskTokenTtlManager.setTokenTtlMillis(millis)
    }

    private fun bootstrapHotWalletRestrictionUpdates() {
        hotWalletRestrictionManager.isCreationEnabled()
            .onEach { isEnabled ->
                uiState = uiState.copy(
                    toggleHotWalletRestrictionUM = uiState.toggleHotWalletRestrictionUM.copy(
                        isEnabled = isEnabled,
                    ),
                )
            }
            .launchIn(viewModelScope)
    }

    private fun bootstrapUsedeskTokenTtlUpdates() {
        usedeskTokenTtlManager.getTokenTtlMillis()
            .onEach { millis ->
                uiState = uiState.copy(
                    usedeskTokenTtlUM = uiState.usedeskTokenTtlUM.copy(
                        currentLabel = formatTtl(millis),
                    ),
                )
            }
            .launchIn(viewModelScope)
    }

    private companion object {

        val TTL_PRESETS = persistentListOf(
            UsedeskTokenTtlUM.Preset(
                label = formatTtl(7.days.inWholeMilliseconds),
                millis = 7.days.inWholeMilliseconds,
            ),
            UsedeskTokenTtlUM.Preset(
                label = formatTtl(1.days.inWholeMilliseconds),
                millis = 1.days.inWholeMilliseconds,
            ),
            UsedeskTokenTtlUM.Preset(
                label = formatTtl(1.hours.inWholeMilliseconds),
                millis = 1.hours.inWholeMilliseconds,
            ),
            UsedeskTokenTtlUM.Preset(
                label = formatTtl(15.minutes.inWholeMilliseconds),
                millis = 15.minutes.inWholeMilliseconds,
            ),
        )

        /** Picks the largest whole unit (days → hours → minutes → seconds) for a readable label. */
        fun formatTtl(millis: Long): String {
            val duration = millis.milliseconds
            return when {
                duration.inWholeDays >= 1 && duration == duration.inWholeDays.days -> duration.inWholeDays.unit("day")
                duration.inWholeHours >= 1 && duration == duration.inWholeHours.hours ->
                    duration.inWholeHours.unit("hour")
                duration.inWholeMinutes >= 1 -> duration.inWholeMinutes.unit("minute")
                else -> duration.inWholeSeconds.unit("second")
            }
        }

        fun Long.unit(name: String): String = "$this $name${if (this == 1L) "" else "s"}"
    }
}