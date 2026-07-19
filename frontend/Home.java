// Save this as Digital-Steganography-Using-JAVA-main_scramble_copy/frontend/Home.java
package frontend;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Home extends JFrame {

    public Home() {
        setTitle("Digital Steganography");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int width = 1200;
        int height = 650; // Adjusted height for better layout fit
        setSize(width, height);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());

        // Load the image
        ImageIcon placeholderIcon = new ImageIcon("frontend/Images/home.png"); 
        Image image = placeholderIcon.getImage();

        // Scale the image down
        Image scaledImage = image.getScaledInstance(800, 500, Image.SCALE_SMOOTH); 
        ImageIcon scaledPlaceholderIcon = new ImageIcon(scaledImage); 

        // Use the scaled image in the JLabel
        JLabel imageLabel = new JLabel(scaledPlaceholderIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50)); 
        panel.add(imageLabel, BorderLayout.CENTER);

        // Encode and Decode buttons 
        JButton encodeButton = new JButton("ENCODE");
        JButton decodeButton = new JButton("DECODE");

        // size for the buttons
        Dimension buttonSize = new Dimension(200, 80);
        encodeButton.setPreferredSize(buttonSize);
        decodeButton.setPreferredSize(buttonSize);

        // text size and styling
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        encodeButton.setFont(customFont);
        decodeButton.setFont(customFont);

        Color purpleColor = new Color(118, 57, 111);
        Border purpleBorder = BorderFactory.createLineBorder(purpleColor, 4);
        encodeButton.setBorder(purpleBorder);
        decodeButton.setBorder(purpleBorder);

        Color buttonTextColor = new Color(118, 57, 111);
        Font buttonFont = new Font("Epilogue", Font.BOLD, 20);
        
        encodeButton.setForeground(buttonTextColor);
        encodeButton.setFont(buttonFont);

        decodeButton.setForeground(buttonTextColor);
        decodeButton.setFont(buttonFont);


        // Add action listeners 
        encodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // *** MODIFIED: Launch the selection page for ENCODE flow ***
                EncodeTypeSelection selectionPage = new EncodeTypeSelection(true); 
                selectionPage.setVisible(true);
                setVisible(false);
            }
        });

        decodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // *** MODIFIED: Launch the selection page for DECODE flow ***
                // This replaces the old 'TextAndImage' call with the new hierarchical flow.
                EncodeTypeSelection selectionPage = new EncodeTypeSelection(false); 
                selectionPage.setVisible(true);
                setVisible(false);
            }
        });

        // --- BUTTON ASSEMBLY ---
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        buttonPanel.setBackground(new Color(200, 200, 200)); 

        buttonPanel.add(encodeButton); 
        buttonPanel.add(decodeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Home().setVisible(true);
            }
        });
    }
}
