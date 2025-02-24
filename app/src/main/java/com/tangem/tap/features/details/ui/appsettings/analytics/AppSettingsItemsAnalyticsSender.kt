package com.tangem.tap.features.details.ui.appsettings.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.appsettings.AppSettingsItemsFactory
import com.tangem.tap.features.details.ui.appsettings.AppSettingsScreenState
import javax.inject.Inject

@ComponentScoped
internal class AppSettingsItemsAnalyticsSender @Inject constructor(
    private val analyticsHandler: AnalyticsEventHandler,
) {

    fun send(items: List<AppSettingsScreenState.Item>) {
        val events = getEvents(items)

        events.forEach { analyticsHandler.send(it) }
    }

    private fun getEvents(items: List<AppSettingsScreenState.Item>): Set<AnalyticsEvent> {
        return items.mapNotNullTo(mutableSetOf(), ::getEvent)
    }

    private fun getEvent(item: AppSettingsScreenState.Item): AnalyticsEvent? {
        return when (item.id) {
            AppSettingsItemsFactory.ID_ENROLL_BIOMETRICS_CARD -> Settings.AppSettings.EnableBiometrics
            else -> null
        }
    }
}