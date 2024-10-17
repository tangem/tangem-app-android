package com.tangem.tap.features.onboarding

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.utils.Provider
import com.tangem.wallet.R
import kotlinx.coroutines.launch

/**
[REDACTED_AUTHOR]
 */
class OnboardingMenuProvider(
    private val scanResponseProvider: Provider<ScanResponse>,
) : MenuProvider {

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_onboarding, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.menu_item_chat_support -> {
            Analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.Intro))

            val cardInfo = store.inject(DaggerGraphState::getCardInfoUseCase).invoke(scanResponseProvider()).getOrNull()
                ?: error("CardInfo must be not null")

            scope.launch {
                store.inject(DaggerGraphState::sendFeedbackEmailUseCase)
                    .invoke(type = FeedbackEmailType.DirectUserRequest(cardInfo))
            }

            true
        }
        else -> false
    }
}