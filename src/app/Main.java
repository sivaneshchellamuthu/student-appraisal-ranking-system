package app;

import store.DataStore;
import ui.LoginFrame;
import ui.UiTheme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default Swing look and feel.
        }
        UiTheme.applyDefaults();

        DataStore store = new DataStore(Paths.get("data"));
        try {
            store.load();
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Could not load data: " + e.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new LoginFrame(store).setVisible(true));
    }
}
