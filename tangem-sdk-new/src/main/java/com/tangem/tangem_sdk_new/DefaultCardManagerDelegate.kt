package com.tangem.tangem_sdk_new

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.CardManagerDelegate
import com.tangem.Log
import com.tangem.LoggerInterface
import com.tangem.common.CompletionResult
import com.tangem.tangem_sdk_new.nfc.NfcReader
import com.tangem.tangem_sdk_new.ui.NfcEnableDialog
import com.tangem.tangem_sdk_new.ui.TouchCardAnimation
import com.tangem.tasks.TaskError
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.main.nfc_bottom_sheet.*


class DefaultCardManagerDelegate(private val reader: NfcReader) : CardManagerDelegate {

    lateinit var activity: FragmentActivity
    private var readingDialog: BottomSheetDialog? = null
    private var nfcEnableDialog: NfcEnableDialog? = null

    init {
        setLogger()
    }

    override fun onTaskStarted() {
//        reader.isoDep = null
        reader.readingCancelled = false
        postUI { showReadingDialog(activity) }
        if (!reader.nfcEnabled) showNFCEnableDialog()
    }

    private fun showReadingDialog(activity: FragmentActivity) {
        val dialogView = activity.getLayoutInflater().inflate(R.layout.nfc_bottom_sheet, null)
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
        }
        readingDialog?.setOnCancelListener {
            reader.readingCancelled = true
//            reader.closeSession()
            Log.i(this::class.simpleName!!, "readingCancelled is set to true")
        }
        readingDialog?.show()
    }

    private fun showNFCEnableDialog() {
        nfcEnableDialog = NfcEnableDialog()
        activity.supportFragmentManager.let { nfcEnableDialog?.show(it, NfcEnableDialog.TAG) }
    }

    override fun showSecurityDelay(ms: Int) {
        postUI {
            readingDialog?.lTouchCard?.visibility = View.GONE
            readingDialog?.tvRemainingTime?.text = ms.div(100).toString()
            readingDialog?.flSecurityDelay?.visibility = View.VISIBLE
            readingDialog?.tvTaskTitle?.text = activity.getText(R.string.dialog_security_delay)
            readingDialog?.tvTaskText?.text =
                    activity.getText(R.string.dialog_security_delay_description)
        }
    }

    override fun onTaskCompleted() {
        reader.closeSession()
        postUI {
            readingDialog?.lTouchCard?.visibility = View.GONE
            readingDialog?.flSecurityDelay?.visibility = View.GONE
            readingDialog?.flCompletion?.visibility = View.VISIBLE
            readingDialog?.ivCompletion?.setImageDrawable(activity.getDrawable(R.drawable.ic_done_135dp))
        }
        postUI(300) { readingDialog?.dismiss() }
    }

    override fun onTaskError(error: TaskError?) {
        reader.closeSession()
        postUI {
            readingDialog?.lTouchCard?.visibility = View.GONE
            readingDialog?.flSecurityDelay?.visibility = View.GONE
            readingDialog?.flCompletion?.visibility = View.VISIBLE
            readingDialog?.ivCompletion?.setImageDrawable(activity.getDrawable(R.drawable.ic_error_outline_135dp))
            readingDialog?.tvTaskTitle?.text = activity.getText(R.string.dialog_error)
            readingDialog?.tvTaskText?.text = if (error != null) error::class.simpleName else ""
        }
    }

    override fun requestPin(callback: (result: CompletionResult<String>) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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