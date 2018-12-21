package com.tangem.presentation.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_wait_pin2.*
import java.util.*

class WaitSecurityDelayDialogNew : AppCompatDialogFragment() {

    companion object {
        val TAG: String = WaitSecurityDelayDialogNew::class.java.simpleName

        private const val MIN_REMAINING_DELAY_TO_SHOW_DIALOG = 1000
        private const val DELAY_BEFORE_SHOW_DIALOG = 5000
    }

    private var msTimeout = 60000
    private var msProgress = 0
    private var timer: Timer? = null
    private var timerToShowDelayDialog: Timer? = null
    private var instance: WaitSecurityDelayDialogNew? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity!!.layoutInflater
        val v = inflater.inflate(R.layout.dialog_wait_pin2, null)

        progressBar.max = msTimeout
        progressBar.progress = msProgress

        timer = Timer()
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                progressBar.post {
                    val progress = this@WaitSecurityDelayDialogNew.progressBar.progress
                    if (progress < this@WaitSecurityDelayDialogNew.progressBar.max) {
                        this@WaitSecurityDelayDialogNew.progressBar.progress = progress + 1000
                    }
                }
            }
        }, 1000, 1000)

        return AlertDialog.Builder(activity)
                .setIcon(R.drawable.tangem_logo_small_new)
                .setTitle(R.string.security_delay)
                .setView(v)
                .setCancelable(false)
                .create()
    }

    fun onReadBeforeRequest(activity: Activity, timeout: Int) {
        activity.runOnUiThread(Runnable {
            if (timerToShowDelayDialog != null || timeout < DELAY_BEFORE_SHOW_DIALOG + MIN_REMAINING_DELAY_TO_SHOW_DIALOG)
                return@Runnable

            timerToShowDelayDialog = Timer()
            timerToShowDelayDialog!!.schedule(object : TimerTask() {
                override fun run() {
                    if (instance != null) return
                    instance = WaitSecurityDelayDialogNew()
                    instance!!.setup(timeout, DELAY_BEFORE_SHOW_DIALOG)
                    instance!!.isCancelable = false
//                    instance.show(activity.fragmentManager, TAG)
                }
            }, DELAY_BEFORE_SHOW_DIALOG.toLong())
        })
    }

    fun onReadAfterRequest(activity: Activity) {
        activity.runOnUiThread(Runnable {
            if (timerToShowDelayDialog == null) return@Runnable
            timerToShowDelayDialog!!.cancel()
            timerToShowDelayDialog = null
        })
    }

    fun onReadWait(activity: Activity, msec: Int) {
        activity.runOnUiThread(Runnable {
            if (timerToShowDelayDialog != null) {
                timerToShowDelayDialog!!.cancel()
                timerToShowDelayDialog = null
            }

            if (msec == 0) {
                if (instance != null) {
                    instance!!.dismiss()
                    instance = null
                }
                return@Runnable
            }

            if (instance == null) {
                if (msec > MIN_REMAINING_DELAY_TO_SHOW_DIALOG) {
                    instance = WaitSecurityDelayDialogNew()
                    // 1000ms - card delay notification interval
                    instance!!.setup(msec + 1000, 1000)
                    instance!!.isCancelable = false
//                    instance.show(activity.fragmentManager, TAG)
                }
            } else {
                instance!!.setRemainingTimeout(msec)
            }
        })
    }

    private fun setup(msTimeout: Int, msProgress: Int) {
        this.msTimeout = msTimeout
        this.msProgress = msProgress
    }

    private fun setRemainingTimeout(msec: Int) {
        progressBar.post {
            val progress = this@WaitSecurityDelayDialogNew.progressBar.progress
            if (timer != null) {
                // we get delay latency from card for first time - don't change progress by timer, only by card answer
                progressBar.max = progress + msec
                timer!!.cancel()
                timer = null
            } else {
                val newProgress = progressBar.max - msec
                if (newProgress > progress)
                    progressBar.progress = newProgress
                else
                    progressBar.max = progress + msec
            }
        }
    }

}