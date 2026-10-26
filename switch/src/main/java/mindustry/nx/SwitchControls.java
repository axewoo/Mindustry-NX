package mindustry.nx;

import arc.*;
import arc.backend.switchgdx.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.graphics.*;
import mindustry.input.*;

import static mindustry.Vars.*;

/**
 * Joy-Con / Pro Controller support on top of Mindustry's touch controls.
 * The right stick moves an on-screen cursor that acts as a finger: A taps, holding A long-presses
 * (shoot / line placement) and dragging with A held drags. The left stick moves the camera,
 * which the player's unit follows, as when panning with a finger.
 *
 * A tap / hold / drag, B back or cancel, X break mode, Y command mode,
 * L / R rotate block, ZL / ZR zoom, d-pad up / down scroll, d-pad left / right nudge the cursor,
 * + pause menu, - pause, R3 recenter the cursor.
 */
public class SwitchControls implements ApplicationListener{
    static final int
    btnA = 1, btnB = 1 << 1, btnX = 1 << 2, btnY = 1 << 3, btnR3 = 1 << 5,
    btnL = 1 << 6, btnR = 1 << 7, btnZL = 1 << 8, btnZR = 1 << 9, btnPlus = 1 << 10, btnMinus = 1 << 11,
    btnLeft = 1 << 12, btnUp = 1 << 13, btnRight = 1 << 14, btnDown = 1 << 15;

    static final float deadzone = 0.15f, cameraSpeed = 14f, zoomSpeed = 0.06f;
    /** Cursor speed in pixels per frame at 1280 wide: it starts slow for precision and speeds up while the stick is held. */
    static final float cursorMinSpeed = 3.5f, cursorMaxSpeed = 10f, cursorRampTime = 40f;
    /** Seconds without cursor input before the cursor hides. */
    static final float hideTime = 6f;

    float cursorX = -1, cursorY = -1, idle = hideTime, scrollRepeat, cursorHeld;
    int lastButtons;
    boolean visible;

    @Override
    public void update(){
        if(!(Core.input instanceof SwitchInput in) || state == null || Core.scene == null) return;

        float w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        if(cursorX < 0){
            cursorX = w / 2f;
            cursorY = h / 2f;
        }

        int buttons = in.getControllerButtons();
        int pressed = buttons & ~lastButtons, released = lastButtons & ~buttons;
        lastButtons = buttons;

        //a finger on the screen takes over: hide the cursor until the controller is used again
        if(in.isScreenTouched()){
            visible = false;
            idle = hideTime;
            return;
        }

        boolean active = buttons != 0;

        //cursor: right stick (squared for precision, faster the longer it is held), d-pad left / right nudges
        float rx = stick(in.getControllerAxis(2)), ry = -stick(in.getControllerAxis(3));
        cursorHeld = rx != 0 || ry != 0 ? Math.min(cursorHeld + Time.delta, cursorRampTime) : 0f;
        float step = Mathf.lerp(cursorMinSpeed, cursorMaxSpeed, cursorHeld / cursorRampTime) * w / 1280f * Time.delta;
        cursorX += rx * Math.abs(rx) * step;
        cursorY += ry * Math.abs(ry) * step;
        if((pressed & btnLeft) != 0) cursorX -= 4f * w / 1280f;
        if((pressed & btnRight) != 0) cursorX += 4f * w / 1280f;
        if((pressed & btnR3) != 0){
            cursorX = w / 2f;
            cursorY = h / 2f;
        }
        cursorX = Mathf.clamp(cursorX, 0, w - 1);
        cursorY = Mathf.clamp(cursorY, 0, h - 1);
        active |= rx != 0 || ry != 0;

        in.injectPointer((int)cursorX, (int)cursorY, (buttons & btnA) != 0);

        //d-pad up / down scrolls whatever is under the cursor (and zooms in game), repeating while held
        int scroll = (buttons & btnUp) != 0 ? -1 : (buttons & btnDown) != 0 ? 1 : 0;
        if(scroll == 0){
            scrollRepeat = 0;
        }else if((scrollRepeat -= Time.delta) <= 0){
            scrollRepeat = (pressed & (btnUp | btnDown)) != 0 ? 20f : 4f;
            Core.scene.scrolled(0, scroll);
        }

        if((pressed & btnPlus) != 0) tap(in, KeyCode.back);
        if((pressed & btnMinus) != 0) tap(in, Binding.pause.value.key);

        if((pressed & btnB) != 0){
            if(!back()) tap(in, KeyCode.back);
        }

        if(state.isGame() && !Core.scene.hasDialog() && control.input instanceof MobileInput input){
            //camera: left stick; the player's unit follows the camera on touch controls
            float lx = stick(in.getControllerAxis(0)), ly = -stick(in.getControllerAxis(1));
            if(lx != 0 || ly != 0){
                float speed = cameraSpeed * Core.camera.width / w * Time.delta;
                Core.camera.position.add(lx * speed, ly * speed);
                Core.camera.position.clamp(-Core.camera.width / 4f, -Core.camera.height / 4f,
                    world.unitWidth() + Core.camera.width / 4f, world.unitHeight() + Core.camera.height / 4f);
            }

            if((buttons & btnZR) != 0) renderer.scaleCamera(zoomSpeed * Time.delta);
            if((buttons & btnZL) != 0) renderer.scaleCamera(-zoomSpeed * Time.delta);

            if((pressed & btnL) != 0) input.rotation = Mathf.mod(input.rotation + 1, 4);
            if((pressed & btnR) != 0) input.rotation = Mathf.mod(input.rotation - 1, 4);

            if((pressed & btnX) != 0){
                input.mode = input.mode == PlaceMode.breaking ? input.block == null ? PlaceMode.none : PlaceMode.placing : PlaceMode.breaking;
            }

            if((pressed & btnY) != 0 && !input.locked()){
                input.commandMode = !input.commandMode;
                if(input.commandMode){
                    input.block = null;
                    input.rebuildMode = false;
                    input.mode = PlaceMode.none;
                }
            }
        }

        if(active){
            idle = 0;
            visible = true;
        }else if((idle += Time.delta / 60f) > hideTime){
            visible = false;
        }

        if(visible) draw();
    }

    /** Cancels block placement or selected plans in game. Returns whether there was something to cancel. */
    boolean back(){
        if(!state.isGame() || Core.scene.hasDialog() || !(control.input instanceof MobileInput input)) return false;

        if(input.block != null || !input.selectPlans.isEmpty() || input.mode != PlaceMode.none || input.commandMode){
            input.block = null;
            input.selectPlans.clear();
            input.mode = PlaceMode.none;
            input.commandMode = false;
            return true;
        }
        return false;
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
