/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.tables;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.*;

public class TestColumnDefinition extends TestCase {

    @Test
    public void testUtfAssumptions1() throws IOException {
        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final ObjectOutputStream out;
        out = new ObjectOutputStream(outBytes);
        out.writeUTF("\0");
        out.flush();

        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(outBytes.toByteArray()));
        TestCase.assertEquals(4, in.available());
        final String bad = in.readUTF();
        TestCase.assertEquals(1, bad.length());
        TestCase.assertEquals('\0', bad.charAt(0));
        TestCase.assertEquals(0, in.available());
    }

    @Test
    public void testUtfAssumptions2() throws IOException {
        final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        final ObjectOutputStream out;
        out = new ObjectOutputStream(outBytes);
        out.writeShort(2);
        out.writeByte(ColumnDefinition.MAGIC_NUMBER);
        out.writeByte(1);
        out.flush();

        final ObjectInputStream in1 = new ObjectInputStream(new ByteArrayInputStream(outBytes.toByteArray()));
        TestCase.assertEquals(4, in1.available());
        TestCase.assertEquals(2, in1.readUnsignedShort());
        TestCase.assertEquals(ColumnDefinition.MAGIC_NUMBER, in1.readByte());
        TestCase.assertEquals(1, in1.readByte());
        TestCase.assertEquals(0, in1.available());

        final ObjectInputStream in2 = new ObjectInputStream(new ByteArrayInputStream(outBytes.toByteArray()));
        TestCase.assertEquals(4, in2.available());
        try {
            in2.readUTF();
            fail("Excpected UTF data format exception!");
        } catch (UTFDataFormatException e) {
            // Expected
        }
    }
}
