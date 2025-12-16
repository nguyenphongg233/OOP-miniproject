package ecosystem.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SelectionManager {
    private final ObjectProperty<Integer> selectedId = new SimpleObjectProperty<>(null);

    public ObjectProperty<Integer> getSelectedIdProperty() {
        return selectedId;
    }

    public Integer getSelectedId() {
        return selectedId.get();
    }

    public void select(Integer id) {
        selectedId.set(id);
    }

    public void clear() {
        selectedId.set(null);
    }
}
