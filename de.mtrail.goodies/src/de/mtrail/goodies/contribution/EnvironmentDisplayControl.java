package de.mtrail.goodies.contribution;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.layout.RowLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.osgi.framework.Bundle;

import de.mtrail.goodies.GoodiesPlugin;
import de.mtrail.goodies.contribution.model.Environment;
import de.mtrail.goodies.contribution.model.EnvironmentPropertiesReader;
import de.mtrail.goodies.internal.launch.ConfigLocationsVariableResolver;
import de.mtrail.goodies.internal.launch.RcsServerProcessLaunchingPreferencePage;

/**
 * Status bar control to switch between target environments for debugging rcs
 * server processes.
 */
public class EnvironmentDisplayControl extends WorkbenchWindowControlContribution {

	private static final String LABEL_TEXT = "${rcs.process.environment} on ${rcs.process.cluster}"; //$NON-NLS-1$
	private static final String ERROR_TEXT = "!%&# Fehler"; //$NON-NLS-1$
	private Label label;
	private IPropertyChangeListener listener;
	private Menu popupMenu;
	private Label imageLabel;
	private final List<Environment> envs;
	private static final String propertiesFileName = "goodies.properties";

	public EnvironmentDisplayControl() {
		final Properties goodieProperties = getProperties();
		this.envs = EnvironmentPropertiesReader.read(goodieProperties);
	}

	private static Properties getProperties() {
		final Properties goodieProperties = new Properties();
		final Bundle thisBundle = GoodiesPlugin.getDefault().getBundle();
		try (InputStream fileStream = FileLocator.openStream(thisBundle, new Path(propertiesFileName), false)) {
			goodieProperties.load(fileStream);
		} catch (final IOException e) {
			throw new RuntimeException("Could not read " + propertiesFileName, e);
		}
		return goodieProperties;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	protected Control createControl(final Composite parent) {
		// Workaround BUG https://bugs.eclipse.org/bugs/show_bug.cgi?id=471313#c12
		parent.getParent().setRedraw(true);

		// Give some room around the control
		final Composite labelContainer = new Composite(parent, SWT.NONE);
		RowLayoutFactory.fillDefaults().margins(2, 2).type(SWT.HORIZONTAL).applyTo(labelContainer);

		imageLabel = new Label(labelContainer, SWT.NONE);

		label = new Label(labelContainer, SWT.NONE);
		label.setText("EMPTYTEXT");
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseUp(final MouseEvent e) {
				if (e.button == 1 || e.button == 2) { // left/middle button
					final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(parent.getShell(),
							RcsServerProcessLaunchingPreferencePage.LAUNCHING_PREFERENCES_ID,
							new String[] { RcsServerProcessLaunchingPreferencePage.LAUNCHING_PREFERENCES_ID }, null);
					dialog.open();
				} else if (e.button == 3) { // right click

				}
			}
		});
		updateLabelText();

		listener = event -> {
			if (event.getProperty().equals(ConfigLocationsVariableResolver.RCS_PROCESS_ENVIRONMENT)) {
				final IContributionManager contributionManager = getParent();

				// With the ContibutionManager we simply call update...
				if (contributionManager != null) {
					contributionManager.update(true);
				} else {
					// ...in E4 we explicitly need to set the text.
					updateLabelText();
				}
			}
		};
		GoodiesPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);

		createPopupMenu(label);
		return labelContainer;
	}

	private void createPopupMenu(final Label aLabel) {
		popupMenu = new Menu(aLabel);

		for (final Environment env : envs) {
			final MenuItem envItem = new MenuItem(popupMenu, SWT.NONE);
			envItem.setText(env.getName() + " (" + env.getCluster() + ")");
			envItem.setImage(GoodiesPlugin.getDefault().getImage(getFlagImageName(env.getName())));
		    envItem.addSelectionListener(new EnvSwitchAdapter(env.getName(), env.getCluster()));
		    popupMenu.addMenuListener(new EnvEnabledAdapter(env.getName(), envItem));
		}
		aLabel.setMenu(popupMenu);
	}

	private String getFlagImageName(final String env) {
		return "/icons/flags/16/" + env.toLowerCase() + ".png"; //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public void dispose() {
		if (null != listener) {
			GoodiesPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(listener);
			listener = null;
		}
		if (popupMenu != null) {
			popupMenu.dispose();
		}
		label.dispose(); // should dispose menu as well
		super.dispose();
	}

	private void updateLabelText() {
		try {
			final String text = substitute(LABEL_TEXT);
			label.setText(text);
			final String env = text.split(" ")[0]; //$NON-NLS-1$
			imageLabel.setImage(GoodiesPlugin.getDefault().getImage(getFlagImageName(env)));
		} catch (final CoreException e) {
			GoodiesPlugin.getDefault().getLog().log(e.getStatus());
			label.setText(ERROR_TEXT);
			imageLabel.setImage(GoodiesPlugin.getDefault().getImage("/icons/view16/failed.gif")); //$NON-NLS-1$
		}
	}

	private static String substitute(String value) throws CoreException {
		final IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		value = mgr.performStringSubstitution(value);
		return value;
	}

	private final static class EnvSwitchAdapter extends SelectionAdapter {

		private final String name;
		private final String cluster;

		EnvSwitchAdapter(final String name, final String cluster) {
			this.name=name;
			this.cluster=cluster;
		}

		@Override
		public void widgetSelected(final SelectionEvent event) {
			Display.getDefault().asyncExec(() -> {
				final IPreferenceStore preferenceStore = GoodiesPlugin.getDefault().getPreferenceStore();
				preferenceStore.setValue(ConfigLocationsVariableResolver.RCS_PROCESS_CLUSTER,
						cluster);
				preferenceStore.setValue(ConfigLocationsVariableResolver.RCS_PROCESS_ENVIRONMENT, name);
			});
		}
	}

	private final static class EnvEnabledAdapter extends MenuAdapter {

		private final String env;
		final MenuItem envItem;

		EnvEnabledAdapter(final String env, final MenuItem envItem) {
			this.env = env;
			this.envItem = envItem;
		}

		@Override
		public void menuShown(final MenuEvent e) {
			final String currentEnv = GoodiesPlugin.getDefault().getPreferenceStore()
					.getString(ConfigLocationsVariableResolver.RCS_PROCESS_ENVIRONMENT);
			envItem.setEnabled(!env.equals(currentEnv));
		}
	}
}