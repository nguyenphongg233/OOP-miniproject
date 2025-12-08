package ecosystem.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Simple tracker that stores the selected organism id (may be null).
 * UI components can listen to {@link #selectedIdProperty()} to react.
 */
public class SelectedOrganism {
    private final ObjectProperty<Integer> selectedId = new SimpleObjectProperty<>(null);

    public ObjectProperty<Integer> selectedIdProperty() { return selectedId; }

    public Integer getSelectedId() { return selectedId.get(); }

    public void select(Integer id) { selectedId.set(id); }

    public void clear() { selectedId.set(null); }
}
