package org.jmisb.st0601;

import java.util.ArrayList;
import java.util.List;
import org.jmisb.api.klv.ArrayBuilder;
import org.jmisb.api.klv.BerDecoder;
import org.jmisb.api.klv.BerField;

/**
 * Control Command Verification List (ST 0601 Item 116).
 *
 * <p>From ST:
 *
 * <blockquote>
 *
 * Acknowledgment of one or more control commands were received by the platform.
 *
 * <p>The Control Command Verification List is a variable length pack of one or more BER-OID values.
 * Each value is a verification or acknowledgment of a Control Command sent to the platform – see
 * Item 115 for more details.
 *
 * </blockquote>
 */
public class ControlCommandVerification implements IUasDatalinkValue {
    private final List<Integer> commands = new ArrayList<>();

    /**
     * Create from value.
     *
     * @param commandIds list of command id values
     */
    public ControlCommandVerification(List<Integer> commandIds) {
        this.commands.addAll(commandIds);
    }

    /**
     * Create from encoded bytes.
     *
     * @param bytes encoded command identifiers list
     */
    public ControlCommandVerification(byte[] bytes) {
        int idx = 0;
        while (idx < bytes.length) {
            BerField idField = BerDecoder.decode(bytes, idx, true);
            this.commands.add(idField.getValue());
            idx += idField.getLength();
        }
    }

    /**
     * Get the command ids that make up this command verification list.
     *
     * @return the ordered list of ids.
     */
    public List<Integer> getCommandIds() {
        return this.commands;
    }

    @Override
    public byte[] getBytes() {
        ArrayBuilder builder = new ArrayBuilder();
        commands.forEach(
                id -> {
                    builder.appendAsOID(id);
                });
        return builder.toBytes();
    }

    @Override
    public String getDisplayableValue() {
        List<String> idsAsText = new ArrayList<>();
        commands.forEach((id) -> idsAsText.add("" + id));
        return "" + String.join(",", idsAsText);
    }

    @Override
    public String getDisplayName() {
        return "Control Command Verification";
    }
}
