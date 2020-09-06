package com.tangem.tap.features.send.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressPayIdActionUI.*
import com.tangem.tap.features.send.redux.FeeActionUI.*
import com.tangem.tap.features.send.redux.ReleaseSendState
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_paste.*
import kotlinx.android.synthetic.main.btn_qr_code.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
[REDACTED_AUTHOR]
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    private val sendSubscriber = SendStateSubscriber(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAddressOrPayIdLayout()
        setupFeeLayout()
    }

    private fun setupAddressOrPayIdLayout() {
        store.dispatch(SetTruncateHandler { etAddressOrPayId.truncateMiddleWith(it, " *** ") })

        etAddressOrPayId.setOnFocusChangeListener { v, hasFocus ->
            store.dispatch(TruncateOrRestore(!hasFocus))
        }
        etAddressOrPayId.inputedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.addressPayIDState.etFieldValue != it }
                .onEach { store.dispatch(SetAddressOrPayId(it)) }
                .launchIn(mainScope)

        imvPaste.setOnClickListener {
            store.dispatch(SetAddressOrPayId(requireContext().getFromClipboard()?.toString() ?: ""))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        }
        imvQrCode.setOnClickListener {
            startActivityForResult(
                    Intent(requireContext(), ScanQrCodeActivity::class.java),
                    ScanQrCodeActivity.SCAN_QR_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != ScanQrCodeActivity.SCAN_QR_REQUEST_CODE) return

        val scannedCode = data?.getStringExtra(ScanQrCodeActivity.SCAN_RESULT) ?: ""
        store.dispatch(SetAddressOrPayId(scannedCode))
        store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
    }

    private fun setupFeeLayout() {
        flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleFeeLayoutVisibility)
        }
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            store.dispatch(ChangeSelectedFee(checkedId))
        }
        swIncludeFee.setOnCheckedChangeListener { btn, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
        }
    }

    override fun subscribeToStore() {
        store.subscribe(sendSubscriber) { appState ->
            appState.skipRepeats { oldState, newState -> oldState == newState }.select { it.sendState }
        }

        storeSubscribersList.add(sendSubscriber)
    }

    override fun onDestroy() {
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}

@ExperimentalCoroutinesApi
fun EditText.inputedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> offer(editable?.toString() ?: "") }
    awaitClose { removeTextChangedListener(watcher) }
}



