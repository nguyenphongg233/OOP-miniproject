package ecosystem.ui;

import javafx.scene.control.*;

public class MenuFactory {
    public static MenuBar createMenuBar(AppController c, Runnable onBgChanged, Runnable onThemeChanged) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuWorld = new Menu("World");
        Menu menuHelp = new Menu("Help");
        Menu menuView = new Menu("View");
        RadioMenuItem bgImgItem = new RadioMenuItem("Grid Background: Image (icons/grid_background.png)");
        RadioMenuItem bgColorItem = new RadioMenuItem("Grid Background: Beige");
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
        menuBar.getMenus().addAll(menuFile, menuWorld, menuView, menuHelp);
        bgImgItem.setOnAction(e -> { c.setUseImageBackground(true); onBgChanged.run(); });
        bgColorItem.setOnAction(e -> { c.setUseImageBackground(false); onBgChanged.run(); });
        lightThemeItem.setOnAction(e -> { c.setDarkTheme(false); if (onThemeChanged != null) onThemeChanged.run(); });
        darkThemeItem.setOnAction(e -> { c.setDarkTheme(true); if (onThemeChanged != null) onThemeChanged.run(); });
        return menuBar;
    }
}
