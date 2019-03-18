package com.ripple.encodings.basex;

import com.ripple.utils.HashUtils;

import java.util.Arrays;

public class BaseX implements IBaseX {
    private char[] alphabet;
    private final char encodedZero;
    protected final int[] indexes;

    public BaseX(String alphabet_) {
        if (alphabet_.length() > 256) {
            throw new IllegalArgumentException();
        }
        alphabet = alphabet_.toCharArray();
        indexes = new int[128];
        encodedZero = alphabet[0];
        Arrays.fill(indexes, -1);
        for (int i = 0; i < alphabet.length; i++) {
            indexes[alphabet[i]] = i;
        }
    }

    private static String repeat(int times, char repeated) {
        char[] chars = new char[times];
        Arrays.fill(chars, repeated);
        return new String(chars);
    }

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended).
     *
     * @param input the bytes to encode
     * @return the base58-encoded string
     */
    @Override
    public String encode(byte[] input) {
        if (input.length == 0) {
            return "";
        }
        // Count leading zeros.
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }
        // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
        input = Arrays.copyOf(input, input.length); // since we modify it in-place
        char[] encoded = new char[input.length * 2]; // upper bound
        int outputStart = encoded.length;
        for (int inputStart = zeros; inputStart < input.length; ) {
            encoded[--outputStart] = alphabet[divmod(input, inputStart, 256, 58)];
            if (input[inputStart] == 0) {
                ++inputStart; // optimization - skip leading zeros
            }
        }
        // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
        while (outputStart < encoded.length && encoded[outputStart] == encodedZero) {
            ++outputStart;
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = encodedZero;
        }
        // Return encoded string (including encoded leading zeros).
        return new String(encoded, outputStart, encoded.length - outputStart);
    }

    /**
     * Decodes the given base58 string into the original data bytes.
     *
     * @param input the base58-encoded string to decode
     * @return the decoded data bytes
     * @throws EncodingFormatException if the given string is not a valid base58 string
     */
    @Override
    public byte[] decode(String input) throws EncodingFormatException {
        if (input.length() == 0) {
            return new byte[0];
        }
        // Convert the base58-encoded ASCII chars to a base58 byte sequence (base58 digits).
        byte[] input58 = new byte[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            int digit = c < 128 ? indexes[c] : -1;
            if (digit < 0) {
                throw new EncodingFormatException("Illegal character " + c + " at position " + i);
            }
            input58[i] = (byte) digit;
        }
        // Count leading zeros.
        int zeros = 0;
        while (zeros < input58.length && input58[zeros] == 0) {
            ++zeros;
        }
        // Convert base-58 digits to base-256 digits.
        byte[] decoded = new byte[input.length()];
        int outputStart = decoded.length;
        for (int inputStart = zeros; inputStart < input58.length; ) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256);
            if (input58[inputStart] == 0) {
                ++inputStart; // optimization - skip leading zeros
            }
        }
        // Ignore extra leading zeroes that were added during the calculation.
        while (outputStart < decoded.length && decoded[outputStart] == 0) {
            ++outputStart;
        }
        // Return decoded data (including original number of leading zeros).
        return Arrays.copyOfRange(decoded, outputStart - zeros, decoded.length);
    }

    /**
     * Decodes the given base58 string into the original data bytes, using the checksum in the
     * last 4 bytes of the decoded data to verify that the rest are correct. The checksum is
     * removed from the returned data.
     *
     * @param input the base58-encoded string to decode (which should include the checksum)
     * @throws EncodingFormatException if the input is not base 58 or the checksum does not validate.
     */
    public byte[] decodeChecked(String input) throws EncodingFormatException {
        byte[] decoded  = decode(input);
        if (decoded.length < 4)
            throw new EncodingFormatException("Input too short");
        byte[] data = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
        byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
        byte[] actualChecksum = Arrays.copyOfRange(HashUtils.doubleDigest(data), 0, 4);
        if (!Arrays.equals(checksum, actualChecksum))
            throw new EncodingFormatException("Checksum does not validate");
        return data;
    }

    /**
     * Divides a number, represented as an array of bytes each containing a single digit
     * in the specified base, by the given divisor. The given number is modified in-place
     * to contain the quotient, and the return value is the remainder.
     *
     * @param number the number to divide
     * @param firstDigit the index within the array of the first non-zero digit
     *        (this is used for optimization by skipping the leading zeros)
     * @param base the base in which the number's digits are represented (up to 256)
     * @param divisor the number to divide by (up to 256)
     * @return the remainder of the division operation
     */
    private byte divmod(byte[] number, int firstDigit, int base, int divisor) {
        // this is just long division which accounts for the base of the input digits
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xFF;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }

    @Override
    public byte[] findPrefix(int payLoadLength, String desiredPrefix) {
        if (alphabet.length != 58) {
            throw new IllegalStateException("Must be base58");
        }
        int totalLength = payLoadLength + 4; // for the checksum
        double chars = Math.log(Math.pow(256, totalLength)) / Math.log(58);
        int requiredChars = (int) Math.ceil(chars + 0.2D);
        // Mess with this to see stability tests fail
        int charPos = (alphabet.length / 2) - 1;
        char padding = alphabet[(charPos)];
        String template = desiredPrefix + repeat(requiredChars, padding);
        byte[] decoded = decode(template);
        return copyOfRange(decoded, 0, decoded.length - totalLength);
    }

    @Override
    public String encodeVersioned(byte[] input, Version version) {
        if (input.length != version.expectedLength) {
            throw new IllegalArgumentException(
                    "input length=" + input.length +
                    ", expected=" + version.expectedLength);
        }
        return encode(concatVersionAndAddChecksum(input, version.bytes));
    }

    private byte[] concatVersionAndAddChecksum(byte[] input, byte[] version) {
        byte[] buffer = new byte[input.length + version.length];
        System.arraycopy(version, 0, buffer, 0, version.length);
        System.arraycopy(input, 0, buffer, version.length, input.length);
        byte[] checkSum = copyOfRange(HashUtils.doubleDigest(buffer), 0, 4);
        byte[] output = new byte[buffer.length + checkSum.length];
        System.arraycopy(buffer, 0, output, 0, buffer.length);
        System.arraycopy(checkSum, 0, output, buffer.length, checkSum.length);
        return output;
    }

    public Decoded decodeVersioned(String input,
                                   Version... possibleVersions) throws EncodingFormatException {

        byte[] buffer = decodeChecked(input);

        Version foundVersion = null;
        int expectedLength = possibleVersions[0].expectedLength;
        int versionLength = buffer.length - expectedLength;
        byte[] versionBytes = copyOfRange(buffer, 0, versionLength);

        for (Version possible : possibleVersions) {
            if (possible.expectedLength != expectedLength) {
                throw new IllegalStateException();
            }

            if (Arrays.equals(possible.bytes, versionBytes)) {
                foundVersion = possible;
                break;
            }
        }
        if (foundVersion == null) {
            throw new EncodingFormatException("Incorrect version");
        }

        byte[] bytes = copyOfRange(buffer, versionLength, buffer.length);
        if (bytes.length != expectedLength) {
            throw new EncodingFormatException("Incorrect length");
        }

        return new Decoded(foundVersion, bytes);
    }

    private byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

}