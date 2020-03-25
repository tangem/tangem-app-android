package com.tangem.tangem_sdk_new

import android.animation.ObjectAnimator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.CardManagerDelegate
import com.tangem.Log
import com.tangem.LoggerInterface
import com.tangem.common.CompletionResult
import com.tangem.tangem_sdk_new.extensions.hide
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.nfc.NfcReader
import com.tangem.tangem_sdk_new.ui.NfcEnableDialog
import com.tangem.tangem_sdk_new.ui.TouchCardAnimation
import com.tangem.tasks.TaskError
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.main.nfc_bottom_sheet.*

/**
 * Default implementation of [CardManagerDelegate].
 * If no customisation is required, this is the preferred way to use Tangem SDK.
 */
class DefaultCardManagerDelegate(private val reader: NfcReader) : CardManagerDelegate {

    lateinit var activity: FragmentActivity
    private var readingDialog: BottomSheetDialog? = null
    private var nfcEnableDialog: NfcEnableDialog? = null

    init {
        setLogger()
    }

    override fun onNfcSessionStarted(cardId: String?) {
        reader.readingCancelled = false
        postUI { showReadingDialog(activity, cardId) }
        if (!reader.nfcEnabled) showNFCEnableDialog()
    }

    private fun showReadingDialog(activity: FragmentActivity, cardId: String?) {
        val dialogView = activity.layoutInflater.inflate(R.layout.nfc_bottom_sheet, null)
        readingDialog = BottomSheetDialog(activity)
        readingDialog?.setContentView(dialogView)
        readingDialog?.dismissWithAnimation = true
        readingDialog?.create()
        readingDialog?.setOnShowListener {
            readingDialog?.rippleBackgroundNfc?.startRippleAnimation()
            val nfcDeviceAntenna = TouchCardAnimation(
                    activity, readingDialog!!.ivHandCardHorizontal,
                    readingDialog!!.ivHandCardVertical, readingDialog!!.llHand, readingDialog!!.llNfc)
            nfcDeviceAntenna.init()
            if (cardId != null) {
                readingDialog?.tvCard?.visibility = View.VISIBLE
                readingDialog?.tvCardId?.visibility = View.VISIBLE
                readingDialog?.tvCardId?.text = cardId
            }
        }
        readingDialog?.setOnCancelListener {
            reader.readingCancelled = true
            reader.closeSession()
            Log.i(this::class.simpleName!!, "readingCancelled is set to true")
        }
        readingDialog?.show()
    }

    private fun showNFCEnableDialog() {
        nfcEnableDialog = NfcEnableDialog()
        activity.supportFragmentManager.let { nfcEnableDialog?.show(it, NfcEnableDialog.TAG) }
    }

    override fun onSecurityDelay(ms: Int, totalDurationSeconds: Int) {
        postUI {
            readingDialog?.lTouchCard?.hide()
            readingDialog?.tvRemainingTime?.text = ms.div(100).toString()
            readingDialog?.flSecurityDelay?.show()
            readingDialog?.tvTaskTitle?.text = activity.getText(R.string.dialog_security_delay)
            readingDialog?.tvTaskText?.text =
                    activity.getText(R.string.dialog_security_delay_description)

            performHapticFeedback()

            if (readingDialog?.pbSecurityDelay?.max != totalDurationSeconds) {
                readingDialog?.pbSecurityDelay?.max = totalDurationSeconds
            }
            readingDialog?.pbSecurityDelay?.progress = totalDurationSeconds - ms + 100

            val animation = ObjectAnimator.ofInt(
                    readingDialog?.pbSecurityDelay,
                    "progress",
                    totalDurationSeconds - ms,
                    totalDurationSeconds - ms + 100)
            animation.duration = 500
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        }
    }

    override fun onDelay(total: Int, current: Int, step: Int) {
        postUI {
            readingDialog?.lTouchCard?.hide()
            readingDialog?.flSecurityDelay?.show()
            readingDialog?.tvRemainingTime?.text = (((total - current) / step) + 1).toString()
            readingDialog?.tvTaskTitle?.text = "Operation in process"
            readingDialog?.tvTaskText?.text = "Please hold the card firmly until the operation is completed…"

            performHapticFeedback()

            if (readingDialog?.pbSecurityDelay?.max != total) {
                readingDialog?.pbSecurityDelay?.max = total
            }
            readingDialog?.pbSecurityDelay?.progress = current

            val animation = ObjectAnimator.ofInt(
                    readingDialog?.pbSecurityDelay,
                    "progress",
                    current,
                    current + step)
            animation.duration = 300
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        }
    }

    override fun onTagLost() {
        postUI {
            readingDialog?.lTouchCard?.show()
            readingDialog?.flSecurityDelay?.hide()
            readingDialog?.tvTaskTitle?.text = activity.getText(R.string.dialog_ready_to_scan)
            readingDialog?.tvTaskText?.text = activity.getText(R.string.dialog_scan_text)
        }
    }

    override fun onNfcSessionCompleted() {
        postUI {
            readingDialog?.lTouchCard?.hide()
            readingDialog?.flSecurityDelay?.hide()
            readingDialog?.flCompletion?.show()
            readingDialog?.ivCompletion?.setImageDrawable(activity.getDrawable(R.drawable.ic_done_135dp))
            performHapticFeedback()
        }
        postUI(300) { readingDialog?.dismiss() }
    }

    override fun onError(error: TaskError) {
        postUI {
            readingDialog?.lTouchCard?.hide()
            readingDialog?.flSecurityDelay?.hide()
            readingDialog?.flCompletion?.hide()
            readingDialog?.flError?.show()
            readingDialog?.tvTaskTitle?.text = activity.getText(R.string.dialog_error)
            readingDialog?.tvTaskText?.text = "${error::class.simpleName}: ${error.code}"
            performHapticFeedback()
        }
    }

    override fun onPinRequested(callback: (result: CompletionResult<String>) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun performHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            readingDialog?.llHeader?.isHapticFeedbackEnabled = true
            readingDialog?.llHeader?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun setLogger() {
        Log.setLogger(
                object : LoggerInterface {
                    override fun i(logTag: String, message: String) {
                        android.util.Log.i(logTag, message)
                    }

                    override fun e(logTag: String, message: String) {
                        android.util.Log.e(logTag, message)
                    }

                    override fun v(logTag: String, message: String) {
                        android.util.Log.v(logTag, message)
                    }
                }
        )
    }
}