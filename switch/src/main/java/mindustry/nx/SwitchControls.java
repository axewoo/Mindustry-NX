package mindustry.nx;

import arc.*;
import arc.backend.switchgdx.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.util.*;
import arc.input.KeyBind.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.graphics.*;
import mindustry.input.*;

import static mindustry.Vars.*;

/**
 * Joy-Con / Pro Controller support. Using the controller switches the game to Mindustry's desktop controls, with the
 * right stick moving an on-screen mouse cursor; touching the game world switches back to the touch controls.
 *
 * In game: left stick moves the unit (or the camera when dead), right stick moves the cursor, A is the left mouse
 * button (place, select, mine, hold to shoot, drag to place lines), B the right mouse button (cancel, hold and drag to
 * break, order units in command mode), X picks the block under the cursor, Y toggles command mode, L / R rotate,
 * ZL / ZR zoom, d-pad left / right choose a block, d-pad up / down change the category, L3 boosts, R3 respawns at
 * the core, + opens the menu and - pauses.
 * In menus: A clicks, B goes back, d-pad up / down scroll, d-pad left / right nudge the cursor, R3 recenters it.
 */
public class SwitchControls implements ApplicationListener{
    static final int
    btnA = 1, btnB = 1 << 1, btnX = 1 << 2, btnY = 1 << 3, btnR3 = 1 << 5,
    btnL = 1 << 6, btnR = 1 << 7, btnZL = 1 << 8, btnZR = 1 << 9, btnPlus = 1 << 10, btnMinus = 1 << 11,
    btnLeft = 1 << 12, btnUp = 1 << 13, btnRight = 1 << 14, btnDown = 1 << 15;

    static final float deadzone = 0.15f, zoomSpeed = 0.06f;
    /** Cursor speed in pixels per frame at 1280 wide: it starts slow for precision and speeds up while the stick is held. */
    static final float cursorMinSpeed = 3.5f, cursorMaxSpeed = 12f, cursorRampTime = 40f;
    /** Seconds without cursor input before the cursor hides. */
    static final float hideTime = 6f;

    float cursorX = -1, cursorY = -1, idle = hideTime, scrollRepeat, cursorHeld;
    /** Per-second frame stats while in game, logged to profile stutters. */
    float perfTime, perfWorst, lastCamX, lastCamY;
    int perfFrames, perfMoving;
    long lastUpdateNs, selfNs;
    final long[] perf = new long[6], perfMax = new long[6];
    int lastButtons;
    boolean visible;
    /** Whether the cursor drives pointer 0. Touching the screen hands the pointer back to the finger until the cursor is used again. */
    boolean pointerActive, screenTouched;

    public SwitchControls(){
        //desktop controls read these binds; the sticks are read through SwitchInput.axis
        Binding.moveX.value = new Axis(KeyCode.controllerLStickXAxis);
        Binding.moveY.value = new Axis(KeyCode.controllerLStickYAxis);
        Binding.boost.value = new Axis(KeyCode.controllerLStick);
        Binding.respawn.value = new Axis(KeyCode.controllerRStick);
        Binding.pick.value = new Axis(KeyCode.controllerX);
        Binding.commandMode.value = new Axis(KeyCode.controllerY);
        Binding.rotate.value = new Axis(KeyCode.controllerRBumper, KeyCode.controllerLBumper);
        Binding.blockSelectLeft.value = new Axis(KeyCode.controllerdPadLeft);
        Binding.blockSelectRight.value = new Axis(KeyCode.controllerdPadRight);
        Binding.categoryPrev.value = new Axis(KeyCode.controllerdPadUp);
        Binding.categoryNext.value = new Axis(KeyCode.controllerdPadDown);
        Core.settings.put("commandmodehold", false);
    }

