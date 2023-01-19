package com.tangem.tap.domain.statePrinter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.redux.state.StringStateConverter
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStep
import com.tangem.tap.store
import timber.log.Timber
import java.math.BigInteger

/**
 * Created by Anton Zhilenkov on 17.10.2022.
 */
class OnboardingWalletStateConverter : StringStateConverter<AppState> {
    private val converter = MoshiJsonConverter(
        MoshiJsonConverter.getTangemSdkAdapters() + internalAdapters(),
        MoshiJsonConverter.getTangemSdkTypedAdapters(),
    )

    private fun internalAdapters(): List<Any> {
        return listOf(
            BackupStepAdapter(),
            BigIntegerAdapter(),

        )
    }

    override fun convert(stateHolder: AppState): String {
        val model = stateHolder.onboardingWalletState
        val json = converter.prettyPrint(model)

        return json
    }
}

fun printOnboardingWalletState() {
    val stringState = OnboardingWalletStateConverter().convert(store.state)
    Timber.d(stringState)
}

class BackupStepAdapter {
    @ToJson
    fun toJson(src: BackupStep): String {
        return when (src) {
            BackupStep.AddBackupCards -> "AddBackupCards"
            BackupStep.EnterAccessCode -> "EnterAccessCode"
            BackupStep.Finished -> "Finished"
            BackupStep.InitBackup -> "InitBackup"
            BackupStep.ReenterAccessCode -> "ReenterAccessCode"
            BackupStep.ScanOriginCard -> "ScanOriginCard"
            BackupStep.SetAccessCode -> "SetAccessCode"
            is BackupStep.WriteBackupCard -> "WriteBackupCard[${src.cardNumber}]"
            BackupStep.WritePrimaryCard -> "WritePrimaryCard"
        }
    }

    @Suppress("MagicNumber")
    @FromJson
    fun fromJson(json: String): BackupStep {
        return when (json) {
            "AddBackupCards" -> BackupStep.AddBackupCards
            "EnterAccessCode" -> BackupStep.EnterAccessCode
            "Finished" -> BackupStep.Finished
            "InitBackup" -> BackupStep.InitBackup
            "ReenterAccessCode" -> BackupStep.ReenterAccessCode
            "ScanOriginCard" -> BackupStep.ScanOriginCard
            "SetAccessCode" -> BackupStep.SetAccessCode
            "WriteBackupCard[1]" -> BackupStep.WriteBackupCard(1)
            "WriteBackupCard[2]" -> BackupStep.WriteBackupCard(2)
            "WriteBackupCard[3]" -> BackupStep.WriteBackupCard(3)
            "WritePrimaryCard" -> BackupStep.WritePrimaryCard
            else -> throw UnsupportedOperationException()
        }
    }
}

class BigIntegerAdapter {
    @ToJson
    fun toJson(src: BigInteger): String {
        return src.toString()
    }

    @FromJson
    fun fromJson(json: String): BigInteger {
        return BigInteger(json)
    }
}
