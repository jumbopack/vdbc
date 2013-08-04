package org.indp.vdbc.ui.profile.impl;

import org.indp.vdbc.model.config.JndiConnectionProfile;
import org.indp.vdbc.ui.profile.config.AbstractProfileField;
import org.indp.vdbc.ui.profile.impl.fields.ColorField;
import org.indp.vdbc.ui.profile.impl.fields.DialectField;
import org.indp.vdbc.ui.profile.impl.fields.SimpleProfileField;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class JndiConnectionProfileDetailsPanel extends AbstractConnectionProfileDetailsPanel<JndiConnectionProfile> {

    public JndiConnectionProfileDetailsPanel(JndiConnectionProfile profile, ProfileEditorEvents profileEditorEvents) {
        super(profile, profileEditorEvents);
    }

    @Override
    protected List<AbstractProfileField> getFields() {
        return Arrays.asList(
                new SimpleProfileField("name"),
                new DialectField("dialect", "Dialect", true),
                new SimpleProfileField("jndiName", "JNDI Name", true),
                new ColorField("color", "Color", false));
    }
}
