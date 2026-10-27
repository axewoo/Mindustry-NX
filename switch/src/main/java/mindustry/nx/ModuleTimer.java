package mindustry.nx;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;

/** Wraps one of the game's modules (Logic, Control, Renderer, ...) to measure the time its update takes. */
public class ModuleTimer implements ApplicationListener{
    public static final Seq<ModuleTimer> all = new Seq<>();

    public final ApplicationListener module;
    public final String name;
    public long total, max;

    public ModuleTimer(ApplicationListener module){
        this.module = module;
        this.name = module.getClass().getSimpleName();
        all.add(this);
    }

    @Override
    public void update(){
        long start = Time.nanos();
        module.update();
        long time = Time.nanos() - start;
        total += time;
        max = Math.max(max, time);
    }

    @Override
    public void init(){
        module.init();
    }

    @Override
    public void resize(int width, int height){
        module.resize(width, height);
    }

    @Override
    public void pause(){
        module.pause();
    }

    @Override
    public void resume(){
        module.resume();
    }

    @Override
    public void dispose(){
        module.dispose();
    }

    @Override
    public void exit(){
        module.exit();
    }

    @Override
    public void fileDropped(Fi file){
        module.fileDropped(file);
    }
}
