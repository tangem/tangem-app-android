package com.tangem.tap.features.feedback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.common.Blockchain
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.StringWriter


/**
[REDACTED_AUTHOR]
 */
class FeedbackManager(
        val infoHolder: AdditionalEmailInfo,
        private val logCollector: TangemLogCollector,
        private val email: String = "support@tangem.com",
) {

    private lateinit var activity: Activity

    fun updateAcivity(activity: Activity) {
        this.activity = activity
    }

    fun send(emailData: EmailData) {
        val fileLog = if (emailData is ScanFailsEmail) createLogFile() else null
        sendTo(email, emailData.subject, emailData.joinTogether(infoHolder), fileLog)
    }

    private fun sendTo(email: String, subject: String, message: String, fileLog: File? = null) {
        val emailFilterIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        val originalIntentResults = activity.packageManager.queryIntentActivities(emailFilterIntent, 0)
        val emailFilterIntentResults = activity.packageManager.queryIntentActivities(emailFilterIntent, 0)
        val targetedIntents = originalIntentResults
                .filter { originalResult ->
                    emailFilterIntentResults.any {
                        originalResult.activityInfo.packageName == it.activityInfo.packageName
                    }
                }
                .map {
                    createEmailShareIntent(email, subject, message, fileLog).apply {
                        setPackage(it.activityInfo.packageName)
                    }
                }
                .toMutableList()
        try {
            val chooserIntent = Intent.createChooser(targetedIntents.removeAt(0), "Send mail...")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(activity, chooserIntent, null)
        } catch (ex: ActivityNotFoundException) {
            Timber.e(ex)
        }
    }

    private fun createEmailShareIntent(recipient: String, subject: String, text: String, file: File? = null): Intent {
        val builder = ShareCompat.IntentBuilder.from(activity)
                .setType("message/rfc822")
                .setEmailTo(arrayOf(recipient))
                .setSubject(subject)
                .setText(text)
        file?.let { builder.setStream(FileProvider.getUriForFile(activity, "${activity.packageName}.provider", it)) }
        return builder.intent
    }

    private fun createLogFile(): File? {
        return try {
            val file = File(activity.filesDir, "logs.txt")
            file.delete()
            file.createNewFile()

            val stringWriter = StringWriter()
            logCollector.getLogs().forEach { stringWriter.append(it) }
            val fileWriter = FileWriter(file)
            fileWriter.write(stringWriter.toString())
            fileWriter.close()
            logCollector.clearLogs()
            file
        } catch (ex: Exception) {
            Timber.e(ex, "Can't create a file for email attachment")
            null
        }
    }
}

class TangemLogCollector : TangemSdkLogger {
    private val logs = mutableListOf<String>()

    override fun log(message: () -> String, level: Log.Level) {
        logs.add(message())
    }

    fun getLogs(): List<String> = logs.toList()

    fun clearLogs() {
        logs.clear()
    }
}

class AdditionalEmailInfo {
    var cardId: String = ""
    var cardFirmwareVersion: String = ""
    var blockchain: Blockchain = Blockchain.Unknown

    var phoneModel: String = Build.MODEL
    var osVersion: String = Build.VERSION.SDK_INT.toString()
    var appVersion: String = ""

    var token: String = ""
    var sourceAddress: String = ""
    var destinationAddress: String = ""
    var amount: String = ""
    var fee: String = ""

    //    var transactionHex: String = ""
    var signedHashesCount: String = ""
    var explorerLink: String = ""
//    var outputsCount: String = ""

    fun updateAppVersion(context: Context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
}

interface EmailData {
    val subject: String
    val mainMessage: String
    fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String

    fun joinTogether(infoHolder: AdditionalEmailInfo): String {
        return "$mainMessage\n\n\n\n\n" +
                "Following information is optional. You can erase it if you don’t want to share it.\n" +
                createOptionalMessage(infoHolder)
    }
}

class RateCanBeBetterEmail : EmailData {
    override val subject: String = "My suggestions"
    override val mainMessage: String = "Tell us what functions you are missing, and we will try to help you."

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Blockchain", infoHolder.blockchain.fullName)
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
        }.toString()
    }
}

class ScanFailsEmail : EmailData {
    override val subject: String = "Can’t scan a card"
    override val mainMessage: String = "Please tell us what card do you have?"
    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
        }.toString()
    }
}

class SendTransactionFailedEmail(private val error: String) : EmailData {
    override val subject: String = "Can’t send a transaction"
    override val mainMessage: String = "Please tell us more about your issue. Every small detail can help."
    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            appendKeyValue("Error", error)
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Blockchain", infoHolder.blockchain.fullName)
            appendKeyValue("Token", infoHolder.token)
            appendKeyValue("Source address", infoHolder.sourceAddress)
            appendKeyValue("Destination address", infoHolder.destinationAddress)
            appendKeyValue("Amount", infoHolder.amount)
            appendKeyValue("Fee", infoHolder.fee)
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
            appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
//            appendKeyValue("Transaction HEX", infoHolder.transactionHex)
        }.toString()
    }
}

class FeedbackEmail : EmailData {
    override val subject: String = "Tangem Tap feedback"
    override val mainMessage: String = "Hi Tangem,"
    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
            appendKeyValue("Signed hashes", infoHolder.signedHashesCount)
            appendKeyValue("Blockchain", infoHolder.blockchain.fullName)
            appendKeyValue("Wallet address", infoHolder.sourceAddress)
            appendKeyValue("Explorer link", infoHolder.explorerLink)
//            appendKeyValue("Outputs count", infoHolder.outputsCount)
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
        }.toString()
    }
}

fun StringBuilder.appendKeyValue(key: String, value: String): StringBuilder {
    return this.append("$key: $value\n")
}