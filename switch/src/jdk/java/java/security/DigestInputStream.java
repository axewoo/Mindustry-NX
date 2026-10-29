package java.security;

import java.io.*;

/** DigestInputStream for the Clearwing runtime, which does not ship one. */
public class DigestInputStream extends FilterInputStream{
    private boolean on = true;
    protected MessageDigest digest;

    public DigestInputStream(InputStream stream, MessageDigest digest){
        super(stream);
        this.digest = digest;
    }

    public MessageDigest getMessageDigest(){
        return digest;
    }

    public void setMessageDigest(MessageDigest digest){
        this.digest = digest;
    }

    public void on(boolean on){
        this.on = on;
    }

    @Override
    public int read() throws IOException{
        int b = in.read();
        if(on && b != -1) digest.update((byte)b);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        int n = in.read(b, off, len);
        if(on && n > 0) digest.update(b, off, n);
        return n;
    }

    @Override
    public String toString(){
        return "[Digest Input Stream] " + digest.toString();
    }
}
