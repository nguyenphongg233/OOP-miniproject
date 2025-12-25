package ecosystem.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ecosystem.models.Organism;
import ecosystem.models.OrganismSnapshot;

public class RightPanel {
    private final AppController controller;
    private final VBox root;
    private final ListView<OrganismSnapshot> orgListView;
    private final Label detailLabel;
    private final Label dayLabel;
    private final Label plantLabel;
    private final Label herbLabel;
    private final Label carnLabel;
    private final Label orgLabel;

    public RightPanel(AppController controller) {
        this.controller = controller;
        root = new VBox(10);
        root.setPadding(new Insets(8));
        root.setPrefWidth(220);
        // Retro pixel look: font is controlled globally via CSS, just
        // keep spacing here.

        Label orgListLabel = new Label("Organisms");
        VBox legend = new VBox(8);
        legend.setPadding(new Insets(6, 0, 6, 0));
        Label legendTitle = new Label("Legend:");
        legendTitle.setStyle("-fx-font-weight: bold; -fx-padding: 2 6 0 0;");
        Label plantLegend = new Label("Plant");
        plantLegend.setGraphic(new javafx.scene.shape.Rectangle(14, 12, Color.web("#7fbf7f")));
        Label herbLegend = new Label("Herbivore");
        herbLegend.setGraphic(new javafx.scene.shape.Rectangle(14, 12, Color.DARKGRAY));
        Label carnLegend = new Label("Carnivore");
        carnLegend.setGraphic(new javafx.scene.shape.Rectangle(14, 12, Color.BLACK));
        legend.getChildren().addAll(legendTitle, plantLegend, herbLegend, carnLegend);

        orgListView = new ListView<>();
        orgListView.setPrefHeight(160);
        orgListView.setPrefWidth(200);
        orgListView.setCellFactory(lv -> new ListCell<OrganismSnapshot>() {
            @Override protected void updateItem(OrganismSnapshot o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) setText(null);
                else setText(o.labelId);
            }
        });

        detailLabel = new Label("Click a cell to view organism details");
        detailLabel.setWrapText(true);

        dayLabel = new Label();
        plantLabel = new Label();
        herbLabel = new Label();
        carnLabel = new Label();
        orgLabel = new Label();

        VBox propBox = new VBox(8);
        // Styling for this box is defined in styles.css (.prop-box)
        propBox.getStyleClass().add("prop-box");
        propBox.getChildren().addAll(dayLabel, plantLabel, herbLabel, carnLabel, orgLabel, new Separator(), detailLabel);

        root.getChildren().addAll(orgListLabel, legend, orgListView, new Label("Properties"), propBox);

        // selection wiring (external code should call updateSummary to refresh list)
        orgListView.getSelectionModel().selectedItemProperty().addListener((obs, oldO, newO) -> {
            if (newO == null) {
                controller.getSelectionManager().clear();
                return;
            }
            controller.getSelectionManager().select(newO.id);
        });

        controller.getSelectionManager().getSelectedIdProperty().addListener((obs, oldId, newId) -> {
            if (newId == null) {
                orgListView.getSelectionModel().clearSelection();
                return;
            }
            for (OrganismSnapshot o : orgListView.getItems()) {
                if (o.id == newId.intValue()) { orgListView.getSelectionModel().select(o); orgListView.scrollTo(o); break; }
            }
        });

        updateSummary();
    }

    public VBox getRoot() { return root; }
    public ListView<OrganismSnapshot> getOrgListView() { return orgListView; }
    public Label getDetailLabel() { return detailLabel; }

    public void updateSummary() {
        // Preserve current selection by ID (if any)
        Integer selectedId = controller.getSelectionManager().getSelectedIdProperty().get();
        java.util.Map<String, Integer> counts = controller.getEngine().counts();
        // Model no longer uses separate steps; each tick is one day.
        dayLabel.setText("Day: " + controller.getEngine().getDay());
        plantLabel.setText("Plants: " + counts.getOrDefault("Plant", 0));
        herbLabel.setText("Herbivores: " + counts.getOrDefault("Herbivore", 0));
        carnLabel.setText("Carnivores: " + counts.getOrDefault("Carnivore", 0));
        int total = 0; for (int v : counts.values()) total += v;
        orgLabel.setText("Total Organisms: " + total);
        ObservableList<OrganismSnapshot> items = orgListView.getItems();
        java.util.List<OrganismSnapshot> current = new java.util.ArrayList<>();
        for (Organism o : controller.getEngine().getGrid().getOrganisms()) current.add(OrganismSnapshot.from(o));
        current.sort((a,b) -> Integer.compare(a.id,b.id));
        items.setAll(current);

        // Restore selection highlight in the list view if the
        // previously selected organism is still present.
        if (selectedId != null) {
            for (OrganismSnapshot o : current) {
                if (o.id == selectedId.intValue()) {
                    orgListView.getSelectionModel().select(o);
                    orgListView.scrollTo(o);
                    break;
                }
            }
        }
    }

    public void updateDetailForSelection() {
        Integer selId = controller.getSelectionManager().getSelectedIdProperty().get();
        if (selId == null) { detailLabel.setText("Click a cell to view organism details"); return; }
        ecosystem.models.Organism sel = controller.getEngine().getGrid().getOrganismById(selId);
        if (sel == null) { controller.getSelectionManager().clear(); detailLabel.setText("Click a cell to view organism details"); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(sel.getType()).append("\n");
        sb.append("Position: (").append(sel.getX()).append(", ").append(sel.getY()).append(")\n");
        sb.append("Age: ").append(sel.getAge()).append("\n");
        sb.append("Energy: ").append(sel.getEnergy()).append("\n");
        sb.append("Alive: ").append(sel.isAlive() ? "Yes" : "No").append("\n");
        sb.append("ID: ").append(sel.getId()).append("\n");
        detailLabel.setText(sb.toString());
    }
}
