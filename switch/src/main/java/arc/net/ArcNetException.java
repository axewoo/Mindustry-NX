package arc.net;

/**
 * Switch copy of arcnet's ArcNetException. The build drops the rest of arcnet, which is built on
 * NIO channels the Clearwing runtime does not have; core's Net only needs this class and
 * {@link Server.ServerConnectFilter}.
 */
public class ArcNetException extends RuntimeException{
    public ArcNetException(){
        super();
    }

    public ArcNetException(String message, Throwable cause){
        super(message, cause);
    }

    public ArcNetException(String message){
        super(message);
    }

    public ArcNetException(Throwable cause){
        super(cause);
    }
}