    void logPerf(){
        float frameMs = Core.graphics.getDeltaTime() * 1000f;
        //very long frames, to match against the [GC] lines in Mindustry.log
        if(frameMs > 250f) Log.debug("[Frame] @ms at @", (int)frameMs, Time.millis());
        if(!state.isGame()) return;

        //phases of the previous frame: the game's update, the buffer swap and polling, and the rest of the listeners (these controls)
        long update = PerfCounter.update.latestValueNs();
        perf[0] += update; perfMax[0] = Math.max(perfMax[0], update);
        perf[1] += PerfCounter.render.latestValueNs(); perfMax[1] = Math.max(perfMax[1], PerfCounter.render.latestValueNs());
        perf[2] += PerfCounter.other.latestValueNs(); perfMax[2] = Math.max(perfMax[2], PerfCounter.other.latestValueNs());
        perf[3] += SwitchApplication.lastNativeNs; perfMax[3] = Math.max(perfMax[3], SwitchApplication.lastNativeNs);
        long rest = Math.max(SwitchApplication.lastListenNs - lastUpdateNs, 0);
        perf[4] += rest; perfMax[4] = Math.max(perfMax[4], rest);
        perf[5] += selfNs; perfMax[5] = Math.max(perfMax[5], selfNs);
        lastUpdateNs = update;

        perfFrames++;
        perfWorst = Math.max(perfWorst, frameMs);
        if(Core.camera.position.x != lastCamX || Core.camera.position.y != lastCamY) perfMoving++;
        lastCamX = Core.camera.position.x;
        lastCamY = Core.camera.position.y;
        perfTime += Core.graphics.getDeltaTime();
        if(perfTime >= 1f){
            StringBuilder out = new StringBuilder();
            String[] names = {"update", "render", "other", "native", "rest", "controls"};
            for(int i = 0; i < names.length; i++){
                out.append(names[i]).append(' ').append(ms(perf[i] / perfFrames)).append('/').append(ms(perfMax[i])).append(' ');
                perf[i] = perfMax[i] = 0;
            }
            for(PerfCounter c : new PerfCounter[]{PerfCounter.entityUpdate, PerfCounter.unitPhysicsAsync, PerfCounter.unitPhysicsWait, PerfCounter.ui}){
                out.append(c.name()).append(' ').append(Strings.fixed(c.rawValueMs(), 1)).append(' ');
            }
            for(ModuleTimer m : ModuleTimer.all){
                out.append(m.name).append(' ').append(ms(m.total / perfFrames)).append('/').append(ms(m.max)).append(' ');
                m.total = m.max = 0;
            }
            Log.debug("[Perf] @ fps, worst @ms, moving @/@, zoom @, voices @ at @ | avg/max ms: @", perfFrames, (int)perfWorst, perfMoving, perfFrames,
                Strings.fixed(renderer.getScale(), 2), Core.audio.countTotalPlaying(), Time.millis(), out);
            perfTime = perfWorst = 0f;
            perfFrames = perfMoving = 0;
        }
    }

    static String ms(long nanos){
        return Strings.fixed(nanos / (float)Time.nanosPerMilli, 1);
    }


    @Override
    public void update(){
        if(!(Core.input instanceof SwitchInput in) || state == null || Core.scene == null) return;

        long start = Time.nanos();
        logPerf();
        updateControls(in);
        selfNs = Time.nanos() - start;
    }

