package com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo

import com.tangem.tangemtest.ucase.variants.personalize.*

class ItemTypes {

    val blockIdList = mutableListOf(
            BlockId.CardNumber, BlockId.Common, BlockId.SigningMethod, BlockId.SignHashExProp, BlockId.Denomination,
            BlockId.Token, BlockId.ProdMask, BlockId.SettingsMask, BlockId.SettingsMaskProtocolEnc,
            BlockId.SettingsMaskNdef, BlockId.Pins
    )

    val listItemList = mutableListOf(Common.Curve, Common.Blockchain, SettingsMaskNdef.Aar, Pins.PauseBeforePin2)

    val boolList = mutableListOf(
            Common.CreateWallet, SigningMethod.SignTx, SigningMethod.SignTxRaw, SigningMethod.SignValidatedTx,
            SigningMethod.SignValidatedTxRaw, SigningMethod.SignValidatedTxIssuer,
            SigningMethod.SignValidatedTxRawIssuer, SigningMethod.SignExternal, SignHashExProp.RequireTerminalCertSig,
            SignHashExProp.RequireTerminalTxSig, SignHashExProp.CheckPin3, Denomination.WriteOnPersonalize, Token.ItsToken,
            ProductMask.Note, ProductMask.Tag, ProductMask.CardId, ProductMask.IssuerId,SettingsMask.IsReusable, SettingsMask.NeedActivation,
            SettingsMask.ForbidPurge, SettingsMask.AllowSelectBlockchain, SettingsMask.UseBlock, SettingsMask.OneApdu,
            SettingsMask.UseCvc, SettingsMask.AllowSwapPin, SettingsMask.AllowSwapPin2, SettingsMask.ForbidDefaultPin,
            SettingsMask.SmartSecurityDelay, SettingsMask.ProtectIssuerDataAgainstReplay,
            SettingsMask.SkipSecurityDelayIfValidated, SettingsMask.SkipPin2CvcIfValidated,
            SettingsMask.SkipSecurityDelayOnLinkedTerminal, SettingsMask.RestrictOverwriteExtraIssuerData,
            SettingsMaskProtocolEnc.AllowUnencrypted, SettingsMaskProtocolEnc.AllowStaticEncryption,
            SettingsMaskNdef.UseNdef, SettingsMaskNdef.DynamicNdef, SettingsMaskNdef.DisablePrecomputedNdef
    )

    val editTextList = mutableListOf(
            CardNumber.Series, CardNumber.BatchId, Common.BlockchainCustom, SignHashExProp.CryptoExKey, Token.Symbol,
            Token.ContractAddress, Pins.Pin, Pins.Pin2, Pins.Pin3, Pins.Cvc, SettingsMaskNdef.AarCustom
    )

    val numberList = mutableListOf(
            CardNumber.Number, Common.MaxSignatures, SignHashExProp.PinLessFloorLimit, Denomination.Denomination, Token.Decimal
    )

    val hiddenList = mutableListOf(
            CardNumber.Series, CardNumber.BatchId, Pins.Pin3, SigningMethod.SignExternal,
            SignHashExProp.CryptoExKey, SignHashExProp.CheckPin3, SettingsMask.OneApdu,
            SettingsMask.UseBlock, SettingsMask.ProtectIssuerDataAgainstReplay,
            SignHashExProp.RequireTerminalCertSig, SignHashExProp.RequireTerminalTxSig
    )

}