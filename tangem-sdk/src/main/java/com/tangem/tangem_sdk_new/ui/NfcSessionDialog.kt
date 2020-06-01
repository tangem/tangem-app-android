package com.tangem.tangem_sdk_new.ui

import android.animation.ObjectAnimator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.localizedDescription
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.postUI
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.main.nfc_bottom_sheet.*

class NfcSessionDialog(val activity: FragmentActivity) : BottomSheetDialog(activity) {

    private var currentState: SessionViewDelegateState? = null

    fun show(state: SessionViewDelegateState) {
        when (state) {
            is SessionViewDelegateState.Ready -> onReady(state)
            is SessionViewDelegateState.Success -> onSuccess(state)
            is SessionViewDelegateState.Error -> onError(state)
            is SessionViewDelegateState.SecurityDelay -> onSecurityDelay(state)
            is SessionViewDelegateState.Delay -> onDelay(state)
            is SessionViewDelegateState.PinRequested -> onPinRequested(state)
            is SessionViewDelegateState.TagLost -> onTagLost()
            is SessionViewDelegateState.TagConnected -> onTagConnected()
            is SessionViewDelegateState.WrongCard -> onWrongCard()
        }
        currentState = state
    }

    private fun onReady(state: SessionViewDelegateState.Ready) {
        show(lTouchCard)
        rippleBackgroundNfc?.startRippleAnimation()
        val nfcDeviceAntenna = TouchCardAnimation(
                activity, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc
        )
        nfcDeviceAntenna.init()
        state.cardId?.let { cardId ->
            tvCard?.show()
            tvCardId?.show()
            tvCardId?.text = cardId
        }
        state.message?.let { message ->
            if (message.body != null) tvTaskText?.text = message.body
            if (message.header != null) tvTaskTitle?.text = message.header
        }
    }

    private fun onSuccess(state: SessionViewDelegateState.Success) {
        show(flCompletion)
        ivCompletion?.setImageDrawable(activity.getDrawable(R.drawable.ic_done_135dp))
        state.message?.let { message ->
            if (message.body != null) tvTaskText?.text = message.body
            if (message.header != null) tvTaskTitle?.text = message.header
        }
        performHapticFeedback()
        postUI(300) { dismiss() }
    }

    private fun onError(state: SessionViewDelegateState.Error) {
        show(flError)
        tvTaskTitle?.text = activity.getText(R.string.dialog_error)
        tvTaskText?.text = activity.getString(
                R.string.error_message,
                state.error.code.toString(), activity.getString(state.error.localizedDescription())
        )
        performHapticFeedback()
    }

    private fun onSecurityDelay(state: SessionViewDelegateState.SecurityDelay) {
        show(flSecurityDelay)

        tvRemainingTime?.text = state.ms.div(100).toString()
        tvTaskTitle?.text = activity.getText(R.string.dialog_security_delay)
        tvTaskText?.text =
                activity.getText(R.string.dialog_security_delay_description)

        performHapticFeedback()

        if (pbSecurityDelay?.max != state.totalDurationSeconds) {
            pbSecurityDelay?.max = state.totalDurationSeconds
        }
        pbSecurityDelay?.progress = state.totalDurationSeconds - state.ms + 100

        val animation = ObjectAnimator.ofInt(
                pbSecurityDelay,
                "progress",
                state.totalDurationSeconds - state.ms,
                state.totalDurationSeconds - state.ms + 100)
        animation.duration = 500
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun onDelay(state: SessionViewDelegateState.Delay) {
        show(flSecurityDelay)
        tvRemainingTime?.text = (((state.total - state.current) / state.step) + 1).toString()
        tvTaskTitle?.text = "Operation in process"
        tvTaskText?.text = "Please hold the card firmly until the operation is completed…"

        performHapticFeedback()

        if (pbSecurityDelay?.max != state.total) {
            pbSecurityDelay?.max = state.total
        }
        pbSecurityDelay?.progress = state.current

        val animation = ObjectAnimator.ofInt(
                pbSecurityDelay,
                "progress",
                state.current,
                state.current + state.step)
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun onPinRequested(state: SessionViewDelegateState.PinRequested) {
        TODO("To be implemented")
    }

    private fun onTagLost() {
        if (currentState is SessionViewDelegateState.Success) return
        show(lTouchCard)
        tvTaskTitle?.text = activity.getText(R.string.dialog_ready_to_scan)
        tvTaskText?.text = activity.getText(R.string.dialog_scan_text)
    }

    private fun onTagConnected() {
        show(flReading)
    }

    private fun onWrongCard() {
        if (currentState !is SessionViewDelegateState.WrongCard) {
            show(flError)
            tvTaskTitle?.text = activity.getText(R.string.dialog_error)
            tvTaskText?.text = activity.getString(
                    R.string.error_message,
                    "Wrong Card", activity.getString(R.string.error_wrong_card_number)
            )
            performHapticFeedback()

            postUI (2000){
                show(lTouchCard)
                tvTaskTitle?.text = activity.getText(R.string.dialog_ready_to_scan)
                tvTaskText?.text = activity.getText(R.string.dialog_scan_text)
            }

        }
    }

    private fun show(view: View) {
        lTouchCard?.show(view.id == lTouchCard.id)
        flSecurityDelay?.show(view.id == flSecurityDelay.id)
        flReading?.show(view.id == flReading.id)
        flError?.show(view.id == flError.id)
        flCompletion?.show(view.id == flCompletion.id)
    }

    private fun performHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            llHeader?.isHapticFeedbackEnabled = true
            llHeader?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}