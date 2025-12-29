/**
 * 
 */

package ecosystem.ui;

import javafx.scene.control.*;

public class MenuFactory {
    public static MenuBar createMenuBar(AppController c, Runnable onBgChanged, Runnable onThemeChanged, Runnable onRegenerateEnv) {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");
        Menu menuWorld = new Menu("World");
        Menu menuView = new Menu("View");
        
        // Regenerate Environment menu item
        MenuItem regenItem = new MenuItem("Regenerate Environment");
        regenItem.setOnAction(e -> { if (onRegenerateEnv != null) onRegenerateEnv.run(); });
        menuWorld.getItems().add(regenItem);

        menuWorld.getItems().add(new SeparatorMenuItem());
        ToggleGroup bgGroup = new ToggleGroup();
        RadioMenuItem bgImgItem = new RadioMenuItem("Grid Background: Default");
        RadioMenuItem bgColorItem = new RadioMenuItem("Grid Background: Generated Environment");
        bgImgItem.setToggleGroup(bgGroup);
        bgColorItem.setToggleGroup(bgGroup);

        if (c.getGridBackgroundImage() != null) {
            bgImgItem.setSelected(true);
            c.setUseImageBackground(true);
        } else {
            bgColorItem.setSelected(true);
            c.setUseImageBackground(false);
        }
        menuWorld.getItems().addAll(bgImgItem, bgColorItem);
        // Theme selection (light / dark)
        menuWorld.getItems().add(new SeparatorMenuItem());
        RadioMenuItem lightThemeItem = new RadioMenuItem("Theme: Light");
        RadioMenuItem darkThemeItem = new RadioMenuItem("Theme: Dark");
        ToggleGroup themeGroup = new ToggleGroup();
        lightThemeItem.setToggleGroup(themeGroup);
        darkThemeItem.setToggleGroup(themeGroup);

        if (c.isDarkTheme())darkThemeItem.setSelected(true);
        else lightThemeItem.setSelected(true);
        
        menuWorld.getItems().addAll(lightThemeItem, darkThemeItem);
        menuBar.getMenus().addAll(menuFile, menuWorld,menuView);

        bgImgItem.setOnAction(e -> { c.setUseImageBackground(true); onBgChanged.run(); });
        bgColorItem.setOnAction(e -> { c.setUseImageBackground(false); onBgChanged.run(); });
        lightThemeItem.setOnAction(e -> { c.setDarkTheme(false); if (onThemeChanged != null) onThemeChanged.run(); });
        darkThemeItem.setOnAction(e -> { c.setDarkTheme(true); if (onThemeChanged != null) onThemeChanged.run(); });
        return menuBar;
    }
}
