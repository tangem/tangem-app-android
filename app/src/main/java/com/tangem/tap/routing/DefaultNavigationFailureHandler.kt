package com.tangem.tap.routing

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.decompose.navigation.NavigationFailureHandler
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.R
import javax.inject.Inject

/** Sends a failed root navigation to analytics and the log, and tells the user about it. */
internal class DefaultNavigationFailureHandler @Inject constructor(
    private val config: AppRouterConfig,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : NavigationFailureHandler {

    private val logger = TangemLogger.withTag("Router")

    override fun onNavigationFailed(errorMessage: String) {
        analyticsExceptionHandler.sendException(ExceptionAnalyticsEvent(RuntimeException(errorMessage)))
        logger.e(errorMessage)

        with(receiver = config.snackbarHandler ?: return) {
            showSnackbar(
                text = R.string.common_unknown_error,
                buttonTitle = R.string.common_ok,
                action = { dismissSnackbar() },
            )
        }
    }
}