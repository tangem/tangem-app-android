package com.tangem.tap.features.details.ui.securityprivacy

import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

class ChangeAccessCodeFragment : BaseStoreFragment(R.layout.view_compose_fragment),
    StoreSubscriber<DetailsState> {


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