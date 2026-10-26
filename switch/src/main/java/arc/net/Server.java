package arc.net;

/** Switch stand-in for arcnet's Server, keeping only the filter interface core's Net uses. See {@link ArcNetException}. */
public class Server{

    public interface ServerConnectFilter{
        boolean accept(String address);
    }
}
