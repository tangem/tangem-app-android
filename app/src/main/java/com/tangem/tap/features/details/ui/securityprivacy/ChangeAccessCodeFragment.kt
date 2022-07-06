package com.tangem.tap.features.details.ui.securityprivacy

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ViewComposeFragmentBinding
import org.rekotlin.StoreSubscriber

class ChangeAccessCodeFragment : BaseStoreFragment(R.layout.view_compose_fragment),
    StoreSubscriber<DetailsState> {

    private val binding: ViewComposeFragmentBinding
            by viewBinding(ViewComposeFragmentBinding::bind)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    override fun newState(state: DetailsState) {

    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
        }
        storeSubscribersList.add(this)
    }

}