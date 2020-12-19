package com.tangem.tap.features.details.redux.twins

import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.twins.TwinCardNumber

data class CreateTwinWalletState(
        val scanResponse: ScanNoteResponse?,
        val step: CreateTwinWalletStep = CreateTwinWalletStep.FirstStep,
        val twinCardNumber: TwinCardNumber?,
        val createTwinWallet: CreateTwinWallet?,
        val showAlert: Boolean,
        val allowRecreatingWallet: Boolean? = null
)

enum class CreateTwinWalletStep { FirstStep, SecondStep, ThirdStep }

enum class CreateTwinWallet { CreateWallet, RecreateWallet }

