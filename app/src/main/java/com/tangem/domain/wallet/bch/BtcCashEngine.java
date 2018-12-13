package com.tangem.domain.wallet.bch;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.data.network.ElectrumRequest;
import com.tangem.data.network.ServerApiElectrum;
import com.tangem.domain.wallet.BCHUtils;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.data.Blockchain;
import com.tangem.domain.wallet.btc.BtcData;
import com.tangem.domain.wallet.CoinData;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.domain.wallet.Transaction;
import com.tangem.domain.wallet.UnspentOutputInfo;
import com.tangem.tangemcard.tasks.SignTask;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.tangemcard.util.Util;
import com.tangem.wallet.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BtcCashEngine extends CoinEngine {

    private static final String TAG = BtcCashEngine.class.getSimpleName();
    public BtcData coinData = null;

    public BtcCashEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for BtcEngine");
        }
    }

    public BtcCashEngine() {

    }

    private static int getDecimals() {
        return 8;
    }

    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        return coinData.getBalanceUnconfirmed() != 0;
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "BCH";
    }

    @Override
    public String getOfflineBalanceHTML() {
        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
        Amount offlineAmount = convertToAmount(offlineInternalAmount);
        return offlineAmount.toDescriptionString(getDecimals());
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo();
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else if (coinData.getUnspentTransactions().size() == 0) {
            ctx.setMessage(R.string.please_wait_for_confirmation);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "BCH";
    }

    @Override
    public boolean validateAddress(String address) {
//        if (address == null || address.isEmpty()) {
//            return false;
//        }
//
//        if (address.length() < 25) {
//            return false;
//        }
//
//        if (address.length() > 35) {
//            return false;
//        }
//
//        if (!address.startsWith("1") && !address.startsWith("2") && !address.startsWith("3") && !address.startsWith("n") && !address.startsWith("m")) {
//            return false;
//        }
//
//        byte[] decAddress = Base58.decodeBase58(address);
//
//        if (decAddress == null || decAddress.length == 0) {
//            return false;
//        }
//
//        byte[] rip = new byte[21];
//        for (int i = 0; i < 21; ++i) {
//            rip[i] = decAddress[i];
//        }
//
//        byte[] kcv = CryptoUtil.doubleSha256(rip);
//
//        for (int i = 0; i < 4; ++i) {
//            if (kcv[i] != decAddress[21 + i])
//                return false;
//        }
//
//        if (ctx.getBlockchain() != Blockchain.BitcoinTestNet && ctx.getBlockchain() != Blockchain.Bitcoin) {
//            return false;
//        }
//
//        if (ctx.getBlockchain() == Blockchain.BitcoinTestNet && (address.startsWith("1") || address.startsWith("3"))) {
//            return false;
//        }
//
//        return true;

        if (CashAddr.isValidCashAddress(address))
            return true;
        return false;
    }

    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getShareWalletUriExplorer() {
        return Uri.parse((ctx.getBlockchain() == Blockchain.BitcoinCash ? "https://bitcoincash.blockexplorer.com/address/" : "https://testnet.blockexplorer.com/address/") + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Unknown balance");
            balanceValidator.setSecondLine("Balance cannot be verified. Swipe down to refresh.");
            return false;
        }

        // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

        if (coinData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine("Transaction in progress");
            balanceValidator.setSecondLine("Wait for confirmation in blockchain");
            return false;
        }

        if (coinData.isBalanceReceived() && coinData.isBalanceEqual()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine("Verified balance");
            balanceValidator.setSecondLine("Balance confirmed in blockchain");
            if (coinData.getBalanceInInternalUnits().isZero()) {
                balanceValidator.setFirstLine("Empty wallet");
                balanceValidator.setSecondLine("");
            }
        }

        // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if((card.getRemainingSignatures() != card.getMaxSignatures()) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && coinData.getBalanceInInternalUnits().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine("Verified offline balance");
            balanceValidator.setSecondLine("Can't obtain balance from blockchain. Restore internet connection to be more confident. ");
        }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

        //
//            if(card.isBalanceReceived() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }

        return true;
    }

    @Override
    public Amount getBalance() {
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {

        // CashAddr format
        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);
        return CashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, hash2);

    }

    public String calculateLegacyAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {

        // Legacy format (BTC)
        byte netSelectionByte = (byte) 0x00;

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    public String convertToLegacyAddress(String cashAddr) throws NoSuchProviderException, NoSuchAlgorithmException {

        BitcoinCashAddressDecodedParts bcadp = CashAddr.decodeCashAddress(cashAddr);
        byte netSelectionByte = (byte) 0x00;
        byte hash2[] = bcadp.hash;

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, getBalanceCurrency());
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) throws Exception {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new BtcData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return coinData.getUnspentInputsDescription();
    }

