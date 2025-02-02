package org.jmisb.api.klv;

import java.io.IOException;
import java.io.InputStream;

/** Decode data using Basic Encoding Rules (BER). */
public class BerDecoder {

    private BerDecoder() {}

    /**
     * Decode a field (length and value) from an encoded byte array.
     *
     * @param data Array holding the BER-encoded data
     * @param offset Index of the first byte of the array to decode
     * @param isOid true if the data is encoded using BER-OID
     * @return decoded The decoded field
     * @throws IllegalArgumentException if the encoded data is invalid
     */
    public static BerField decode(byte[] data, int offset, boolean isOid)
            throws IllegalArgumentException {
        final int length, value;

        // logger.debug("First byte of BER: " + String.format("%02X ", data[offset]));
        if (data.length <= offset) {
            throw new IllegalArgumentException("Cannot read BER from beyond array limit");
        }
        if (!isOid) {
            if ((data[offset] & 0x80) == 0) {
                // BER Short Form. If the first bit of the BER is 0 then the BER is 1-byte and the
                // value is encoded directly in that byte. This means the short form encodes values
                // from 0 to 127.
                length = 1;
                value = data[offset] & 0x7f;
            } else {
                // BER Long Form (variable length). If the first bit of the BER is 1 then the rest
                // of the first byte encodes the length of the BER in bytes and the value is read
                // from that number of bytes immediately following. Theoretically the long form
                // encodes values 128 to 2^(8*127), but for our purposes we handle 128 to 2^32,
                // which should be plenty for a length field.
                int berLength = data[offset] & 0x7f;
                if (data.length < offset + 1 + berLength) {
                    throw new IllegalArgumentException(
                            "BER long form: BER length overruns packet size");
                }

                if (berLength > 4) {
                    throw new IllegalArgumentException(
                            "BER long form: BER length is >5 bytes; data is probably corrupt");
                }
                int val = 0;
                for (int i = 0; i < berLength; ++i) {
                    int b = 0x00FF & data[offset + i + 1];
                    val = (val << 8) | b;
                }
                length = berLength + 1;
                value = val;
            }
        } else {
            // BER-OID
            int read;
            int tag = 0;
            int i = 0;
            do {
                if (offset + i >= data.length) {
                    throw new IllegalArgumentException("BER-OID: out of bytes");
                }
                read = data[offset + i];
                int highbits = (tag << 7);
                int lowbits = (read & 0x7F);
                tag = highbits + lowbits;
                i++;
            } while ((read & 0x80) == 0x80);
            length = i;
            value = tag;
        }

        if (value < 0) {
            throw new IllegalArgumentException("BER: error decoding value");
        }

        return new BerField(length, value);
    }

    public static BerField decode(InputStream is, boolean isOid) throws IOException {
        if (!isOid) {
            return decodeBer(is);
        }

        return decodeBerOid(is);
    }

    private static BerField decodeBer(InputStream is) throws IOException {
        int length = is.read();

        if ((length & 0x80) == 0) {
            // BER Short Form.  If the first bit of the BER is 0 then the BER is 1-byte.
            return new BerField(1, length);
        }

        // BER Long Form (variable length)
        int berLength = length & 0x7f;
        int fullBerSize = berLength + 1;
        byte[] data = new byte[berLength];
        int read = is.read(data, 0, data.length);
        if (read != data.length) {
            throw new IllegalArgumentException("BER parsing ran out of bytes");
        }
        int len = 0;
        for (int i = 0; i < berLength; ++i) {
            int b = 0x00FF & data[i];
            len = (len << 8) | b;
        }
        length = len;

        return new BerField(fullBerSize, length);
    }

    private static BerField decodeBerOid(InputStream is) throws IOException {
        int read;
        int value = 0;
        int length = 0;
        do {
            read = is.read();
            int highbits = (value << 7);
            int lowbits = (read & 0x7F);
            value = highbits + lowbits;
            length++;
        } while ((read & 0x80) == 0x80);
        return new BerField(length, value);
    }
}
