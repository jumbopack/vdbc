package org.indp.vdbc.ui;

import com.google.common.base.Strings;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ShortcutAction;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Reindeer;
import org.indp.vdbc.SettingsManager;
import org.indp.vdbc.model.config.ConnectionProfile;
import org.indp.vdbc.services.DatabaseSessionManager;
import org.indp.vdbc.ui.profile.ConnectionProfileLoginPanel;
import org.indp.vdbc.ui.profile.ConnectionProfileSupportService;
import org.indp.vdbc.ui.settings.SettingsManagerDialog;

import java.util.List;

public class ConnectionSelectorComponent extends VerticalLayout {

    private final DatabaseSessionManager databaseSessionManager;

    public ConnectionSelectorComponent(DatabaseSessionManager databaseSessionManager) {
        this.databaseSessionManager = databaseSessionManager;
    }

    @Override
    public void attach() {
        super.attach();

        Panel rootPanel = new Panel("Connect to...");
        rootPanel.setWidth("500px");
        rootPanel.setHeight("250px");
        rootPanel.setContent(createRootLayout(rootPanel));

        setSizeFull();
        addComponent(rootPanel);
        setComponentAlignment(rootPanel, Alignment.MIDDLE_CENTER);
    }

    private Component createRootLayout(Panel rootPanel) {
        List<ConnectionProfile> profiles = SettingsManager.get().getConfiguration().getProfiles();
        return profiles.isEmpty()
                ? createProfileEditorInvite(rootPanel)
                : createConnectionSelectorLayout(rootPanel);
    }

