import ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point.
 *
 * Compile and run notes are in README.txt.
 */
public class Main {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info
                    : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the platform look and feel.
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
