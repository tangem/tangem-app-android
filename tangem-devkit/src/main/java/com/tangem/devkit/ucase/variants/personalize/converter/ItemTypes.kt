package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit.ucase.variants.personalize.*

class ItemTypes {

    val blockIdList = mutableListOf(
            BlockId.CardNumber, BlockId.Common, BlockId.SigningMethod, BlockId.SignHashExProp, BlockId.Denomination,
            BlockId.Token, BlockId.ProdMask, BlockId.SettingsMask, BlockId.SettingsMaskProtocolEnc,
            BlockId.SettingsMaskNdef, BlockId.Pins
    )

    val listItemList = mutableListOf(CommonId.Curve, CommonId.Blockchain, SettingsMaskNdefId.Aar, PinsId.PauseBeforePin2)

    val boolList = mutableListOf(
            CommonId.CreateWallet, SigningMethodId.SignTx, SigningMethodId.SignTxRaw, SigningMethodId.SignValidatedTx,
            SigningMethodId.SignValidatedTxRaw, SigningMethodId.SignValidatedTxIssuer,
            SigningMethodId.SignValidatedTxRawIssuer, SigningMethodId.SignExternal, SignHashExPropId.RequireTerminalCertSig,
            SignHashExPropId.RequireTerminalTxSig, SignHashExPropId.CheckPin3, TokenId.ItsToken,
            ProductMaskId.Note, ProductMaskId.Tag, ProductMaskId.IdCard, ProductMaskId.IdIssuerCard, SettingsMaskId.IsReusable, SettingsMaskId.NeedActivation,
            SettingsMaskId.ForbidPurge, SettingsMaskId.AllowSelectBlockchain, SettingsMaskId.UseBlock, SettingsMaskId.OneApdu,
            SettingsMaskId.UseCvc, SettingsMaskId.AllowSwapPin, SettingsMaskId.AllowSwapPin2, SettingsMaskId.ForbidDefaultPin,
            SettingsMaskId.SmartSecurityDelay, SettingsMaskId.ProtectIssuerDataAgainstReplay,
            SettingsMaskId.SkipSecurityDelayIfValidated, SettingsMaskId.SkipPin2CvcIfValidated,
            SettingsMaskId.SkipSecurityDelayOnLinkedTerminal, SettingsMaskId.RestrictOverwriteExtraIssuerData,
            SettingsMaskProtocolEncId.AllowUnencrypted, SettingsMaskProtocolEncId.AllowStaticEncryption,
            SettingsMaskNdefId.UseNdef, SettingsMaskNdefId.DynamicNdef, SettingsMaskNdefId.DisablePrecomputedNdef
    )

    val editTextList = mutableListOf(
            CardNumberId.Series, CardNumberId.BatchId, CommonId.BlockchainCustom, SignHashExPropId.CryptoExKey, TokenId.Symbol,
            TokenId.ContractAddress, PinsId.Pin, PinsId.Pin2, PinsId.Pin3, PinsId.Cvc, SettingsMaskNdefId.AarCustom, SettingsMaskNdefId.Uri
    )

    val numberList = mutableListOf(
            CardNumberId.Number, CommonId.MaxSignatures, SignHashExPropId.PinLessFloorLimit, TokenId.Decimal
    )

    val hiddenList = mutableListOf<Id>(
            CardNumberId.Series, CardNumberId.BatchId, SigningMethodId.SignExternal,
            SignHashExPropId.CryptoExKey, SignHashExPropId.CheckPin3, SettingsMaskId.OneApdu,
            SettingsMaskId.UseBlock, SettingsMaskId.ProtectIssuerDataAgainstReplay,
            SignHashExPropId.RequireTerminalCertSig, SignHashExPropId.RequireTerminalTxSig,
            BlockId.Pins,  PinsId.Pin, PinsId.Pin2, PinsId.Pin3, PinsId.Cvc
    )

    val oftenUsedList = listOf<Id>(
//            BlockId.CardNumber,
            BlockId.Common,
//            BlockId.SigningMethod,
//            BlockId.SignHashExProp,
//            BlockId.Denomination,
//            BlockId.Token,
            BlockId.ProdMask,
//            BlockId.SettingsMask,
//            BlockId.SettingsMaskProtocolEnc,
            BlockId.SettingsMaskNdef,
//            BlockId.Pins,
            CommonId.Curve,
            CommonId.Blockchain,
            CommonId.BlockchainCustom,
//            Common.MaxSignatures,
            CommonId.CreateWallet,
//            SigningMethod.SignTx,
//            SigningMethod.SignTxRaw,
//            SigningMethod.SignValidatedTx,
//            SigningMethod.SignValidatedTxRaw,
//            SigningMethod.SignValidatedTxIssuer,
//            SigningMethod.SignValidatedTxRawIssuer,
//            SigningMethod.SignExternal,
//            SignHashExProp.PinLessFloorLimit,
//            SignHashExProp.CryptoExKey,
//            SignHashExProp.RequireTerminalCertSig,
//            SignHashExProp.RequireTerminalTxSig,
//            SignHashExProp.CheckPin3,
//            Denomination.WriteOnPersonalize,
//            Denomination.Denomination,
//            Token.ItsToken,
//            Token.Symbol,
//            Token.ContractAddress,
//            Token.Decimal,
            ProductMaskId.Note,
            ProductMaskId.Tag,
            ProductMaskId.IdCard,
            ProductMaskId.IdIssuerCard,
//            SettingsMask.IsReusable,
//            SettingsMask.NeedActivation,
//            SettingsMask.ForbidPurge,
//            SettingsMask.AllowSelectBlockchain,
//            SettingsMask.UseBlock,
//            SettingsMask.OneApdu,
//            SettingsMask.UseCvc,
//            SettingsMask.AllowSwapPin,
//            SettingsMask.AllowSwapPin2,
//            SettingsMask.ForbidDefaultPin,
//            SettingsMask.SmartSecurityDelay,
//            SettingsMask.ProtectIssuerDataAgainstReplay,
//            SettingsMask.SkipSecurityDelayIfValidated,
//            SettingsMask.SkipPin2CvcIfValidated,
//            SettingsMask.SkipSecurityDelayOnLinkedTerminal,
//            SettingsMask.RestrictOverwriteExtraIssuerData,
//            SettingsMaskProtocolEnc.AllowUnencrypted,
//            SettingsMaskProtocolEnc.AllowStaticEncryption,
//            SettingsMaskNdef.UseNdef,
//            SettingsMaskNdef.DynamicNdef,
//            SettingsMaskNdef.DisablePrecomputedNdef,
            SettingsMaskNdefId.Aar,
            SettingsMaskNdefId.AarCustom,
            SettingsMaskNdefId.Uri,
//            Pins.Pin,
//            Pins.Pin2,
//            Pins.Pin3,
//            Pins.Cvc,
            PinsId.PauseBeforePin2
    )

}
