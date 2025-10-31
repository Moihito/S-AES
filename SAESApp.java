import javax.swing.SwingUtilities;

public class SAESApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SAESGui().createAndShow());
    }
}
