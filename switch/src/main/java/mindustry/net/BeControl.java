package mindustry.net;

import arc.func.*;

/** Switch replacement for core's BeControl: bleeding-edge auto-updates don't apply to an NRO. */
public class BeControl{

    public boolean active(){
        return false;
    }

    public void init(){}

    public void checkUpdate(Boolc done){
        done.get(false);
    }

    public boolean isUpdateAvailable(){
        return false;
    }

    public void showUpdateDialog(){}
}
