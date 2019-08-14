package de.mtrail.goodies.internal.workspacesupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import de.mtrail.goodies.internal.workspacesupport.model.State;
import de.mtrail.goodies.internal.workspacesupport.util.FeatureUtility;
import de.mtrail.goodies.internal.workspacesupport.util.WorkspaceProperties;
import de.mtrail.goodies.internal.workspacesupport.util.WorkspacePropertiesConstants;
import de.mtrail.goodies.internal.workspacesupport.util.WorkspaceUtility;

/**
 * Reads all the bundles from the workspace and creates a workspace.properties
 * file. Bundles will be set to their current state (open/closed) and put in a
 * workspace according to the feature they belong to.
 */
public final class CreateWorkspacePropertiesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// Bundles from the workspace
		Map<String, IProject> projectIndex = WorkspaceUtility.createWorkspaceProjectIndex();

		// create map of plugin->feature for workingset assignment
		Map<String, String> pluginFeatureIndex = createPluginFeatureIndex(projectIndex);

		// now create the Properties instance
		WorkspaceProperties properties = createProperties(projectIndex.values(), pluginFeatureIndex);

		// Let user decide where to save
		String filename = createFileSaveDialog().open();
		if (filename != null) {
			properties.write(filename);
		}

		return null;
	}

	private WorkspaceProperties createProperties(Collection<IProject> bundles,
			Map<String, String> bundleWorkingSetIdx) {
		WorkspaceProperties properties = new WorkspaceProperties();

		for (IProject bundle : bundles) {
			String key = bundle.getName();
			
			properties.put(key + "." + WorkspacePropertiesConstants.STATE, getState(bundle));

			String workingSetName = bundleWorkingSetIdx.get(key);
			properties.put(key + "." + WorkspacePropertiesConstants.WORKINGSET, workingSetName == null ? "" : workingSetName);
		}
		return properties;
	}

	private String getState(IProject bundle) {
		return bundle.isOpen() ? State.open.toString() : State.closed.toString();
	}

	private FileDialog createFileSaveDialog() {
		FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
		dialog.setFilterNames(new String[] { "Properties File", "All Files (*.*)" });
		dialog.setFilterExtensions(new String[] { "*.properties", "*.*" }); // Windows
		dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getFullPath().toString()); // Workspace Directory
		dialog.setFileName("workspace.properties");
		return dialog;
	}

	private Map<String, String> createPluginFeatureIndex(Map<String, IProject> projectIndex) {
		Map<String, String> plugInFeatureIdx = new HashMap<>();
		List<IProject> alLFeaturesFromWorkspace = projectIndex.values().stream().filter(FeatureUtility::isFeature)
				.collect(Collectors.toList());
		for (IProject featureProject : alLFeaturesFromWorkspace) {
			IProject[] referencedPlugins = FeatureUtility.getReferencedPlugins(featureProject);
			for (IProject aPlugin : referencedPlugins) {
				plugInFeatureIdx.put(aPlugin.getName(), featureProject.getName());
			}
		}
		return plugInFeatureIdx;
	}
}