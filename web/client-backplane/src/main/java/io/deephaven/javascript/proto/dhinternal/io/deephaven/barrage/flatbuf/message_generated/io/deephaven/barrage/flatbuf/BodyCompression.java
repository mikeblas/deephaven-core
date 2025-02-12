package io.deephaven.javascript.proto.dhinternal.io.deephaven.barrage.flatbuf.message_generated.io.deephaven.barrage.flatbuf;

import io.deephaven.javascript.proto.dhinternal.flatbuffers.Builder;
import io.deephaven.javascript.proto.dhinternal.flatbuffers.ByteBuffer;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(
    isNative = true,
    name =
        "dhinternal.io.deephaven.barrage.flatbuf.Message_generated.io.deephaven.barrage.flatbuf.BodyCompression",
    namespace = JsPackage.GLOBAL)
public class BodyCompression {
  public static native void addCodec(Builder builder, int codec);

  public static native void addMethod(Builder builder, int method);

  public static native double createBodyCompression(
      Builder builder, int codec, int method);

  public static native double endBodyCompression(Builder builder);

  public static native BodyCompression getRootAsBodyCompression(ByteBuffer bb, BodyCompression obj);

  public static native BodyCompression getRootAsBodyCompression(ByteBuffer bb);

  public static native BodyCompression getSizePrefixedRootAsBodyCompression(
      ByteBuffer bb, BodyCompression obj);

  public static native BodyCompression getSizePrefixedRootAsBodyCompression(ByteBuffer bb);

  public static native void startBodyCompression(Builder builder);

  public ByteBuffer bb;
  public double bb_pos;

  public native BodyCompression __init(double i, ByteBuffer bb);

  public native int codec();

  public native int method();
}
