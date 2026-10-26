package arc.util;

import java.nio.*;

/** Switch replacement for Arc's Java16Buffers: the Clearwing runtime lacks the Java 16 bulk ByteBuffer.put. */
public class Java16Buffers{

    public static void copy(ByteBuffer src, int srcPos, ByteBuffer dst, int dstPos, int len){
        if(src.hasArray() && dst.hasArray()){
            System.arraycopy(src.array(), src.arrayOffset() + srcPos, dst.array(), dst.arrayOffset() + dstPos, len);
            return;
        }
        for(int i = 0; i < len; i++){
            dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
}
