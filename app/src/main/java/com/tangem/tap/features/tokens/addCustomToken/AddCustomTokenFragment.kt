package com.tangem.tap.features.tokens.addCustomToken

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.compose.ClosePopupTrigger
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.FragmentOnBackPressedHandler
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.tokens.addCustomToken.compose.AddCustomTokenScreen
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

/**
 * Created by Anton Zhilenkov on 23/03/2022.
 */
class AddCustomTokenFragment : BaseStoreFragment(R.layout.view_compose_fragment), StoreSubscriber<AddCustomTokenState> {

    private var state: MutableState<AddCustomTokenState> = mutableStateOf(domainStore.state.addCustomTokensState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(ManageTokens.CustomToken.ScreenOpened())
    }

    override fun subscribeToStore() {
        domainStore.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.addCustomTokensState == newState.addCustomTokensState
            }.select { it.addCustomTokensState }
        }
    }

    override fun newState(state: AddCustomTokenState) {
        if (activity == null || view == null) return

        this.state.value = state
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        view.findViewById<Toolbar>(R.id.toolbar)?.setTitle(R.string.add_custom_token_title)

        val closePopupTrigger = initClosingPopupTriggerEvent()
        view.findViewById<ComposeView>(R.id.view_compose)?.setContent {
            TangemTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AddCustomTokenScreen(state, closePopupTrigger)
                }
            }
        }
    }

    private fun initClosingPopupTriggerEvent(): ClosePopupTrigger = ClosePopupTrigger().apply {
        onCloseComplete = ::handleOnBackPressed
        addBackPressHandler(
            object : FragmentOnBackPressedHandler {
                override fun handleOnBackPressed() = close()
            },
        )
    }
}
