package com.tangem.tangemtest

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardManager
import com.tangem.commands.ReadUserDataResponse
import com.tangem.commands.WriteUserDataResponse
import com.tangem.common.CardEnvironment
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.a_test_user_data.*
import java.nio.charset.StandardCharsets

/**
[REDACTED_AUTHOR]
 */
class TestUserDataActivity: AppCompatActivity() {

  private lateinit var cardManager: CardManager
  private lateinit var writeOptions: WriteOptions

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.a_test_user_data)

    init()
    initWriteOptions()
  }

  private fun init() {
    cardManager = CardManager.init(this)

    btn_scan?.setOnClickListener { _ ->
      cardManager.scanCard { taskEvent ->
        when (taskEvent) {
          is TaskEvent.Event -> {
            when (taskEvent.data) {
              is ScanEvent.OnReadEvent -> {
                // Handle returned card data
                writeOptions.cardId = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId
                runOnUiThread { showReadWriteSection(true) }
              }
              is ScanEvent.OnVerifyEvent -> {
                //Handle card verification
              }
            }
          }
          is TaskEvent.Completion -> {
            if (taskEvent.error != null) {
              if (taskEvent.error is TaskError.UserCancelled) {
                // Handle case when user cancelled manually
              }
              // Handle other errors
            }
            // Handle completion
          }
        }
      }
    }

    btn_write.setOnClickListener {
      if (writeOptions.cardId == null) return@setOnClickListener

      cardManager.writeUserData(
          writeOptions.cardId !!,
          writeOptions.userData,
          writeOptions.userProtectedData,
          writeOptions.userCounter,
          writeOptions.userProtectedCounter
      ) {
        when (it) {
          is TaskEvent.Completion -> handleError(tv_write_result, it.error)
          is TaskEvent.Event -> {
            runOnUiThread {
              val data = it.data as? WriteUserDataResponse
              if (data == null) {
                tv_write_result.text = "Response doesn't match"
                return@runOnUiThread
              }
              tv_write_result?.text = "Success"
            }
          }
        }
      }
    }

    btn_read.setOnClickListener {
      if (writeOptions.cardId == null) return@setOnClickListener

      cardManager.readUserData(writeOptions.cardId !!) {
        when (it) {
          is TaskEvent.Completion -> handleError(tv_read_result, it.error)
          is TaskEvent.Event -> {
            runOnUiThread {
              val data = it.data as? ReadUserDataResponse
              if (data == null) {
                tv_read_result.text = "Response doesn't match"
                return@runOnUiThread
              }
              tv_read_result?.text = "Success"

              writeOptions.userData = data.userData
              writeOptions.userProtectedData = data.userProtectedData
              writeOptions.userCounter = data.userCounter
              writeOptions.userProtectedCounter = data.userProtectedCounter

              tv_card_cid.text = data.cardId
              tv_data.text = String(data.userData, StandardCharsets.US_ASCII)
              tv_protected_data.text = String(data.userProtectedData, StandardCharsets.US_ASCII)
              tv_counter.text = data.userCounter.toString()
              tv_protected_counter.text = data.userProtectedCounter.toString()
            }

          }
        }
      }
    }
  }

  private fun handleError(tv: TextView, error: TaskError?) {
    val er = error ?: return
    if (er is TaskError.UserCancelled) return

    runOnUiThread { tv.text = er::class.simpleName }
  }

  private fun initWriteOptions() {
    writeOptions = WriteOptions()

    chb_with_ud.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateData(buttonView) }
    chb_with_ud_protected.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateProtectedData(buttonView) }
    chb_with_counter.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateCounter(buttonView) }
    chb_with_protected_counter.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updateProtectedCounter(buttonView) }
    chb_with_pin2.setOnCheckedChangeListener { buttonView, isChecked -> writeOptions.updatePin2(buttonView) }
  }

  private fun showReadWriteSection(show: Boolean) {
    val state = if (show) View.VISIBLE else View.GONE
    cl_read_write.visibility = state
  }
}

class WriteOptions {
  var cardId: String? = null
  var userData: ByteArray? = null
  var userProtectedData: ByteArray? = null
  var userCounter: Int? = null
  var userProtectedCounter: Int? = null
  var pin2: String? = null

  fun updateData(chbx: CompoundButton) {
    val value = "simple user data".toByteArray()
    userData = if (chbx.isChecked) value else null
  }

  fun updateProtectedData(chbx: CompoundButton) {
    val value = "protected user data".toByteArray()
    userProtectedData = if (chbx.isChecked) value else null
  }

  fun updateCounter(chbx: CompoundButton) {
    val value = if (userCounter == null) 0 else userCounter !! + 1
    userCounter = if (chbx.isChecked) value else null
  }

  fun updateProtectedCounter(chbx: CompoundButton) {
    val value = if (userProtectedCounter == null) 0 else userProtectedCounter !! + 1
    userProtectedCounter = if (chbx.isChecked) value else null
  }

  fun updatePin2(chbx: CompoundButton) {
    val value = CardEnvironment.DEFAULT_PIN2
    pin2 = if (chbx.isChecked) value else null
  }
}