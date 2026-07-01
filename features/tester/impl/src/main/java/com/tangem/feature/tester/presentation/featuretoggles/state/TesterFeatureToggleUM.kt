package com.tangem.feature.tester.presentation.featuretoggles.state

/**
 * Presentation model of feature toggle
 *
 * @property name      name
 * @property version   release version ("undefined" for permanently disabled toggles)
 * @property status    release status relative to the current app version
 * @property isEnabled availability
 */
internal data class TesterFeatureToggleUM(
    val name: String,
    val version: String,
    val status: Status,
    val isEnabled: Boolean,
) {

    /**
     * Release status relative to the current app version.
     *
     * Declaration order defines the display order of groups on the screen
     * (most interesting first, least interesting last).
     */
    enum class Status(val title: String, val emoji: String) {
        PENDING("Planned for current release", "⏳"),
        PLANNED("Planned for next releases", "🗓️"),
        UNDEFINED("Not planned yet", "❓"),
        RELEASED("Released", "✅"),
    }
}