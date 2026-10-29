package mindustry.net;

import arc.func.*;
import arc.struct.*;
import mindustry.net.Net.*;

import java.io.*;

/**
 * Switch replacement for core's ArcNetProvider. Arc's networking is built on NIO selectors,
 * which the Clearwing runtime does not have, so multiplayer is unavailable on Switch.
 * The build drops core's ArcNetProvider classes and uses this one instead.
 */
public class ArcNetProvider implements NetProvider{
    private static final String unsupported = "Multiplayer is not available on Switch.";

    @Override
    public void connectClient(String ip, int port, Runnable success) throws IOException{
        throw new IOException(unsupported);
    }

    @Override
    public void sendClient(Object object, boolean reliable){}

    @Override
    public void disconnectClient(){}

    @Override
    public void discoverServers(Cons<Host> callback, Runnable done){
        done.run();
    }

    @Override
    public void pingHost(String address, int port, Cons<Host> valid, Cons<Exception> failed){
        failed.get(new IOException(unsupported));
    }

    @Override
    public void hostServer(int port) throws IOException{
        throw new IOException(unsupported);
    }

    @Override
    public Iterable<? extends NetConnection> getConnections(){
        return new Seq<>();
    }

    @Override
    public void closeServer(){}
}
