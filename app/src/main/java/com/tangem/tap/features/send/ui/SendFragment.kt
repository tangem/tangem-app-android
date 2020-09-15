package com.tangem.tap.features.send.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import com.tangem.merchant.common.toggleWidget.ToggleWidget
import com.tangem.tangem_sdk_new.extensions.hideSoftKeyboard
import com.tangem.tap.common.KeyboardObserver
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.tap.common.extensions.setOnImeActionListener
import com.tangem.tap.common.qrCodeScan.ScanQrCodeActivity
import com.tangem.tap.common.snackBar.MaxAmountSnackbar
import com.tangem.tap.common.text.truncateMiddleWith
import com.tangem.tap.common.toggleWidget.*
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.*
import com.tangem.tap.features.send.redux.AmountActionUi.*
import com.tangem.tap.features.send.redux.FeeActionUi.*
import com.tangem.tap.features.send.redux.states.FeeType
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.ui.stateSubscribers.SendStateSubscriber
import com.tangem.tap.mainScope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_amount.*
import kotlinx.android.synthetic.main.layout_send_fee.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
[REDACTED_AUTHOR]
 */
class SendFragment : BaseStoreFragment(R.layout.fragment_send) {

    lateinit var sendBtn: ToggleWidget

    private fun initSendButtonStates() {
        sendBtn = ToggleWidget(flSendButtonContainer, btnSend, progress, ProgressState.None())
        sendBtn.setupSendButtonStateModifiers(requireContext())
        sendBtn.setState(ProgressState.None())
    }

    private val sendSubscriber = SendStateSubscriber(this)
    private lateinit var keyboardObserver: KeyboardObserver

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSendButtonStates()
        setupAddressOrPayIdLayout()
        setupAmountLayout()
        setupFeeLayout()

