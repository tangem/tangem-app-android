package com.tangem.tap.common.feedback

import android.content.Context
import com.tangem.domain.common.TapWorkarounds
import com.tangem.tap.common.extensions.breakLine
import com.tangem.wallet.R

interface FeedbackData {
    val subjectResId: Int
    val mainMessageResId: Int

    fun getDataCollectionMessageResId(): Int = R.string.feedback_data_collection_message

    fun prepare(infoHolder: AdditionalFeedbackInfo) {}

    fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String

    @Suppress("MagicNumber")
    fun joinTogether(context: Context, infoHolder: AdditionalFeedbackInfo): String {
        return StringBuilder().apply {
            append(context.getString(mainMessageResId))
            breakLine(3)
            append(context.getString(getDataCollectionMessageResId()))
            breakLine()
            append(createOptionalMessage(infoHolder))
        }.toString()
    }
}

class RateCanBeBetterEmail : FeedbackData {
    override val subjectResId: Int = R.string.feedback_subject_rate_negative
    override val mainMessageResId: Int = R.string.feedback_preface_rate_negative

    override fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String = FeedbackDataBuilder(infoHolder)
        .appendCardInfo()
        .breakLine()
        .appendPhoneInfo()
        .build()
}

class ScanFailsEmail : FeedbackData {

    override val subjectResId: Int = R.string.feedback_subject_scan_failed
    override val mainMessageResId: Int = R.string.feedback_preface_scan_failed

    @Suppress("MagicNumber")
    override fun joinTogether(context: Context, infoHolder: AdditionalFeedbackInfo): String = StringBuilder().apply {
        append(context.getString(mainMessageResId))
        breakLine(4)
        append(createOptionalMessage(infoHolder))
    }.toString()

    override fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String = FeedbackDataBuilder(infoHolder)
        .appendPhoneInfo()
        .build()
}

class SendTransactionFailedEmail(
    val error: String,
) : FeedbackData {

    override val subjectResId: Int = R.string.feedback_subject_tx_failed
    override val mainMessageResId: Int = R.string.feedback_preface_tx_failed

    override fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String = FeedbackDataBuilder(infoHolder)
        .appendCardInfo()
        .appendDelimiter()
        .appendTxFailedBlockchainInfo(error)
        .breakLine()
        .appendPhoneInfo()
        .build()
}

class FeedbackEmail : FeedbackData {
    override val subjectResId: Int
        get() = if (isS2CCard) s2cSubject else tangemSubject
    override val mainMessageResId: Int
        get() = if (isS2CCard) s2cMainMessage else tangemMainMessage

    private val tangemSubject: Int = R.string.feedback_subject_support_tangem
    private val tangemMainMessage: Int = R.string.feedback_preface_support

    private val s2cSubject: Int = R.string.feedback_subject_support
    private val s2cMainMessage: Int = R.string.feedback_preface_support

    private var isS2CCard = false

    override fun prepare(infoHolder: AdditionalFeedbackInfo) {
        isS2CCard = TapWorkarounds.isStart2CoinIssuer(infoHolder.cardIssuer)
    }

    override fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String = FeedbackDataBuilder(infoHolder)
        .appendCardInfo()
        .appendWalletsInfo()
        .breakLine()
        .appendPhoneInfo()
        .build()
}

class SupportInfo : FeedbackData {
    override val subjectResId: Int = R.string.details_chat
    override val mainMessageResId: Int = R.string.details_chat

    override fun createOptionalMessage(infoHolder: AdditionalFeedbackInfo): String {
        return FeedbackDataBuilder(infoHolder)
            .appendCardInfo()
            .appendDelimiter()
            .appendPhoneInfo()
            .build()
    }
}
