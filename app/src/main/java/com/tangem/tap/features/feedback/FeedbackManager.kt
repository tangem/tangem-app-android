package com.tangem.tap.features.feedback

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tangem.Log
import com.tangem.TangemSdkLogger
import com.tangem.blockchain.common.*
import com.tangem.common.card.Card
import com.tangem.tap.common.extensions.sendEmail
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.domain.TapWorkarounds
import com.tangem.tap.features.feedback.EmailData.Companion.appendBlankLine
import com.tangem.tap.features.feedback.EmailData.Companion.appendDelimiter
import com.tangem.wallet.R
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*


/**
[REDACTED_AUTHOR]
 */
class FeedbackManager(
    val infoHolder: AdditionalEmailInfo,
    private val logCollector: TangemLogCollector,
) {

    private lateinit var activity: Activity

    fun updateActivity(activity: Activity) {
        this.activity = activity
    }

    fun send(emailData: EmailData, onFail: ((Exception) -> Unit)? = null) {
        if (!this::activity.isInitialized) return

        emailData.prepare(infoHolder)
        val fileLog = if (emailData is ScanFailsEmail) createLogFile() else null
        activity.sendEmail(
            email = getSupportEmail(),
            subject = activity.getString(emailData.subjectResId),
            message = emailData.joinTogether(activity, infoHolder),
            file = fileLog,
            onFail = onFail
        )
    }

    private fun getSupportEmail(): String {
        return if (TapWorkarounds.isStart2CoinIssuer(infoHolder.cardIssuer)) {
            S2C_SUPPORT_EMAIL
        } else {
            DEFAULT_SUPPORT_EMAIL
        }
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

    companion object {
        const val DEFAULT_SUPPORT_EMAIL = "support@tangem.com"
        const val S2C_SUPPORT_EMAIL = "cardsupport@start2coin.com"
    }
}

class TangemLogCollector : TangemSdkLogger {
    private val dateFormatter = SimpleDateFormat("HH:mm:ss.SSS")
    private val logs = mutableListOf<String>()

    override fun log(message: () -> String, level: Log.Level) {
        val time = dateFormatter.format(Date())
        logs.add("$time: ${message()}\n")
    }

    fun getLogs(): List<String> = logs.toList()

    fun clearLogs() {
        logs.clear()
    }
}

class AdditionalEmailInfo {
    class EmailWalletInfo(
        var blockchain: Blockchain = Blockchain.Unknown,
        var address: String = "",
        var explorerLink: String = "",
        var host: String = "",
        //    var outputsCount: String = ""
        //    var transactionHex: String = ""
    )

    var appVersion: String = ""

    // card
    var cardId: String = ""
    var cardFirmwareVersion: String = ""
    var cardIssuer: String = ""

    // wallets
    internal val walletsInfo = mutableListOf<EmailWalletInfo>()
    internal val tokens = mutableMapOf<Blockchain, Collection<Token>>()
    internal var onSendErrorWalletInfo: EmailWalletInfo? = null
    var signedHashesCount: String = ""

    // device
    var phoneModel: String = Build.MODEL
    var osVersion: String = Build.VERSION.SDK_INT.toString()

    // send error
    var destinationAddress: String = ""
    var amount: String = ""
    var fee: String = ""
    var token: String = ""

    fun setAppVersion(context: Context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    fun setCardInfo(card: Card) {
        cardId = card.cardId
        cardFirmwareVersion = card.firmwareVersion.stringValue
        cardIssuer = card.issuer.name
        signedHashesCount = card.wallets
            .joinToString(";") { "${it.curve?.curve} - ${it.totalSignedHashes}" }
    }

    fun setWalletsInfo(walletManagers: List<WalletManager>) {
        walletsInfo.clear()
        tokens.clear()
        walletManagers.forEach { manager ->
            walletsInfo.add(
                EmailWalletInfo(
                    blockchain = manager.wallet.blockchain,
                    address = getAddress(manager.wallet),
                    explorerLink = getExploreUri(manager.wallet),
                    host = manager.currentHost
                )
            )
            if (manager.cardTokens.isNotEmpty()) {
                tokens[manager.wallet.blockchain] = manager.cardTokens
            }
        }
    }

    fun updateOnSendError(wallet: Wallet, host: String, amountToSend: Amount, feeAmount: Amount, destinationAddress: String) {
        onSendErrorWalletInfo = EmailWalletInfo(
            blockchain = wallet.blockchain,
            address = getAddress(wallet),
            explorerLink = getExploreUri(wallet),
            host = host
        )

        this.destinationAddress = destinationAddress
        amount = amountToSend.value?.stripZeroPlainString() ?: "0"
        fee = feeAmount.value?.stripZeroPlainString() ?: "0"
        token = if (amountToSend.type is AmountType.Token) amountToSend.currencySymbol else ""
    }

    private fun getAddress(wallet: Wallet): String {
        return if (wallet.addresses.size == 1) {
            wallet.address
        } else {
            val addresses = wallet.addresses.joinToString(", ") {
                "${it.type.javaClass.simpleName} - ${it.value}"
            }
            "Multiple address: $addresses"
        }
    }

    private fun getExploreUri(wallet: Wallet): String {
        return if (wallet.addresses.size == 1) {
            wallet.getExploreUrl(wallet.address)
        } else {
            val links = wallet.addresses.joinToString(", ") {
                "${it.type.javaClass.simpleName} - ${wallet.getExploreUrl(it.value)}"
            }
            "Multiple explorers links: $links"
        }
    }
}

interface EmailData {
    val subjectResId: Int
    val mainMessageResId: Int

    fun getDataCollectionMessageResId(): Int = R.string.feedback_data_collection_message

    fun prepare(infoHolder: AdditionalEmailInfo) {}

    fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String

    fun joinTogether(context: Context, infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            append(context.getString(mainMessageResId))
            append("\n\n\n\n")
            append(context.getString(getDataCollectionMessageResId()))
            append(createOptionalMessage(infoHolder))
        }.toString()
    }

    companion object {
        internal fun StringBuilder.appendDelimiter() = append("----------\n")
        internal fun StringBuilder.appendBlankLine() = append("\n")
    }
}

class RateCanBeBetterEmail : EmailData {
    override val subjectResId: Int = R.string.feedback_subject_rate_negative
    override val mainMessageResId: Int = R.string.feedback_preface_rate_negative

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        val walletInfo = infoHolder.walletsInfo[0]
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
            appendBlankLine()
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
        }.toString()
    }
}

