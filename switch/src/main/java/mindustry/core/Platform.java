package mindustry.core;

import arc.*;
import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.serialization.*;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.type.*;
import mindustry.ui.FileChooser.*;
import rhino.*;

/**
 * Switch replacement for core's Platform. The default loadJar builds a URLClassLoader that uses
 * class loading locks, which the Clearwing runtime does not have. The build drops core's Platform.
 */
public interface Platform{

    /** Dynamically creates a class loader for a jar file. Jar mods can't be loaded on Switch, so this returns the parent. */
    default ClassLoader loadJar(Fi jar, ClassLoader parent) throws Exception{
        return parent;
    }

    /** Steam: Update lobby visibility.*/
    default void updateLobby(){}

    /** Steam: Show multiplayer friend invite dialog.*/
    default void inviteFriends(){}

    /** Steam: Share a map on the workshop.*/
    default void publish(Publishable pub){}

    /** Steam: View a listing on the workshop.*/
    default void viewListing(Publishable pub){}

    /** Steam: View a listing on the workshop by an ID.*/
    default void viewListingID(String mapid){}

    /** Steam: Return external workshop maps to be loaded.*/
    default Seq<Fi> getWorkshopContent(Class<? extends Publishable> type){
        return new Seq<>(0);
    }

    /** Steam: Open workshop for maps.*/
    default void openWorkshop(){}

    /** Get the networking implementation.*/
    default NetProvider getNet(){
        return new ArcNetProvider();
    }

    /** Gets the scripting implementation. */
    default Scripts createScripts(){
        return new Scripts();
    }

    default Context getScriptContext(){
        Context context = Context.getCurrentContext();
        if(context == null) context = Context.enter();
        context.setOptimizationLevel(9);
        return context;
    }

    /** Update discord RPC. */
    default void updateRPC(){
    }

    /** Must be a base64 string 8 bytes in length. */
    default String getUUID(){
        String uuid = Core.settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new Rand().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Core.settings.put("uuid", uuid);
            return uuid;
        }
        return uuid;
    }

    /** Only used for iOS: open the share menu for a map or save. */
    default void shareFile(Fi file){
    }

    /** Do not call directly; use the builder pattern in {@link mindustry.ui.FileChooser}. */
    default void showFileChooser(FileChooserParams params){
        throw new IllegalArgumentException("Not implemented on this platform!");
    }

    /** Hide the app. Android only. */
    default void hide(){
    }

    /** Forces the app into landscape mode.*/
    default void beginForceLandscape(){
    }

    /** Stops forcing the app into landscape orientation.*/
    default void endForceLandscape(){
    }

    interface FileWriter{
        void write(Fi file) throws Throwable;
    }
}