//    @Override
//    public String getAmountDescription(TangemCard mCard, String amount) throws Exception {
//        return mCard.getAmountDescription(Double.parseDouble(amount));
//    }

    @Override
    public void defineWallet() throws CardProtocol.TangemException {
        try {
            String wallet = calculateAddress(ctx.getCard().getWalletPublicKeyRar());
            ctx.getCoinData().setWallet(wallet);
        } catch (Exception e) {
            ctx.getCoinData().setWallet("ERROR");
            throw new CardProtocol.TangemException("Can't define wallet address");
        }
    }


    @Override
    public SignTask.PaymentToSign constructPayment(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        String srcLegacyAddress = convertToLegacyAddress(ctx.getCoinData().getWallet());
        String destLegacyAddress = convertToLegacyAddress(targetAddress);
        byte[] pbKey = ctx.getCard().getWalletPublicKeyRar(); //ALWAYS USING COMPRESS KEY

        // Build script for our address
        List<BtcData.UnspentTransaction> rawTxList = coinData.getUnspentTransactions();
        byte[] outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(srcLegacyAddress).bytes;

        // Collect unspent
        ArrayList<UnspentOutputInfo> unspentOutputs = BCHUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend);

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }


        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final long amountFinal = amount;
        final long changeFinal = change;

        byte[][] txForSign = new byte[unspentOutputs.size()][];
        byte[][] bodyHash = new byte[unspentOutputs.size()][];
        byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BCHUtils.buildTXForSign(srcLegacyAddress, destLegacyAddress, srcLegacyAddress, unspentOutputs, i, amount, change);
            bodyHash[i] = Util.calculateSHA256(txForSign[i]);
            bodyDoubleHash[i] = Util.calculateSHA256(bodyHash[i]);
        }

        return new SignTask.PaymentToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[unspentOutputs.size()][];
                if (txForSign.length > 10) throw new Exception("To much hashes in one transaction!");
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    dataForSign[i] = bodyDoubleHash[i];
                }
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                for (int i = 0; i < txForSign.length; i++) {
                    if (i != 0 && txForSign[0].length != txForSign[i].length)
                        throw new Exception("Hashes length must be identical!");
                    bs.write(txForSign[i]);
                }

                return bs.toByteArray();
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256x2";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version!");
            }

            @Override
            public void onSignCompleted(byte[] signFromCard) throws Exception {
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                    s = CryptoUtil.toCanonicalised(s);

                    unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDerBitcoinCash(r, s, pbKey);
                }

                byte[] txForSend = BCHUtils.buildTXForSend(destLegacyAddress, srcLegacyAddress, unspentOutputs, amountFinal, changeFinal);

                notifyOnNeedSendPayment(txForSend);
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsNotifications blockchainRequestsNotifications) throws Exception {
        final ServerApiElectrum serverApiElectrum = new ServerApiElectrum();

        ServerApiElectrum.ElectrumRequestDataListener electrumBodyListener = new ServerApiElectrum.ElectrumRequestDataListener() {
            @Override
            public void onSuccess(ElectrumRequest electrumRequest) {
                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                    try {
                        String walletAddress = electrumRequest.getParams().getString(0);
                        if (!walletAddress.equals(coinData.getWallet())) {
                            // todo - check
                            throw new Exception("Invalid wallet address in answer!");
                        }
                        Long confBalance = electrumRequest.getResult().getLong("confirmed");
                        Long unconfirmedBalance = electrumRequest.getResult().getLong("unconfirmed");
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceConfirmed(confBalance);
                        coinData.setBalanceUnconfirmed(unconfirmedBalance);
                        coinData.setValidationNodeDescription(serverApiElectrum.getValidationNodeDescription());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance JSONException");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "FAIL METHOD_GetBalance Exception");
                    }
                }

                if (electrumRequest.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                    try {
                        String walletAddress = electrumRequest.getParams().getString(0);
                        JSONArray jsUnspentArray = electrumRequest.getResultArray();
                        try {
                            coinData.getUnspentTransactions().clear();
                            for (int i = 0; i < jsUnspentArray.length(); i++) {
                                JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
                                trUnspent.txID = jsUnspent.getString("tx_hash");
                                trUnspent.Amount = jsUnspent.getInt("value");
                                trUnspent.Height = jsUnspent.getInt("height");
                                coinData.getUnspentTransactions().add(trUnspent);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "FAIL METHOD_ListUnspent JSONException");
                        }

                        for (int i = 0; i < jsUnspentArray.length(); i++) {
                            JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                            Integer height = jsUnspent.getInt("height");
                            String hash = jsUnspent.getString("tx_hash");
                            if (height != -1) {
                                if (!blockchainRequestsNotifications.needTerminate()) {
                                    serverApiElectrum.electrumRequestData(ctx, ElectrumRequest.getTransaction(walletAddress, hash));
                                } else {
                                    serverApiElectrum.setErrorOccured("Terminated by user");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (electrumRequest.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                    try {
                        String txHash = electrumRequest.txHash;
                        String raw = electrumRequest.getResultString();
                        for (BtcData.UnspentTransaction tx : coinData.getUnspentTransactions()) {
                            if (tx.txID.equals(txHash))
                                tx.Raw = raw;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (!serverApiElectrum.hasRequests()) {
                    blockchainRequestsNotifications.onComplete(serverApiElectrum.isErrorOccured());
                }
            }

            @Override
            public void onFail(String method) {
                if (!serverApiElectrum.hasRequests()) {
                    blockchainRequestsNotifications.onComplete(serverApiElectrum.isErrorOccured());
                }
            }
        };

        serverApiElectrum.setElectrumRequestData(electrumBodyListener);

        serverApiElectrum.electrumRequestData(ctx, ElectrumRequest.checkBalance(convertToLegacyAddress(coinData.getWallet())));
        serverApiElectrum.electrumRequestData(ctx, ElectrumRequest.listUnspent(convertToLegacyAddress(coinData.getWallet())));
    }

}