class ScanFailsEmail : EmailData {

    override val subjectResId: Int = R.string.feedback_subject_scan_failed
    override val mainMessageResId: Int = R.string.feedback_preface_scan_failed

    override fun joinTogether(context: Context, infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            append(context.getString(mainMessageResId))
            append("\n\n\n\n")
            append(createOptionalMessage(infoHolder))
        }.toString()
    }

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        return StringBuilder().apply {
            appendBlankLine()
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
        }.toString()
    }
}

class SendTransactionFailedEmail(private val error: String) : EmailData {
    override val subjectResId: Int = R.string.feedback_subject_tx_failed
    override val mainMessageResId: Int = R.string.feedback_preface_tx_failed

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        val walletInfo = infoHolder.onSendErrorWalletInfo ?: AdditionalEmailInfo.EmailWalletInfo()
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
            appendKeyValue("Signed hashes", infoHolder.signedHashesCount)
            appendDelimiter()
            appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
            appendKeyValue("Host", walletInfo.host)
            appendKeyValue("Token", infoHolder.token)
            appendKeyValue("Error", error)
            appendDelimiter()
            appendKeyValue("Source address", walletInfo.address)
            appendKeyValue("Destination address", infoHolder.destinationAddress)
            appendKeyValue("Amount", infoHolder.amount)
            appendKeyValue("Fee", infoHolder.fee)
            appendBlankLine()
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
//            appendKeyValue("Transaction HEX", infoHolder.transactionHex)
        }.toString()
    }
}

class FeedbackEmail : EmailData {
    override val subjectResId: Int
        get() = if (isS2CCard) s2cSubject else tangemSubject
    override val mainMessageResId: Int
        get() = if (isS2CCard) s2cMainMessage else tangemMainMessage

    private val tangemSubject: Int = R.string.feedback_subject_support_tangem
    private val tangemMainMessage: Int = R.string.feedback_preface_support

    private val s2cSubject: Int = R.string.feedback_subject_support
    private val s2cMainMessage: Int = R.string.feedback_preface_support

    private var isS2CCard = false

    override fun prepare(infoHolder: AdditionalEmailInfo) {
        isS2CCard = TapWorkarounds.isStart2CoinIssuer(infoHolder.cardIssuer)
    }

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        val builder = StringBuilder()
        builder.appendKeyValue("Card ID", infoHolder.cardId)
        builder.appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
        builder.appendKeyValue("Signed hashes", infoHolder.signedHashesCount)

        infoHolder.walletsInfo.forEach {
            builder.appendDelimiter()
            builder.appendKeyValue("Blockchain", it.blockchain.fullName)
            builder.appendKeyValue("Host", it.host)
            builder.appendKeyValue("Wallet address", it.address)
            builder.appendKeyValue("Explorer link", it.explorerLink)
        }
        builder.appendBlankLine()

        infoHolder.tokens.forEach { tokens ->
            builder.appendDelimiter()
            builder.appendKeyValue("Blockchain", tokens.key.fullName)
            builder.appendKeyValue("Tokens", tokens.value.map { "${it.name} - ${it.symbol}" }.toString())
        }
        builder.appendDelimiter()
        builder.appendBlankLine()
//            appendKeyValue("Outputs count", infoHolder.outputsCount)
        builder.appendKeyValue("Phone model", infoHolder.phoneModel)
        builder.appendKeyValue("OS version", infoHolder.osVersion)
        builder.appendKeyValue("App version", infoHolder.appVersion)

        return builder.toString()
    }
}

private fun StringBuilder.appendKeyValue(key: String, value: String): StringBuilder {
    return if (value.isNotBlank()) this.append("$key: $value\n") else this
}