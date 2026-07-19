package frontend;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextAndImage extends JFrame {

    private static final String BACKGROUND_IMAGE_PATH = "home.png"; 
    private boolean isEncodeFlow; // true for Encode, false for Decode

    public TextAndImage(boolean isEncodeFlow) {
        this.isEncodeFlow = isEncodeFlow;
        
        setTitle("Choose Image Security Method");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int width = 1200;
        int height = 650; // Set height consistent with Home.java
        setSize(width, height);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());

        // --- IMAGE SETUP (Matches Home.java look and feel) ---
        ImageIcon placeholderIcon = new ImageIcon(BACKGROUND_IMAGE_PATH);
        Image image = placeholderIcon.getImage(); 
        
        // Scale the image down 
        Image scaledImage = image.getScaledInstance(800, 500, Image.SCALE_SMOOTH); 
        ImageIcon scaledPlaceholderIcon = new ImageIcon(scaledImage); 

        // Use the scaled image in the JLabel
        JLabel imageLabel = new JLabel(scaledPlaceholderIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50)); 
        panel.add(imageLabel, BorderLayout.CENTER);
        // ------------------------------------------

        // --- BUTTONS ---
        JButton imageAesButton = createCustomButton("Image AES/LSB", "Arial", Font.BOLD, 20); 
        JButton audioAesButton = createCustomButton("Audio Steganography", "Arial", Font.BOLD, 20); 
        JButton scrambleButton = createCustomButton("Image Scramble", "Arial", Font.BOLD, 20); 
        
        // NEW BUTTON: Feature with secret message capability
        JButton imageAesMessageButton = createCustomButton("Image AES/LSB + Message", "Arial", Font.BOLD, 20);


        // Set consistent button size
        Dimension buttonSize = new Dimension(200, 80);
        imageAesButton.setPreferredSize(buttonSize);
        audioAesButton.setPreferredSize(buttonSize);
        scrambleButton.setPreferredSize(buttonSize);
        imageAesMessageButton.setPreferredSize(buttonSize); // Set size for new button
        
        // --- LISTENERS ---
        
        // 1. Image AES/LSB Listener (Original)
        imageAesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (TextAndImage.this.isEncodeFlow) {
                    new UploadPageEncode().setVisible(true);
                } else {
                    new UploadPageDecode().setVisible(true);
                }
                setVisible(false);
            }
        });

        // 2. Audio Steganography Listener (Unchanged)
        audioAesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (TextAndImage.this.isEncodeFlow) {
                    new AudioPageEncode().setVisible(true);
                } else {
                    new AudioPageDecode().setVisible(true);
                }
                setVisible(false); // Close the current frame
            }
        });
        
        // 3. Image Scramble Listener (Unchanged)
        scrambleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UploadPageScramble scramblePage = new UploadPageScramble(TextAndImage.this.isEncodeFlow);
                scramblePage.setVisible(true);
                setVisible(false); 
            }
        });
        
        // NEW LISTENER: Image AES/LSB + Message
        imageAesMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (TextAndImage.this.isEncodeFlow) {
                    new ImageAesMessageEncode().setVisible(true); // Launch new Encode page
                } else {
                    new ImageAesMessageDecode().setVisible(true); // Launch new Decode page
                }
                setVisible(false);
            }
        });


        // --- BUTTON ASSEMBLY (Layout.SOUTH) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 0)); // Horizontal gap
        
        Color buttonPanelColor = new Color(200, 200, 200); // Light Gray
        buttonPanel.setBackground(buttonPanelColor); 

        // Add buttons in the desired order
        buttonPanel.add(scrambleButton);
        buttonPanel.add(imageAesButton);
        buttonPanel.add(imageAesMessageButton); // ADD THE NEW BUTTON
        buttonPanel.add(audioAesButton); 

        panel.add(buttonPanel, BorderLayout.SOUTH);
        // ------------------------------------------

        setContentPane(panel);
        setLocationRelativeTo(null);
    }
    
    private JButton createCustomButton(String text, String fontName, int fontStyle, int fontSize) {
        JButton button = new JButton(text);

        Font customFont = new Font(fontName, fontStyle, fontSize);
        button.setFont(customFont);

        // Styling matches Home.java ENCODE/DECODE buttons
        Color purpleColor = new Color(118, 57, 111); 
        Border purpleBorder = BorderFactory.createLineBorder(purpleColor, 4); 
        
        button.setBorder(purpleBorder);
        button.setForeground(purpleColor);
        button.setFont(new Font("Epilogue", Font.BOLD, fontSize)); 

        return button;
    }
}