        btnSend.setOnClickListener {
            store.dispatch(SendActionUi.SendAmountToRecipient)
        }
    }

    private fun setupAddressOrPayIdLayout() {
        store.dispatch(SetTruncateHandler { etAddressOrPayId.truncateMiddleWith(it, "...") })
        store.dispatch(AddressPayIdVerifyAction.VerifyClipboard(requireContext().getFromClipboard()?.toString()))

        etAddressOrPayId.setOnFocusChangeListener { v, hasFocus ->
            store.dispatch(TruncateOrRestore(!hasFocus))
        }
        etAddressOrPayId.inputtedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.addressPayIdState.etFieldValue != it }
                .onEach {
                    store.dispatch(ChangeAddressOrPayId(it))
                    store.dispatch(FeeAction.RequestFee)
                }
                .launchIn(mainScope)

        imvPaste.setOnClickListener {
            store.dispatch(ChangeAddressOrPayId(requireContext().getFromClipboard()?.toString() ?: ""))
            store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
            store.dispatch(FeeAction.RequestFee)
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
        if (scannedCode.isEmpty()) return

        store.dispatch(ChangeAddressOrPayId(scannedCode))
        store.dispatch(TruncateOrRestore(!etAddressOrPayId.isFocused))
        store.dispatch(FeeAction.RequestFee)
    }

    private fun setupAmountLayout() {
        store.dispatch(SetMainCurrency(restoreMainCurrency()))
        store.dispatch(ReceiptAction.RefreshReceipt)
        store.dispatch(SendAction.ChangeSendButtonState(store.state.sendState.getButtonState()))

        tvAmountCurrency.setOnClickListener {
            store.dispatch(ToggleMainCurrency)
            store.dispatch(ReceiptAction.RefreshReceipt)
            store.dispatch(SendAction.ChangeSendButtonState(store.state.sendState.getButtonState()))
        }

        val maxAmountSnackbar = MaxAmountSnackbar.make(etAmountToSend) {
            etAmountToSend.clearFocus()
            etAmountToSend.postDelayed(200) { etAmountToSend.hideSoftKeyboard() }
            store.dispatch(SetMaxAmount)
            store.dispatch(CheckAmountToSend())
        }
        var snackbarControlledByChangingFocus = false
        keyboardObserver = KeyboardObserver(requireActivity())
        keyboardObserver.registerListener { isShow ->
            if (snackbarControlledByChangingFocus) return@registerListener

            if (isShow) {
                if (etAmountToSend.isFocused && !maxAmountSnackbar.isShown) maxAmountSnackbar.show()
            } else {
                if (maxAmountSnackbar.isShown) maxAmountSnackbar.dismiss()
            }
        }

        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
            snackbarControlledByChangingFocus = true
            if (hasFocus) {
                etAmountToSend.postDelayed(200) {
                    maxAmountSnackbar.show()
                    snackbarControlledByChangingFocus = false
                }
            } else {
                etAmountToSend.postDelayed(350) {
                    maxAmountSnackbar.dismiss()
                    snackbarControlledByChangingFocus = false
                }
            }
        }

        val prevFocusChangeListener = etAmountToSend.onFocusChangeListener
        etAmountToSend.setOnFocusChangeListener { v, hasFocus ->
            prevFocusChangeListener.onFocusChange(v, hasFocus)
//            if (hasFocus && etAmountToSend.text?.toString() == "0") etAmountToSend.setText("")
            if (!hasFocus && etAmountToSend.text?.toString() == "") etAmountToSend.setText("0")
        }

        etAmountToSend.inputtedTextAsFlow()
                .debounce(400)
                .filter { store.state.sendState.amountState.viewAmountValue != it && it.isNotEmpty() }
                .onEach { store.dispatch(CheckAmountToSend(it)) }
                .launchIn(mainScope)

        etAmountToSend.setOnImeActionListener(EditorInfo.IME_ACTION_DONE) {
            it.hideSoftKeyboard()
            it.clearFocus()
        }
    }

    private fun setupFeeLayout() {
        flExpandCollapse.setOnClickListener {
            store.dispatch(ToggleControlsVisibility)
        }
        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == -1) return@setOnCheckedChangeListener

            store.dispatch(ChangeSelectedFee(FeeUiHelper.idToFee(checkedId)))
            store.dispatch(CheckAmountToSend())
        }
        swIncludeFee.setOnCheckedChangeListener { btn, isChecked ->
            store.dispatch(ChangeIncludeFee(isChecked))
            store.dispatch(CheckAmountToSend())
        }
    }

    override fun subscribeToStore() {
        store.subscribe(sendSubscriber) { appState ->
            appState.skipRepeats { oldState, newState ->
                oldState.sendState == newState.sendState
            }.select { it.sendState }
        }
        storeSubscribersList.add(sendSubscriber)
    }

    private fun restoreMainCurrency(): MainCurrencyType {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        val mainCurrency = sp.getString("mainCurrency", TapCurrency.main)
        val foundType = MainCurrencyType.values()
                .firstOrNull { it.name.toLowerCase() == mainCurrency!!.toLowerCase() } ?: MainCurrencyType.FIAT
        return foundType
    }

    fun saveMainCurrency(type: MainCurrencyType) {
        val sp = requireContext().getSharedPreferences("SendScreen", Context.MODE_PRIVATE)
        sp.edit().putString("mainCurrency", type.name).apply()
    }

    override fun onDestroy() {
        keyboardObserver.unregisterListener()
        store.dispatch(ReleaseSendState)
        super.onDestroy()
    }
}

@ExperimentalCoroutinesApi
fun EditText.inputtedTextAsFlow(): Flow<String> = callbackFlow {
    val watcher = addTextChangedListener { editable -> offer(editable?.toString() ?: "") }
    awaitClose { removeTextChangedListener(watcher) }
}

class FeeUiHelper {
    companion object {
        fun feeToId(fee: FeeType): Int {
            return when (fee) {
                FeeType.SINGLE -> 0
                FeeType.LOW -> R.id.chipLow
                FeeType.NORMAL -> R.id.chipNormal
                FeeType.PRIORITY -> R.id.chipPriority
            }
        }

        fun idToFee(id: Int): FeeType {
            return when (id) {
                R.id.chipLow -> FeeType.LOW
                R.id.chipNormal -> FeeType.NORMAL
                R.id.chipPriority -> FeeType.PRIORITY
                else -> FeeType.NORMAL
            }
        }
    }
}

private fun ToggleWidget.setupSendButtonStateModifiers(context: Context) {
    mainViewModifiers.clear()
    mainViewModifiers.add(ReplaceTextStateModifier(context.getString(R.string.send_btn_send), ""))
    mainViewModifiers.add(
            TextViewDrawableStateModifier(
                    context.getDrawableCompat(R.drawable.ic_arrow_right), null, TextViewDrawableStateModifier.RIGHT
            ))
    mainViewModifiers.add(ClickableStateModifier())
    toggleViewModifiers.clear()
    toggleViewModifiers.add(ShowHideStateModifier())
}

