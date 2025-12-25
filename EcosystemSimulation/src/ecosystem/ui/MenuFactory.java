package ecosystem.ui;

import javafx.scene.control.*;

public class MenuFactory {
    public static MenuBar createMenuBar(AppController c, Runnable onBgChanged, Runnable onThemeChanged) {
        MenuBar menuBar = new MenuBar();
        // File menu will be populated by SimulationUIManager (New/Open/Save/...)
        Menu menuFile = new Menu("File");
        // View menu controls grid background and theme
        Menu menuView = new Menu("View");
        RadioMenuItem bgImgItem = new RadioMenuItem("Grid Background: Image (icons/grid_background.png)");
        // More generic and accurate name for the non-image background option
        RadioMenuItem bgColorItem = new RadioMenuItem("Grid Background: Color Fill");
        ToggleGroup bgGroup = new ToggleGroup();
        bgImgItem.setToggleGroup(bgGroup);
        bgColorItem.setToggleGroup(bgGroup);
        if (c.getGridBackgroundImage() != null) {
            bgImgItem.setSelected(true);
            c.setUseImageBackground(true);
        } else {
            bgColorItem.setSelected(true);
            c.setUseImageBackground(false);
        }
        menuView.getItems().addAll(bgImgItem, bgColorItem);
        // Theme selection (light / dark)
        menuView.getItems().add(new SeparatorMenuItem());
        RadioMenuItem lightThemeItem = new RadioMenuItem("Theme: Light");
        RadioMenuItem darkThemeItem = new RadioMenuItem("Theme: Dark");
        ToggleGroup themeGroup = new ToggleGroup();
        lightThemeItem.setToggleGroup(themeGroup);
        darkThemeItem.setToggleGroup(themeGroup);
        if (c.isDarkTheme()) {
            darkThemeItem.setSelected(true);
        } else {
            lightThemeItem.setSelected(true);
        }
        menuView.getItems().addAll(lightThemeItem, darkThemeItem);

        // Expose File (filled in elsewhere) and View
        menuBar.getMenus().addAll(menuFile, menuView);
        bgImgItem.setOnAction(e -> { c.setUseImageBackground(true); onBgChanged.run(); });
        bgColorItem.setOnAction(e -> { c.setUseImageBackground(false); onBgChanged.run(); });
        lightThemeItem.setOnAction(e -> { c.setDarkTheme(false); if (onThemeChanged != null) onThemeChanged.run(); });
        darkThemeItem.setOnAction(e -> { c.setDarkTheme(true); if (onThemeChanged != null) onThemeChanged.run(); });
        return menuBar;
    }
}
