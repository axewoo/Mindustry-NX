package com.thelogicmaster.switchgdx;

import arc.Core;
import arc.backend.switchgdx.SwitchApplication;
import arc.files.Fi;
import arc.util.Log;
import mindustry.ClientLauncher;
import mindustry.Vars;
import mindustry.nx.SwitchControls;

public class SwitchLauncher extends ClientLauncher {

	@Override
	public ClassLoader loadJar(Fi jar, ClassLoader parent) throws Exception {
		return parent;
	}

	@Override
	public void init() {
		super.init();
		// Added after the game's listener, so the controller cursor draws on top of everything
		Core.app.post(() -> Core.app.addListener(new SwitchControls()));
	}

	public static void main (String[] arg) {
		Log.level = Log.LogLevel.debug;
		Vars.loadLogger();
		new SwitchApplication(new SwitchLauncher(), new SwitchApplication.Config());
	}
}
