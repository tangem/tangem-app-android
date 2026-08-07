package com.tangem.features.pushnotificationsettings.impl.model

import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.IsPushNotificationFirstActivationDoneUseCase
import com.tangem.domain.pushnotificationpreferences.MarkPushNotificationFirstActivationDoneUseCase
import com.tangem.domain.pushnotificationpreferences.ObserveWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.SetAllWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.UpdateWalletPushNotificationPreferenceUseCase
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.wallets.usecase.ApplyPushNotificationFirstActivationUseCase
import com.tangem.domain.wallets.usecase.SetNotificationsEnabledUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotificationsettings.component.PushNotificationSettingsComponent
import com.tangem.features.pushnotificationsettings.impl.R
import com.tangem.features.pushnotificationsettings.impl.entity.AllowPushNotificationsBannerUM
import com.tangem.features.pushnotificationsettings.impl.entity.NetworksAvailableForNotificationBSConfig
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleId
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class PushNotificationSettingsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val observePreferences: ObserveWalletPushNotificationPreferencesUseCase,
    private val updatePreference: UpdateWalletPushNotificationPreferenceUseCase,
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider,
    private val settingsManager: SettingsManager,
    private val setAllPreferences: SetAllWalletPushNotificationPreferencesUseCase,
    private val setNotificationsEnabled: SetNotificationsEnabledUseCase,
    private val isFirstActivationDone: IsPushNotificationFirstActivationDoneUseCase,
    private val markFirstActivationDone: MarkPushNotificationFirstActivationDoneUseCase,
    private val applyFirstActivation: ApplyPushNotificationFirstActivationUseCase,
    private val notificationsRepository: NotificationsRepository,
) : Model() {

    private val params: PushNotificationSettingsComponent.Params = paramsContainer.require()
    private val userWalletId: UserWalletId get() = params.userWalletId

    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    private val osNotificationsEnabled = MutableStateFlow(systemNotificationsStateProvider.areNotificationsEnabled())

    private var pendingPermissionToggle: ToggleSpec? = null
    private val wasAutoActivationAttempted = AtomicBoolean(false)
    private val preferencesJobHolder = JobHolder()

    private val cachedPrefs: WalletPushNotificationPreferences?
        get() = (loadState.value as? LoadState.Content)?.prefs

    private val requestPushPermissionChannel = Channel<Unit>(Channel.BUFFERED)

    /** One-shot requests to launch the system push permission prompt, consumed by the component. */
    val requestPushPermission: Flow<Unit> = requestPushPermissionChannel.receiveAsFlow()

    val uiState: StateFlow<PushNotificationSettingsUM> = combine(
        loadState,
        osNotificationsEnabled,
    ) { load, osEnabled ->
        when (load) {
            is LoadState.Failed -> PushNotificationSettingsUM.Error(onRetryClick = ::onRetry)
            is LoadState.Loading -> PushNotificationSettingsUM.Loading
            is LoadState.Content -> buildContent(prefs = load.prefs, osEnabled = osEnabled)
        }
    }.stateIn(
        scope = modelScope,
        // Eagerly: tests read uiState.value synchronously after advanceUntilIdle() with no live collector.
        started = SharingStarted.Eagerly,
        initialValue = PushNotificationSettingsUM.Loading,
    )

    val bottomSheetNavigation: SlotNavigation<NetworksAvailableForNotificationBSConfig> = SlotNavigation()

    private val ToggleId.analyticsValue: String
        get() = when (this) {
            ToggleId.TransactionAlerts -> "transaction_alerts"
            ToggleId.OffersUpdates -> "offers_updates"
            ToggleId.PriceAlerts -> "price_alerts"
        }

    init {
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.NotificationSettingsScreenOpened(
                isSystemPermissionEnabled = osNotificationsEnabled.value,
            ),
        )
        subscribeOnPreferences()
    }

    fun onResume() {
        osNotificationsEnabled.value = systemNotificationsStateProvider.areNotificationsEnabled()
        if (cachedPrefs == null) return

        // An explicitly tapped toggle wins over the silent first-activation rule.
        if (pendingPermissionToggle != null) {
            modelScope.launch { applyPendingToggle() }
        } else {
            autoApplyFirstActivationIfNeeded()
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        modelScope.launch {
            // isGranted alone is unreliable: an already-granted-but-OS-disabled wallet returns true
            // instantly, and pre-Android 13 there is no runtime permission at all.
            val isNotificationsEnabled = systemNotificationsStateProvider.areNotificationsEnabled()
            osNotificationsEnabled.value = isNotificationsEnabled
            analyticsEventHandler.send(PushNotificationAnalyticEvents.PermissionStatus(isAllowed = isGranted))

            if (isNotificationsEnabled) {
                applyPendingToggle()
            } else {
                // The tapped toggle stays pending until the user comes back from the system settings.
                showEnableNotificationsDialog()
            }
        }
    }

    /**
     * Applies the toggle the user tapped before the permission ask, once notifications are actually enabled.
     * A refusal never fixes the first-activation flag, so the rule survives until a real grant.
     */
    private suspend fun applyPendingToggle() {
        val tapped = pendingPermissionToggle ?: return
        if (!osNotificationsEnabled.value || cachedPrefs == null) return
        // Consumed before the first suspension point, so a concurrent resume cannot apply it twice.
        pendingPermissionToggle = null

        if (isFirstActivationDone(userWalletId)) {
            applyOptimisticToggle(tapped, newValue = true)
        } else if (enableAllCategories(initiatingToggle = tapped)) {
            // Fix the flag only after a successful enable, so a transient failure can retry.
            markFirstActivationDone(userWalletId)
        }
    }

    private fun subscribeOnPreferences() {
        observePreferences(userWalletId)
            .catch {
                // Fall to Failed only when nothing is cached yet; otherwise keep showing the last value.
                if (loadState.value !is LoadState.Content) loadState.value = LoadState.Failed
            }
            .onEach { value ->
                loadState.value = LoadState.Content(value)
                autoApplyFirstActivationIfNeeded()
            }
            .launchIn(modelScope)
            .saveIn(preferencesJobHolder)
    }

    /**
     * Silently re-applies the first-activation rule once preferences are loaded: the grant-time attempt

     */
    private fun autoApplyFirstActivationIfNeeded() {
        if (!wasAutoActivationAttempted.compareAndSet(false, true)) return
        modelScope.launch(dispatchers.io) {
            val areGatesPassed = osNotificationsEnabled.value &&
                notificationsRepository.isUserAllowToSubscribeOnPushNotifications()
            if (areGatesPassed) {
                // Deliberately not retried on failure within this screen instance: a retry fired by the
                // cache echo of a manual toggle write would force-enable all three against the user's choice.
                applyFirstActivation(userWalletId)
            } else {
                // A gate miss is not an attempt — onResume may retry after the OS state changes.
                wasAutoActivationAttempted.set(false)
            }
        }
    }

    private fun buildContent(
        prefs: WalletPushNotificationPreferences,
        osEnabled: Boolean,
    ): PushNotificationSettingsUM.Content {
        return PushNotificationSettingsUM.Content(
            banner = buildBanner(prefs = prefs, osEnabled = osEnabled),
            toggles = buildToggles(prefs),
            onMoreInfoClick = ::onMoreInfoClick,
        )
    }

    private fun onMoreInfoClick() {
        bottomSheetNavigation.activate(NetworksAvailableForNotificationBSConfig)
    }

    private fun buildToggles(prefs: WalletPushNotificationPreferences): PersistentList<ToggleUM> {
        return TOGGLE_ORDER
            .asSequence()
            .map { id -> id.spec(prefs) }
            .map { spec ->
                ToggleUM(
                    id = spec.id,
                    titleRes = spec.titleRes,
                    subtitle = spec.subtitle,
                    isOn = spec.preference.isEnabled,
                    onCheckedChange = { newValue -> onToggleTapped(spec, newValue) },
                )
            }
            .toList()
            .toPersistentList()
    }

    private fun buildBanner(
        prefs: WalletPushNotificationPreferences,
        osEnabled: Boolean,
    ): AllowPushNotificationsBannerUM? {
        val isAnyOn = prefs.transactionAlerts.isEnabled ||
            prefs.offersUpdates.isEnabled ||
            prefs.priceAlerts.isEnabled
        if (osEnabled || !isAnyOn) return null
        return AllowPushNotificationsBannerUM(onOpenSettingsClick = ::onBannerCtaClick)
    }

    private fun requestPermission(tapped: ToggleSpec? = null) {
        pendingPermissionToggle = tapped
        requestPushPermissionChannel.trySend(Unit)
    }

    private fun onBannerCtaClick() {
        analyticsEventHandler.send(PushNotificationAnalyticEvents.BannerOpenSettingsTapped())
        // The banner only shows when OS notifications are disabled, which also covers the case
        // where POST_NOTIFICATIONS is already granted but notifications are off at the system level.
        // A permission request would be a no-op there, so send the user to the OS settings instead.
        settingsManager.openAppNotificationSettings()
    }

    private fun onRetry() {
        loadState.value = LoadState.Loading
        subscribeOnPreferences()
    }

    private fun onToggleTapped(spec: ToggleSpec, newValue: Boolean) {
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.ToggleClicked(toggleType = spec.id.analyticsValue, isEnabled = newValue),
        )

        if (newValue && !osNotificationsEnabled.value) {
            requestPermission(tapped = spec)
            return
        }

        applyOptimisticToggle(spec, newValue)
    }

    private fun applyOptimisticToggle(spec: ToggleSpec, newValue: Boolean) {
        val current = cachedPrefs ?: return
        loadState.value = LoadState.Content(current.withCategory(spec.category, newValue))

        // Writes are intentionally not serialized here: serializing repository writes is the data
        // layer's responsibility, not the model's. A failed write reverts only its own category.
        modelScope.launch { writeToggle(spec, newValue) }
    }

    private suspend fun writeToggle(spec: ToggleSpec, newValue: Boolean) {
        if (spec.category == PushNotificationCategory.TransactionAlerts) {
            writeTransactionAlerts(spec, newValue)
        } else {
            updatePreference(userWalletId, spec.category, newValue).onLeft { revertOptimistic(spec, newValue) }
        }
    }

    /** Tokens (address re-subscription) first, then preferences; on failure revert and undo the token subscription. */
    private suspend fun writeTransactionAlerts(spec: ToggleSpec, newValue: Boolean) {
        val tokensResult = setNotificationsEnabled(userWalletId, isEnabled = newValue)
        if (tokensResult is Either.Left) {
            revertOptimistic(spec, newValue)
            return
        }
        updatePreference(userWalletId, spec.category, newValue).onLeft {
            setNotificationsEnabled(userWalletId, isEnabled = !newValue)
            revertOptimistic(spec, newValue)
        }
    }

    /** Enables all three categories at once (first-activation): tokens first, then preferences. True on full success. */
    private suspend fun enableAllCategories(initiatingToggle: ToggleSpec?): Boolean {
        val previous = cachedPrefs ?: return false
        loadState.value = LoadState.Content(
            previous.copy(
                transactionAlerts = previous.transactionAlerts.copy(isEnabled = true),
                offersUpdates = previous.offersUpdates.copy(isEnabled = true),
                priceAlerts = previous.priceAlerts.copy(isEnabled = true),
            ),
        )
        val tokensResult = setNotificationsEnabled(userWalletId, isEnabled = true)
        if (tokensResult is Either.Left) {
            revertAll(previous, initiatingToggle)
            return false
        }
        return setAllPreferences(
            userWalletId = userWalletId,
            transactionAlerts = true,
            offersUpdates = true,
            priceAlerts = true,
        ).fold(
            ifLeft = {
                setNotificationsEnabled(userWalletId, isEnabled = false)
                revertAll(previous, initiatingToggle)
                false
            },
            ifRight = { true },
        )
    }

    private fun revertOptimistic(spec: ToggleSpec, newValue: Boolean) {
        // Revert only the failed category on top of the current state, so a concurrent toggle's
        // optimistic value isn't clobbered by a stale full-snapshot replacement.
        loadState.update { state ->
            if (state is LoadState.Content) {
                LoadState.Content(state.prefs.withCategory(spec.category, !newValue))
            } else {
                state
            }
        }
        showWriteErrorMessage(toggleType = spec.id.analyticsValue)
    }

    private fun revertAll(previous: WalletPushNotificationPreferences, initiatingToggle: ToggleSpec?) {
        loadState.update { state -> if (state is LoadState.Content) LoadState.Content(previous) else state }
        showWriteErrorMessage(toggleType = (initiatingToggle?.id ?: ToggleId.TransactionAlerts).analyticsValue)
    }

    private fun showWriteErrorMessage(toggleType: String) {
        analyticsEventHandler.send(
            PushNotificationAnalyticEvents.NotificationSettingsErrorShown(
                toggleType = toggleType,
                errorType = ERROR_TYPE_WRITE_FAILED,
            ),
        )
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_ok),
                    onClick = {},
                ),
            ),
        )
    }

    private fun showEnableNotificationsDialog() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.push_notifications_permission_alert_title),
                message = resourceReference(R.string.push_notifications_permission_alert_description),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.push_notifications_permission_alert_positive_button),
                    onClick = { settingsManager.openAppNotificationSettings() },
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(R.string.push_notifications_permission_alert_negative_button),
                    // Declining is a final answer: the tapped toggle is dropped.
                    onClick = { pendingPermissionToggle = null },
                ),
            ),
        )
    }

    private fun ToggleId.spec(prefs: WalletPushNotificationPreferences): ToggleSpec = when (this) {
        ToggleId.TransactionAlerts -> ToggleSpec(
            id = this,
            titleRes = R.string.push_notification_settings_transaction_alerts_title,
            subtitle = resourceReference(R.string.push_notification_settings_transaction_alerts_subtitle),
            preference = prefs.transactionAlerts,
            category = PushNotificationCategory.TransactionAlerts,
        )
        ToggleId.OffersUpdates -> ToggleSpec(
            id = this,
            titleRes = R.string.push_notification_settings_offers_updates_title,
            subtitle = resourceReference(R.string.push_notification_settings_offers_updates_subtitle),
            preference = prefs.offersUpdates,
            category = PushNotificationCategory.OffersUpdates,
        )
        ToggleId.PriceAlerts -> ToggleSpec(
            id = this,
            titleRes = R.string.push_notification_settings_price_alerts_title,
            subtitle = resourceReference(R.string.push_notification_settings_price_alerts_subtitle),
            preference = prefs.priceAlerts,
            category = PushNotificationCategory.PriceAlerts,
        )
    }

    private data class ToggleSpec(
        val id: ToggleId,
        val titleRes: Int,
        val subtitle: TextReference,
        val preference: PushNotificationPreference,
        val category: PushNotificationCategory,
    )

    private sealed interface LoadState {
        data object Loading : LoadState
        data object Failed : LoadState
        data class Content(val prefs: WalletPushNotificationPreferences) : LoadState
    }

    private companion object {
        const val ERROR_TYPE_WRITE_FAILED = "Write Failed"
        val TOGGLE_ORDER = listOf(
            ToggleId.TransactionAlerts,
            ToggleId.OffersUpdates,
            ToggleId.PriceAlerts,
        )
    }
}