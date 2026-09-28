package com.tangem.feature.tester.presentation.menu.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.walletids.state.WALLET_IDS_TITLE
import kotlinx.collections.immutable.ImmutableSet

/**
 * Content state of tester menu screen
 *
 * @property onBackClick   the lambda to be invoked when back button is pressed
 * @property buttons       list of buttons
 * @property onButtonClick the lambda to be invoked when tester button is pressed
 */
data class TesterMenuUM(
    val onBackClick: () -> Unit,
    val buttons: ImmutableSet<ButtonUM>,
    val onButtonClick: (ButtonUM) -> Unit,
) {

    enum class ButtonUM(val text: TextReference) {
        FEATURE_TOGGLES(resourceReference(R.string.feature_toggles)),
        EXCLUDED_BLOCKCHAINS(resourceReference(R.string.excluded_blockchains)),
        ENVIRONMENT_TOGGLES(resourceReference(R.string.environment_toggles)),
        BLOCKCHAIN_PROVIDERS(resourceReference(R.string.blockchain_providers)),
        TESTER_ACTIONS(resourceReference(R.string.tester_actions)),
        TEST_PUSHES(resourceReference(R.string.test_push)),
        ACCOUNTS(resourceReference(R.string.accounts)),
        ADDRESSES_INFO(resourceReference(R.string.addresses_info)),
        WALLET_IDS(stringReference(WALLET_IDS_TITLE)),
        STORY_BOOK(resourceReference(R.string.story_book)),
        SURVEY_SPARROW(resourceReference(R.string.survey_sparrow)),
        BACKEND_AUTH_STATUS(resourceReference(R.string.backend_auth_status)),
        SELL_REDIRECT_GENERATOR(resourceReference(R.string.sell_redirect_generator)),
    }
}