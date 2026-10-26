package arc.util;

import java.nio.*;

/**
 * Switch replacement for Arc's UnsafeBuffers. The Clearwing runtime has no sun.misc.Unsafe,
 * so this always reports failure and Pixmap falls back to {@link Java16Buffers}.
 */
public class UnsafeBuffers{
    public static boolean failed = true, initialized = true;

    public static void checkInit(){}

    public static void copy(Buffer src, int srcPos, Buffer dst, int dstPos, int length){
        throw new UnsupportedOperationException("Unsafe is not available on Switch");
    }
}