    void updateControls(SwitchInput in){

        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        if(cursorX < 0){
            cursorX = w / 2f;
            cursorY = h / 2f;
        }

        int buttons = in.getControllerButtons();
        int pressed = buttons & ~lastButtons;
        lastButtons = buttons;

        //a finger on the screen takes over: hide the cursor until the controller is used again,
        //and go back to the touch controls when the finger lands on the game world rather than on the UI
        boolean touching = in.isScreenTouched();
        if(touching && !screenTouched && state.isGame() && !Core.scene.hasMouse() && !(control.input instanceof MobileInput)){
            control.setInput(new MobileInput());
        }
        screenTouched = touching;
        if(touching){
            visible = false;
            pointerActive = false;
            idle = hideTime;
            return;
        }

        float rx = stick(in.getControllerAxis(2)), ry = -stick(in.getControllerAxis(3));
        boolean sticks = rx != 0 || ry != 0 || stick(in.getControllerAxis(0)) != 0 || stick(in.getControllerAxis(1)) != 0;
        boolean active = buttons != 0 || sticks;

        //any controller input switches to the desktop controls
        if((pressed != 0 || sticks) && !(control.input instanceof DesktopInput)){
            control.setInput(new DesktopInput());
        }

        boolean inGame = state.isGame() && !Core.scene.hasDialog();

        //cursor: right stick (squared for precision, faster the longer it is held)
        cursorHeld = rx != 0 || ry != 0 ? Math.min(cursorHeld + Time.delta, cursorRampTime) : 0f;
        float step = Mathf.lerp(cursorMinSpeed, cursorMaxSpeed, cursorHeld / cursorRampTime) * w / 1280f * Time.delta;
        cursorX += rx * Math.abs(rx) * step;
        cursorY += ry * Math.abs(ry) * step;
        if(!inGame){
            if((pressed & btnLeft) != 0) cursorX -= 4f * w / 1280f;
            if((pressed & btnRight) != 0) cursorX += 4f * w / 1280f;
            if((pressed & btnR3) != 0){
                cursorX = w / 2f;
                cursorY = h / 2f;
            }
        }
        cursorX = Mathf.clamp(cursorX, 0, w - 1);
        cursorY = Mathf.clamp(cursorY, 0, h - 1);

        pointerActive |= active;
        //B is the right mouse button in game, and goes back everywhere else
        if(pointerActive) in.injectPointer((int)cursorX, (int)cursorY, (buttons & btnA) != 0, inGame && (buttons & btnB) != 0);
        if((pressed & btnB) != 0 && !inGame) tap(in, KeyCode.back);

        //d-pad up / down scrolls whatever is under the cursor outside the game, repeating while held
        int scroll = inGame ? 0 : (buttons & btnUp) != 0 ? -1 : (buttons & btnDown) != 0 ? 1 : 0;
        if(scroll == 0){
            scrollRepeat = 0;
        }else if((scrollRepeat -= Time.delta) <= 0){
            scrollRepeat = (pressed & (btnUp | btnDown)) != 0 ? 20f : 4f;
            Core.scene.scrolled(0, scroll);
        }

        if((pressed & btnPlus) != 0) tap(in, control.input instanceof DesktopInput ? Binding.menu.value.key : KeyCode.back);
        if((pressed & btnMinus) != 0) tap(in, Binding.pause.value.key);

        if(inGame){
            if((buttons & btnZR) != 0) renderer.scaleCamera(zoomSpeed * Time.delta);
            if((buttons & btnZL) != 0) renderer.scaleCamera(-zoomSpeed * Time.delta);
        }

        if(active){
            idle = 0;
            visible = true;
        }else if((idle += Time.delta / 60f) > hideTime && !(state.isGame() && control.input instanceof DesktopInput)){
            //the cursor is also the aim in game, so it only hides in menus
            visible = false;
        }

        if(visible && pointerActive) draw();
    }

    static void tap(SwitchInput in, KeyCode key){
        if(key == null) return;
        in.injectKey(key, true);
        in.injectKey(key, false);
    }

    static float stick(float value){
        return Math.abs(value) < deadzone ? 0f : (value - Math.signum(value) * deadzone) / (1f - deadzone);
    }

    void draw(){
        float s = Core.graphics.getWidth() / 1280f;
        Draw.proj(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        boolean down = (lastButtons & btnA) != 0;
        float r = (down ? 7f : 9f) * s;

        Lines.stroke(4f * s, Color.black);
        Lines.circle(cursorX, cursorY, r);
        Lines.stroke(2f * s, down ? Pal.accent : Color.white);
        Lines.circle(cursorX, cursorY, r);
        Fill.circle(cursorX, cursorY, 1.5f * s);
        Draw.reset();
        Draw.flush();
    }
}
