package com.tangem.tap.features.send.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressPayIdActionUI.SetAddressOrPayId
import com.tangem.tap.features.send.redux.FeeActionUI.*
import com.tangem.tap.features.send.redux.ReleaseSendState
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.features.send.ui.stateSubscribers.WalletStateSubscriber
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_paste.*
import kotlinx.android.synthetic.main.btn_qr_code.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*

/**
[REDACTED_AUTHOR]
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    private val sendSubscriber = SendStateSubscriber(this)
    private val walletSubscriber = WalletStateSubscriber(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleFeeLayoutVisibility)
        }
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            store.dispatch(ChangeSelectedFee(checkedId))
        }
        swIncludeFee.setOnCheckedChangeListener { btn, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
        }

        imvPaste.setOnClickListener {
            store.dispatch(SetAddressOrPayId(requireContext().getFromClipboard()))
        }
        imvQrCode.setOnClickListener {
            requireActivity().startActivity(Intent(requireContext(), ScanQrCodeActivity::class.java))
        }
    }

    override fun subscribeToStore() {
        store.subscribe(walletSubscriber) { appState ->
            appState.skipRepeats { oldState, newState -> false }.select { it.walletState }
        }
        store.subscribe(sendSubscriber) { appState ->
            appState.skipRepeats { oldState, newState -> false }.select { it.sendState }
        }

        storeSubscribersList.add(walletSubscriber)
        storeSubscribersList.add(sendSubscriber)
    }

    override fun onDestroy() {
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}



