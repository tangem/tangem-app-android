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
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.domainStore
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.tokens.addCustomToken.compose.AddCustomTokenScreen
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
class AddCustomTokenFragment : BaseStoreFragment(R.layout.view_compose_fragment), StoreSubscriber<AddCustomTokenState> {

    private var state: MutableState<AddCustomTokenState> = mutableStateOf(domainStore.state.addCustomTokensState)

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

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        view.findViewById<Toolbar>(R.id.toolbar)?.let {
            it.setTitle(R.string.add_custom_token_title)
        }

        view.findViewById<ComposeView>(R.id.view_compose)?.setContent {
            AppCompatTheme(requireContext()) {
                Box(modifier = Modifier
                    .fillMaxSize()
                ) {
                    AddCustomTokenScreen(state)
                }

            }
        }
        addBackPressHandler(this)
    }
}