package org.jumpmind.symmetric.is.ui.views;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.EnableFocusTextField;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedApplicationPanel;
import org.jumpmind.symmetric.is.ui.views.design.PropertySheet;
import org.jumpmind.symmetric.is.ui.views.design.ViewProjectsPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class ProjectNavigator extends VerticalLayout {

    ApplicationContext context;

    TabbedApplicationPanel tabs;

    TreeTable treeTable;

    ShortcutListener treeTableEnterKeyShortcutListener;

    ShortcutListener treeTableDeleteKeyShortcutListener;

    AbstractObject itemBeingEdited;

    AbstractObject itemClicked;

    long itemClickTimeInMs;

    PropertySheet designPropertySheet;

    MenuItem newFlow;

    MenuItem newResource;

    MenuItem newModel;

    MenuItem newComponent;

    MenuItem delete;

    MenuItem search;

    MenuItem closeProject;

    VerticalLayout openProjectsLayout;

    HorizontalLayout searchBarLayout;

    List<ProjectVersion> projects = new ArrayList<ProjectVersion>();

    public ProjectNavigator(ApplicationContext context, TabbedApplicationPanel tabs) {
        this.context = context;
        this.tabs = tabs;

        setSizeFull();
        addStyleName(ValoTheme.MENU_ROOT);

        addComponent(buildMenuBar());

        searchBarLayout = buildSearchBar();
        addComponent(searchBarLayout);

        treeTable = buildTreeTable();

    }

    protected HorizontalLayout buildSearchBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(new MarginInfo(false, true, true, true));
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setVisible(false);
        TextField search = new TextField();
        search.setIcon(Icons.SEARCH);
        search.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        search.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(search);
        return layout;
    }

    protected void setMenuItemsEnabled() {
        Object selected = treeTable.getValue();
        boolean newProjectComponentEnabled = treeTable.getValue() != null;
        newComponent.setEnabled(newProjectComponentEnabled);
        newFlow.setEnabled(newProjectComponentEnabled);
        newModel.setEnabled(newProjectComponentEnabled);
        newResource.setEnabled(newProjectComponentEnabled);
        closeProject.setEnabled(selected instanceof ProjectVersion);

        boolean deleteEnabled = false;
        deleteEnabled |= isDeleteButtonEnabled(treeTable.getValue());
        delete.setEnabled(deleteEnabled);

    }

    protected HorizontalLayout buildMenuBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);

        MenuItem menu = leftMenuBar.addItem("Menu", null);

        MenuItem newMenu = menu.addItem("New", null);

        MenuItem viewMenu = menu.addItem("View", null);
        viewMenu.addItem("Projects", Icons.PROJECT, new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                viewProjects();
            }
        });

        closeProject = menu.addItem("Close Project", Icons.FOLDER_CLOSED, new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                closeProject();
            }
        });

        newFlow = newMenu.addItem("Flow", Icons.FLOW, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFlow();
            }
        });

        newModel = newMenu.addItem("Model", Icons.MODEL, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewModel();
            }
        });

        newComponent = newMenu.addItem("Component", Icons.COMPONENT, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });

        newResource = newMenu.addItem("Resource", Icons.GENERAL_RESOURCE, null);
        newResource.setDescription("Add Resource");

        newResource.addItem("Database", Icons.DATABASE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewDatabase();
            }
        });

        newResource.addItem("Local File System", Icons.FILE_SYSTEM, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addNewFileSystem();
            }

        });

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        search = rightMenuBar.addItem("", Icons.SEARCH, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                search.setChecked(!search.isChecked());
                searchBarLayout.setVisible(search.isChecked());
            }
        });

        delete = rightMenuBar.addItem("", Icons.DELETE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                handleDelete();
            }
        });

        layout.addComponent(leftMenuBar);
        layout.addComponent(rightMenuBar);
        layout.setExpandRatio(leftMenuBar, 1);

        return layout;
    }

    public void addProjectVersion(ProjectVersion projectVersion) {
        projects.remove(projectVersion);
        projects.add(projectVersion);
        refresh();
    }

    protected TreeTable buildTreeTable() {
        final TreeTable table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.addStyleName("noselect");
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setItemCaptionMode(ItemCaptionMode.EXPLICIT);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setEditable(true);
        table.setContainerDataSource(new BeanItemContainer<AbstractObject>(AbstractObject.class));

        table.setTableFieldFactory(new DefaultFieldFactory() {
            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId,
                    Component uiContext) {
                return buildEditableNavigatorField(itemId);
            }
        });
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);
        treeTableDeleteKeyShortcutListener = new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delete.isEnabled()) {
                    handleDelete();
                }
            }
        };
        table.addShortcutListener(treeTableDeleteKeyShortcutListener);

        treeTableEnterKeyShortcutListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                openItem(treeTable.getValue());
            }
        };
        table.addShortcutListener(treeTableEnterKeyShortcutListener);
        table.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                selectionChanged(event);
            }
        });
        table.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (event.isDoubleClick()) {
                        abortEditingItem();
                        openItem(event.getItemId());
                        itemClicked = null;
                    } else {
                        if (itemClicked != null && itemClicked.equals(event.getItemId())) {
                            long timeSinceClick = System.currentTimeMillis() - itemClickTimeInMs;
                            if (timeSinceClick > 600 && timeSinceClick < 2000) {
                                startEditingItem(itemClicked);
                            } else {
                                itemClicked = null;
                            }
                        } else if (event.getItemId() instanceof AbstractObject) {
                            itemClicked = (AbstractObject) event.getItemId();
                            itemClickTimeInMs = System.currentTimeMillis();
                        }
                    }
                }
            }
        });
        table.addExpandListener(new ExpandListener() {
            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_OPEN);
                }
            }
        });
        table.addCollapseListener(new CollapseListener() {
            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), Icons.FOLDER_CLOSED);
                }
            }
        });
        table.setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (itemId instanceof Folder && "name".equals(propertyId)) {
                    return "folder";
                } else {
                    return null;
                }

            }
        });

        return table;
    }

    protected Field<?> buildEditableNavigatorField(Object itemId) {
        if (itemBeingEdited != null && itemBeingEdited.equals(itemId)) {
            final EnableFocusTextField field = new EnableFocusTextField();
            field.addStyleName(ValoTheme.TEXTFIELD_SMALL);
            field.setImmediate(true);
            field.addFocusListener(new FocusListener() {

                @Override
                public void focus(FocusEvent event) {
                    field.setFocusAllowed(false);
                    field.selectAll();
                    field.setFocusAllowed(true);
                }
            });
            field.focus();
            field.addShortcutListener(new ShortcutListener("Escape", KeyCode.ESCAPE, null) {

                @Override
                public void handleAction(Object sender, Object target) {
                    abortEditingItem();
                }
            });
            field.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

                private static final long serialVersionUID = 1L;

                @Override
                public void handleAction(Object sender, Object target) {
                    finishEditingItem();
                }
            });
            field.addBlurListener(new BlurListener() {
                @Override
                public void blur(BlurEvent event) {
                    finishEditingItem();
                }
            });
            return field;
        } else {
            return null;
        }
    }

    protected boolean startEditingItem(AbstractObject obj) {
        if (obj.isSettingNameAllowed()) {
            treeTable.removeShortcutListener(treeTableDeleteKeyShortcutListener);
            treeTable.removeShortcutListener(treeTableEnterKeyShortcutListener);
            itemBeingEdited = obj;
            treeTable.refreshRowCache();
            return true;
        } else {
            return false;
        }
    }

    protected void finishEditingItem() {
        if (itemBeingEdited != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            treeTable.addShortcutListener(treeTableDeleteKeyShortcutListener);
            treeTable.addShortcutListener(treeTableEnterKeyShortcutListener);
            Object selected = itemBeingEdited;
            Method method = null;
            try {
                method = configurationService.getClass().getMethod("save",
                        itemBeingEdited.getClass());
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            }
            if (method != null) {
                try {
                    method.invoke(configurationService, itemBeingEdited);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                configurationService.save(itemBeingEdited);
            }
            itemBeingEdited = null;
            treeTable.refreshRowCache();
            treeTable.focus();
            treeTable.setValue(selected);
        }
    }

    protected void abortEditingItem() {
        if (itemBeingEdited != null) {
            Object selected = itemBeingEdited;
            itemBeingEdited = null;
            itemClicked = null;
            refresh();
            treeTable.focus();
            treeTable.setValue(selected);
        }
    }

    public void refresh() {
        refreshOpenProjects();

        removeComponent(treeTable);

        if (openProjectsLayout != null) {
            removeComponent(openProjectsLayout);
        }

        setMenuItemsEnabled();

        if (treeTable.size() == 0) {
            openProjectsLayout = new VerticalLayout();
            openProjectsLayout.setSizeFull();
            openProjectsLayout.setMargin(true);
            Button viewProjects = new Button("Click to open projects");
            viewProjects.addStyleName(ValoTheme.BUTTON_LINK);
            viewProjects.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    viewProjects();
                }
            });
            openProjectsLayout.addComponent(viewProjects);
            openProjectsLayout.setComponentAlignment(viewProjects, Alignment.TOP_CENTER);
            addComponent(openProjectsLayout);
            setExpandRatio(openProjectsLayout, 1);
        } else {
            addComponent(treeTable);
            setExpandRatio(treeTable, 1);
            treeTable.refreshRowCache();
        }
    }

    protected void refreshOpenProjects() {
        // add any open projects to the tree table. check cookies
        
        Iterator<ProjectVersion> i = projects.iterator();
        while (i.hasNext()) {
            ProjectVersion projectVersion = i.next();
            if (projectVersion.isDeleted() || projectVersion.getProject().isDeleted()) {
                i.remove();
            }
        }

        Collections.sort(projects, new Comparator<ProjectVersion>() {
            @Override
            public int compare(ProjectVersion o1, ProjectVersion o2) {
                return o1.getProject().getName().compareTo(o2.getProject().getName());
            }
        });

        treeTable.removeAllItems();

        for (ProjectVersion projectVersion : projects) {
            treeTable.addItem(projectVersion);
            treeTable.setItemIcon(projectVersion, Icons.PROJECT);
            treeTable.setItemCaption(projectVersion, projectVersion.getProject().getName());
            treeTable.setChildrenAllowed(projectVersion, true);

            addFlowsToFolder(addVirtualFolder("Flows", projectVersion), projectVersion);
            addModelsToFolder(addVirtualFolder("Models", projectVersion), projectVersion);
            addResourcesToFolder(addVirtualFolder("Resources", projectVersion), projectVersion);
            // addComponentsToFolder(addFolder("Components", projectVersion),
            // projectVersion);
        }
    }

    protected Folder addVirtualFolder(String name, ProjectVersion projectVersion) {
        String folderId = name + "-" + projectVersion.getId();
        Folder folder = new Folder();
        folder.makeVirtual();
        folder.getProjectVersionId();
        folder.setId(folderId);
        folder.setName(name);

        treeTable.addItem(folder);
        treeTable.setItemIcon(folder, Icons.FOLDER_CLOSED);
        treeTable.setItemCaption(folder, name);
        treeTable.setParent(folder, projectVersion);
        return folder;
    }

    protected void addResourcesToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Resource> resources = configurationService.findResourcesInProject(projectVersion
                .getId());
        for (Resource resource : resources) {
            this.treeTable.addItem(resource);
            if (DataSourceResource.TYPE.equals(resource.getType())) {
                this.treeTable.setItemIcon(resource, Icons.DATABASE);
            } else {
                this.treeTable.setItemIcon(resource, Icons.GENERAL_RESOURCE);
            }
            this.treeTable.setChildrenAllowed(resource, false);
            this.treeTable.setParent(resource, folder);
        }

    }

    protected void addFlowsToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Flow> flows = configurationService.findFlowsInProject(projectVersion.getId());
        for (Flow flow : flows) {
            this.treeTable.addItem(flow);
            this.treeTable.setItemIcon(flow, Icons.FLOW);
            this.treeTable.setParent(flow, folder);

            List<FlowStep> flowSteps = flow.getFlowSteps();

            this.treeTable.setChildrenAllowed(flow, flowSteps.size() > 0);

            for (FlowStep flowStep : flowSteps) {
                this.treeTable.addItem(flowStep);
                this.treeTable.setItemCaption(flowStep, flowStep.getName());
                this.treeTable.setItemIcon(flowStep, Icons.COMPONENT);
                this.treeTable.setParent(flowStep, flow);
                this.treeTable.setChildrenAllowed(flowStep, false);
            }
        }
    }

    protected void addModelsToFolder(Folder folder, ProjectVersion projectVersion) {
        IConfigurationService configurationService = context.getConfigurationService();
        List<Model> models = configurationService.findModelsInProject(projectVersion.getId());
        for (Model model : models) {
            this.treeTable.addItem(model);
            this.treeTable.setItemIcon(model, Icons.MODEL);
            this.treeTable.setParent(model, folder);
            this.treeTable.setChildrenAllowed(model, false);
        }
    }

    protected void selectionChanged(ValueChangeEvent event) {
        setMenuItemsEnabled();
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return false;
    }

    protected void openItem(Object item) {
    }

    protected void viewProjects() {
        tabs.addCloseableTab("projectslist", "View Projects", Icons.PROJECT, new ViewProjectsPanel(
                context, this));
    }

    protected void closeProject() {
        Object selected = treeTable.getValue();
        if (selected instanceof ProjectVersion) {
            projects.remove(selected);
            refresh();
        }
    }

    protected void handleDelete() {

    }

    protected void addNewFileSystem() {

    }

    protected void addNewDatabase() {

    }

    protected Folder findFolderWithName(String name) {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            Collection<?> children = treeTable.getChildren(value);
            for (Object object : children) {
                if (object instanceof Folder) {
                    Folder folder = (Folder) object;
                    if (folder.getName().equals(name)) {
                        return folder;
                    }
                }
            }
        }
        return null;
    }

    protected ProjectVersion findProjectVersion() {
        Object value = treeTable.getValue();
        while (!(value instanceof ProjectVersion) && value != null) {
            value = treeTable.getParent(value);
        }

        if (value instanceof ProjectVersion) {
            return ((ProjectVersion) value);
        } else {
            return null;
        }
    }

    protected void addNewFlow() {

        Folder folder = findFolderWithName("Flows");
        if (folder != null) {
            ProjectVersion projectVersion = findProjectVersion();
            Flow flow = new Flow();
            flow.setProjectVersionId(projectVersion.getId());
            flow.setName("New Flow");
            context.getConfigurationService().save(flow);
            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);
            treeTable.setChildrenAllowed(flow, false);

            treeTable.setCollapsed(folder, false);
            treeTable.setCollapsed(projectVersion, false);
            treeTable.setValue(flow);

            startEditingItem(flow);
        }
    }

    protected void addNewComponent() {

    }

    protected void addNewModel() {

    }

}