    private Component createConnectionSelectorLayout(final Panel rootPanel) {
        final ProfileInfoPanelHolder<ConnectionProfileLoginPanel> profileInfoPanel = new ProfileInfoPanelHolder<>();
        final HorizontalLayout rootLayout = new HorizontalLayout();

        final Table profilesTable = createProfileSelector(profileInfoPanel);

        Button connectButton = new Button("Connect", new Button.ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                ConnectionProfileLoginPanel panel = profileInfoPanel.getSettingsComponent();
                ConnectionProfile profile = panel.createConnectionProfile();
                try {
                    databaseSessionManager.createSession(profile);
                } catch (Exception ex) {
                    Notification.show("Failed to connect\n", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });
        connectButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        Button testButton = new Button("Test", new Button.ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    ConnectionProfile selectedProfile = (ConnectionProfile) profilesTable.getValue();
                    databaseSessionManager.validateProfile(selectedProfile);
                    Notification.show("Connection successful");
                } catch (Exception ex) {
                    Notification.show("Test failed\n", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });

        Button settingsButton = new Button("Settings...");
        if (!SettingsManager.get().isSettingsEditorEnabled()) {
            settingsButton.setEnabled(false);
            settingsButton.setDescription("Settings editor is disabled because '" +
                                          SettingsManager.VDBC_SETTINGS_EDITOR_ENABLED_PROPERTY +
                                          "' system property is defined and its value is not 'true'.");
        } else {
            settingsButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    SettingsManagerDialog settingsManagerDialog = showSettingsManagerDialog(
                            rootPanel, rootLayout, profileInfoPanel, (ConnectionProfile) profilesTable.getValue());

                    ConnectionProfile selectedProfile = (ConnectionProfile) profilesTable.getValue();
                    if (selectedProfile != null) {
                        settingsManagerDialog.selectProfile(selectedProfile);
                    }
                }
            });
        }

        HorizontalLayout buttonsLayout = new HorizontalLayout(settingsButton, testButton, connectButton);
        buttonsLayout.setWidth("100%");
        buttonsLayout.setComponentAlignment(testButton, Alignment.MIDDLE_RIGHT);
        buttonsLayout.setComponentAlignment(connectButton, Alignment.MIDDLE_RIGHT);

        VerticalLayout settingsPanelLayout = new VerticalLayout(profileInfoPanel, buttonsLayout);
        settingsPanelLayout.setSizeFull();
        settingsPanelLayout.setMargin(new MarginInfo(false, true, true, true));
        settingsPanelLayout.setExpandRatio(profileInfoPanel, 1);

        rootLayout.addComponents(profilesTable, settingsPanelLayout);
        rootLayout.setExpandRatio(settingsPanelLayout, 1);
        rootLayout.setSizeFull();

        return rootLayout;
    }

    private SettingsManagerDialog showSettingsManagerDialog(final Panel rootPanel, final AbstractOrderedLayout currentRootLayout,
                                                            final ProfileInfoPanelHolder<ConnectionProfileLoginPanel> currentProfileInfoPanel,
                                                            final ConnectionProfile selectedProfile) {
        SettingsManagerDialog settingsManagerDialog = new SettingsManagerDialog();
        settingsManagerDialog.addCloseListener(new Window.CloseListener() {
            @Override
            public void windowClose(Window.CloseEvent e) {
                if (currentRootLayout == null) {
                    rootPanel.setContent(createRootLayout(rootPanel));
                } else if (SettingsManager.get().getConfiguration().getProfiles().isEmpty()) {
                    rootPanel.setContent(createProfileEditorInvite(rootPanel));
                } else {
                    Table profileSelector = createProfileSelector(currentProfileInfoPanel);
                    currentRootLayout.replaceComponent(currentRootLayout.getComponent(0), profileSelector);
                    if (profileSelector.containsId(selectedProfile)) {
                        profileSelector.select(selectedProfile);
                    }
                }
            }
        });
        getUI().addWindow(settingsManagerDialog);
        return settingsManagerDialog;
    }

    private Component createProfileEditorInvite(final Panel rootPanel) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);

        if (!SettingsManager.get().isSettingsEditorEnabled()) {
            layout.addComponent(new Label("No connection profiles defined and you are not allowed to create one."));
        } else {
            Button settingsEditorLink = new Button("Open settings editor", new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    showSettingsManagerDialog(rootPanel, null, null, null);
                }
            });
            settingsEditorLink.setStyleName(Reindeer.BUTTON_LINK);

            Label line1 = new Label("No connection profiles defined.");
            line1.setSizeUndefined();

            VerticalLayout inner = new VerticalLayout();
            inner.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
            inner.addComponents(
                    line1,
                    new HorizontalLayout(settingsEditorLink, new Label("&nbsp;to create one.", ContentMode.HTML)));

            layout.addComponents(inner);
        }

        return layout;
    }

    private Table createProfileSelector(final ProfileInfoPanelHolder<ConnectionProfileLoginPanel> infoPanelHolder) {
        List<ConnectionProfile> profileList = SettingsManager.get().getConfiguration().getProfiles();
        Table table = new Table();
        table.setHeight("100%");
        table.setWidth("200px");
        table.setImmediate(true);
        table.setSelectable(true);
        table.setNullSelectionAllowed(false);
        table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);

        table.addGeneratedColumn("color", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                String color = ((ConnectionProfile) itemId).getColor();
                return Strings.isNullOrEmpty(color)
                        ? null
                        : new Label("<div style=\"width: 10px; height: 10px; background-color: #" + color + "\"></div>", ContentMode.HTML);
            }
        });

        table.addContainerProperty("title", String.class, "");
        table.setItemCaptionPropertyId("title");
        table.setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);

        table.setColumnWidth("color", 12);

        for (ConnectionProfile profile : profileList) {
            Item item = table.addItem(profile);
            item.getItemProperty("title").setValue(profile.getName());
        }

        table.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                ConnectionProfile cp = (ConnectionProfile) event.getProperty().getValue();
                if (cp == null) {
                    return;
                }
                ConnectionProfileSupportService<? extends ConnectionProfile> connectionProfileSupportService = ConnectionProfileSupportService.Lookup.find(cp.getClass());
                ConnectionProfileLoginPanel<? extends ConnectionProfile> loginPanel = connectionProfileSupportService.createLoginPanel(cp);
                infoPanelHolder.setSettingsComponent(loginPanel);
                infoPanelHolder.focus();
            }
        });

        if (!profileList.isEmpty()) {
            table.select(profileList.get(0));
        }

        return table;
    }

    protected static class ProfileInfoPanelHolder<T extends Component> extends VerticalLayout {

        private T component;

        public ProfileInfoPanelHolder() {
            setMargin(false);
            setWidth("100%");
        }

        public void setSettingsComponent(T component) {
            removeAllComponents();
            this.component = component;
            addComponent(this.component);
        }

        public T getSettingsComponent() {
            return component;
        }

        public void focus() {
            if (component instanceof Focusable) {
                ((Focusable) component).focus();
            }
        }
    }
}
