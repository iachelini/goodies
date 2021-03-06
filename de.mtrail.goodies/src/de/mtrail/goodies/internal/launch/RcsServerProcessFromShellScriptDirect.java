/*
 * Project: RCS - Rail Control System
 *
 * © Copyright by SBB AG, Alle Rechte vorbehalten
 */
package de.mtrail.goodies.internal.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;

/**
 * Shortcut to launch an RCS server process directly based on parameters from a shell script.
 */
public class RcsServerProcessFromShellScriptDirect extends RcsServerProcessFromShellScriptBase {

	@Override
	protected void doLaunch(ILaunchConfiguration config, String mode) {
		DebugUITools.launch(config, mode);
	}

}
