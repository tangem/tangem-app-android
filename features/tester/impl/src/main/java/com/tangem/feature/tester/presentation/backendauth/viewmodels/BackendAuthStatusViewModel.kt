package com.tangem.feature.tester.presentation.backendauth.viewmodels

import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM.Action
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM.IconAction
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM.Section
import com.tangem.feature.tester.presentation.backendauth.state.BackendAuthStatusUM.StatusRow
import com.tangem.core.ui.R as CoreUiR
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.hours

/**
 * ViewModel for the Backend Authentication status screen.
 *
 * Aggregates the observable auth state (toggle, environment, device key, registration flag,
 * session tokens) so QA doesn't have to read logcat, and exposes action buttons grouped by the
 * state they operate on. Keys/tokens are shown shortened with a copy action for the full value.
 */
@Suppress("LongParameterList")
@HiltViewModel
internal class BackendAuthStatusViewModel @Inject constructor(
    private val authFeatureToggles: AuthFeatureToggles,
    private val deviceKeyManager: DeviceKeyManager,
    private val sessionTokensStore: SessionTokensStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val apiConfigsManager: ApiConfigsManager,
    private val clipboardManager: ClipboardManager,
    private val deviceRegistrar: DeviceRegistrar,
    private val sessionTokenRefresher: SessionTokenRefresher,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackendAuthStatusUM(onCopyClick = ::onCopy))
    val uiState: StateFlow<BackendAuthStatusUM> = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    /** Setup navigation state property by router [router] */
    fun setupNavigation(router: InnerTesterRouter) {
        _uiState.update { it.copy(onBackClick = router::back) }
    }

    private fun loadStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(sections = buildSections()) }
        }
    }

    private suspend fun buildSections(): ImmutableList<Section> {
        val publicKeyRow = deviceKeyManager.getPublicKeyEncoded().getOrNull()?.let { key ->
            val spki = Base64.encodeToString(key, Base64.NO_WRAP)
            StatusRow("Device public key (SPKI)", spki.shorten(), copyValue = spki)
        } ?: StatusRow("Device public key (SPKI)", "absent")

        val environmentValue = runCatching {
            val authEnv = apiConfigsManager.getEnvironmentConfig(ApiConfig.ID.Auth)
            "${authEnv.environment.name} (${authEnv.baseUrl})"
        }.getOrDefault("unavailable")
        val isRegistered = appPreferencesStore.getSyncOrDefault(
            PreferencesKeys.IS_DEVICE_REGISTERED_KEY,
            default = false,
        )
        val tokens = sessionTokensStore.get().getOrNull()

        return persistentListOf(
            Section(
                rows = persistentListOf(
                    StatusRow("Feature toggle", if (authFeatureToggles.isBackendAuthenticationEnabled) "ON" else "OFF"),
                    StatusRow("Environment", environmentValue),
                    publicKeyRow,
                ),
            ),
            Section(
                rows = persistentListOf(
                    StatusRow(
                        label = "Registered",
                        value = if (isRegistered) "✅" else "❌",
                        iconActions = persistentListOf(
                            iconAction("Register now", CoreUiR.drawable.ic_plus_24) {
                                deviceRegistrar.register().fold({ "failed: $it" }, { "ok" })
                            },
                            iconAction("Reset registration", CoreUiR.drawable.ic_close_24, isProgressShown = false) {
                                resetRegistration()
                            },
                        ),
                    ),
                ),
            ),
            Section(
                rows = buildTokenRows(tokens),
                actions = persistentListOf(
                    action("Force refresh") { sessionTokenRefresher.refresh().fold({ "failed: $it" }, { "ok" }) },
                    action("Corrupt access token") { corruptAccessToken() },
                    action("Expire access token") { expireAccessToken() },
                    action("Clear session tokens") { sessionTokensStore.clear(); "done" },
                ),
            ),
        )
    }

    private fun buildTokenRows(tokens: SessionTokens?): ImmutableList<StatusRow> = buildList {
        add(StatusRow("Session tokens", if (tokens != null) "✅" else "❌"))
        if (tokens != null) {
            val isAccessBlank = tokens.accessToken.isBlank()
            add(
                StatusRow(
                    label = "Access token",
                    value = if (isAccessBlank) "—" else tokens.accessToken.shorten(),
                    copyValue = tokens.accessToken.ifBlank { null },
                    subtitle = if (isAccessBlank) {
                        null
                    } else {
                        "expires ${tokens.accessTokenExpiresAt.formatWithCountdown(hoursMinutes = true)}"
                    },
                ),
            )
            val refresh = tokens.refreshToken
            add(
                StatusRow(
                    label = "Refresh token",
                    value = refresh?.shorten() ?: "none",
                    copyValue = refresh,
                    subtitle = tokens.refreshTokenExpiresAt?.let { "expires ${it.formatWithCountdown()}" },
                ),
            )
            add(StatusRow("Wallet IDs", tokens.walletIds.joinToString().ifEmpty { "—" }))
        }
    }.toImmutableList()

    private fun onCopy(row: StatusRow) {
        val value = row.copyValue ?: return
        clipboardManager.setText(text = value, isSensitive = false, label = row.label)
        Toast.makeText(context, "Copied ${row.label}", Toast.LENGTH_SHORT).show()
    }

    private fun action(label: String, block: suspend () -> String): Action = Action(label) { runAction(label, block) }

    private fun iconAction(
        label: String,
        iconRes: Int,
        isProgressShown: Boolean = true,
        block: suspend () -> String,
    ): IconAction = IconAction(
        label = label,
        iconRes = iconRes,
        isProgressShown = isProgressShown,
    ) { runAction(label, block) }

    /** Shows a progress on the tapped button, runs [block], toasts its result, then refreshes the panel. */
    private fun runAction(label: String, block: suspend () -> String) {
        if (_uiState.value.runningAction != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(runningAction = label) }
            val result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "error: ${e.message}"
            }
            Toast.makeText(context, "$label: $result", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(sections = buildSections(), runningAction = null) }
        }
    }

    /** Replaces the access token with a blank value (refresh token kept) so the next request 401s. */
    private suspend fun corruptAccessToken(): String {
        val tokens = sessionTokensStore.get().getOrNull() ?: return "no tokens"
        sessionTokensStore.save(tokens.copy(accessToken = ""))
        return "done"
    }

    /** Back-dates the access token expiry so it is treated as expired. */
    private suspend fun expireAccessToken(): String {
        val tokens = sessionTokensStore.get().getOrNull() ?: return "no tokens"
        sessionTokensStore.save(tokens.copy(accessTokenExpiresAt = Clock.System.now() - 1.hours))
        return "done"
    }

    /** Clears tokens and the registration flag so the next launch re-registers (no `pm clear`). */
    private suspend fun resetRegistration(): String {
        sessionTokensStore.clear()
        appPreferencesStore.store(PreferencesKeys.IS_DEVICE_REGISTERED_KEY, value = false)
        return "done"
    }

    /** Shortens a long value to `prefix…suffix` for display; the full value stays copyable. */
    private fun String.shorten(): String =
        if (length <= SHORTEN_KEEP * 2 + 1) this else "${take(SHORTEN_KEEP)}…${takeLast(SHORTEN_KEEP)}"

    /**
     * Formats an [Instant] as `dd.MM.yyyy HH:mm` plus a remaining-time hint.
     * @param hoursMinutes `true` → `… (3h 25m left)` (short-lived access token);
     *                     `false` → `… (2d 5h left)` (long-lived refresh token).
     */
    private fun Instant.formatWithCountdown(hoursMinutes: Boolean = false): String {
        val readable = EXPIRY_FORMATTER.format(toJavaInstant())
        val left = this - Clock.System.now()
        val hint = when {
            left.isNegative() -> "expired"
            hoursMinutes -> "${left.inWholeHours}h ${left.inWholeMinutes % MINUTES_IN_HOUR}m left"
            else -> "${left.inWholeDays}d ${left.inWholeHours % HOURS_IN_DAY}h left"
        }
        return "$readable ($hint)"
    }

    private companion object {
        const val SHORTEN_KEEP = 6
        const val HOURS_IN_DAY = 24
        const val MINUTES_IN_HOUR = 60
        val EXPIRY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())
    }
}