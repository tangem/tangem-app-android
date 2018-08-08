package com.tangem.domain.wallet;

import android.net.Uri;

import com.tangem.domain.cardReader.CardProtocol;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {

    public abstract String getNextNode(TangemCard card);

    public abstract int getNextNodePort(TangemCard card);

    public abstract String getNode(TangemCard card);

    public abstract int getNodePort(TangemCard card);

    public abstract void switchNode(TangemCard card);

    public abstract boolean awaitingConfirmation(TangemCard card);

    public abstract boolean hasBalanceInfo(TangemCard card);

    public abstract boolean isBalanceNotZero(TangemCard card);

    public abstract boolean isBalanceAlterNotZero(TangemCard card);

    public abstract boolean checkAmount(TangemCard card, String amount) throws Exception;

    public abstract int getTokenDecimals(TangemCard card);

    public abstract String getContractAddress(TangemCard card);

    public abstract byte[] sign(String feeValue, String amountValue, String toValue, TangemCard mCard, CardProtocol protocol) throws Exception;

    public abstract boolean checkUnspentTransaction(TangemCard card);

    public abstract Uri getShareWalletUriExplorer(TangemCard card);

    public abstract Long getBalanceLong(TangemCard card);

    public abstract Uri getShareWalletUri(TangemCard card);

    public abstract String evaluateFeeEquivalent(TangemCard card, String fee);

    public abstract boolean checkAmountValue(TangemCard card, String amount, String fee, Long minFeeInInternalUnits);

    public abstract boolean inOutPutVisible();

    public abstract String getBalance(TangemCard card);

    public abstract String getBalanceWithAlter(TangemCard card);

    public abstract String getBalanceCurrency(TangemCard card);

    public abstract String getFeeCurrency();

    public abstract boolean isNeedCheckNode();

    public abstract String getBalanceEquivalent(TangemCard card);

    public abstract String getBalanceValue(TangemCard card);

    public abstract String getAmountDescription(TangemCard card, String amount) throws Exception;

    public abstract String getAmountEquivalentDescriptor(TangemCard card, String value);

    public abstract boolean validateAddress(String address, TangemCard card);

    public abstract String calculateAddress(TangemCard card, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException;

    public abstract String convertByteArrayToAmount(TangemCard card, byte[] bytes) throws Exception;

    public abstract byte[] convertAmountToByteArray(TangemCard card, String amount) throws Exception;

}