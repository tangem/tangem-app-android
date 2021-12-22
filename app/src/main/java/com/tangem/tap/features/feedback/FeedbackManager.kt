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
            subject = emailData.subject, message = emailData.joinTogether(infoHolder, emailData !is ScanFailsEmail),
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
    val subject: String
    val mainMessage: String

    fun prepare(infoHolder: AdditionalEmailInfo) {}

    fun appendDelimiter(builder: StringBuilder) {
        builder.append("----------\n")
    }

    fun appendBlankLine(builder: StringBuilder) {
        builder.append("\n")
    }

    fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String

    fun joinTogether(infoHolder: AdditionalEmailInfo, allowToErasePhoneInfo: Boolean): String {
        val allowToErasePhoneInfoDisclaimer = if (allowToErasePhoneInfo) {
            "Following information is optional. You can erase it if you don’t want to share it.\n\n"
        } else {
            ""
        }
        return "$mainMessage\n\n\n\n" +
                allowToErasePhoneInfoDisclaimer +
                createOptionalMessage(infoHolder)
    }
}

class RateCanBeBetterEmail : EmailData {
    override val subject: String = "My suggestions"
    override val mainMessage: String = "Tell us what functions you are missing, and we will try to help you."

    override fun createOptionalMessage(infoHolder: AdditionalEmailInfo): String {
        val walletInfo = infoHolder.walletsInfo[0]
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
            appendBlankLine(this)
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
            appendBlankLine(this)
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
        val walletInfo = infoHolder.onSendErrorWalletInfo ?: AdditionalEmailInfo.EmailWalletInfo()
        return StringBuilder().apply {
            appendKeyValue("Card ID", infoHolder.cardId)
            appendKeyValue("Firmware version", infoHolder.cardFirmwareVersion)
            appendKeyValue("Signed hashes", infoHolder.signedHashesCount)
            appendDelimiter(this)
            appendKeyValue("Blockchain", walletInfo.blockchain.fullName)
            appendKeyValue("Host", walletInfo.host)
            appendKeyValue("Token", infoHolder.token)
            appendKeyValue("Error", error)
            appendDelimiter(this)
            appendKeyValue("Source address", walletInfo.address)
            appendKeyValue("Destination address", infoHolder.destinationAddress)
            appendKeyValue("Amount", infoHolder.amount)
            appendKeyValue("Fee", infoHolder.fee)
            appendBlankLine(this)
            appendKeyValue("Phone model", infoHolder.phoneModel)
            appendKeyValue("OS version", infoHolder.osVersion)
            appendKeyValue("App version", infoHolder.appVersion)
//            appendKeyValue("Transaction HEX", infoHolder.transactionHex)
        }.toString()
    }
}

class FeedbackEmail : EmailData {
    override val subject: String
        get() = if (isS2CCard) s2cSubject else tangemSubject
    override val mainMessage: String
        get() = if (isS2CCard) s2cMainMessage else tangemMainMessage

    private val tangemSubject = "Tangem feedback"
    private val tangemMainMessage = "Hi support team,"

    private val s2cSubject = "Feedback"
    private val s2cMainMessage = "Hi support team,"

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
            appendDelimiter(builder)
            builder.appendKeyValue("Blockchain", it.blockchain.fullName)
            builder.appendKeyValue("Host", it.host)
            builder.appendKeyValue("Wallet address", it.address)
            builder.appendKeyValue("Explorer link", it.explorerLink)
        }
        appendBlankLine(builder)

        infoHolder.tokens.forEach { tokens ->
            appendDelimiter(builder)
            builder.appendKeyValue("Blockchain", tokens.key.fullName)
            builder.appendKeyValue("Tokens", tokens.value.map { "${it.name} - ${it.symbol}" }.toString())
        }
        appendDelimiter(builder)
        appendBlankLine(builder)

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