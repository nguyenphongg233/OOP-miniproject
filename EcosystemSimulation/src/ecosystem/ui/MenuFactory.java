package ecosystem.ui;

import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;

public class MenuFactory {
    public static MenuBar createMenuBar(AppController c, Runnable onBgChanged) {
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
        menuBar.getMenus().addAll(menuFile, menuWorld, menuView, menuHelp);
        bgImgItem.setOnAction(e -> { c.setUseImageBackground(true); onBgChanged.run(); });
        bgColorItem.setOnAction(e -> { c.setUseImageBackground(false); onBgChanged.run(); });
        return menuBar;
    }
}
