package de.mtrail.goodies.internal.workspacesupport.util;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.mtrail.goodies.internal.GoodiesPreferenceConstants;

public class PreferenceSupportTest {
	private static final String A_VALID_VALUE = "A_VALID_VALUE";

	IPreferenceStore store;
	private PreferenceSupport supportUnderTest;

	@Before
	public void before() {
		store = new PreferenceStore();
		supportUnderTest = new PreferenceSupport(store);
	}

	@Test
	public void checkPreferences_shouldNotFail_whenWorkspaceConfigLocationIsSet() throws ExecutionException {
		// GIVEN
		store.setValue(GoodiesPreferenceConstants.WORKSPACE_CONFIG_LOCATION, A_VALID_VALUE);

		// WHEN
		supportUnderTest.checkPreferences();

		// THEN
		Assert.assertThat(supportUnderTest.getPathString(), CoreMatchers.is(A_VALID_VALUE));
	}
}
