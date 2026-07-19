package frontend;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class EncodeTypeSelection extends JFrame {

    private JPanel dynamicButtonPanel; // This holds the flow buttons (Image Encrypt, Secret Msg, etc.)
    private JButton backButton;
    private String currentView = "initial"; 
    private boolean isEncodeFlow;

    private static final String BACKGROUND_IMAGE_PATH = "frontend/Images/home_3.jpg"; 

    public EncodeTypeSelection(boolean isEncodeFlow) { 
        this.isEncodeFlow = isEncodeFlow;
        String flowType = isEncodeFlow ? "ENCODE" : "DECODE";
        
        setTitle("Choose " + flowType + " Flow");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Window Size (1200x650)
        int width = 1200;
        int height = 650;
        
        setSize(width, height);
        setResizable(false);
        setLocationRelativeTo(null); 

        // Main panel using BorderLayout
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // No main padding

        // --- 1. IMAGE DISPLAY & HEADER (CENTER) ---
        
        // Load and scale the image exactly like Home.java
        ImageIcon placeholderIcon = new ImageIcon(BACKGROUND_IMAGE_PATH); 
        Image image = placeholderIcon.getImage();
        
        // Scale the image down to a fixed size (matching Home.java's visual area)
        Image scaledImage = image.getScaledInstance(950, 390, Image.SCALE_SMOOTH); 
        ImageIcon scaledPlaceholderIcon = new ImageIcon(scaledImage); 

        // JLabel for the scaled image, centered visually
        JLabel imageLabel = new JLabel(scaledPlaceholderIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50)); 
        
        // Wrapper for the header and image
        JPanel centerWrapper = new JPanel(new BorderLayout()); 
        centerWrapper.setOpaque(true); 
        
        // Header Text
        JLabel headerLabel = new JLabel("CHOOSE " + flowType + " TYPE", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 30));
        
        centerWrapper.add(headerLabel, BorderLayout.NORTH);
        centerWrapper.add(imageLabel, BorderLayout.CENTER);
        panel.add(centerWrapper, BorderLayout.CENTER); 
        // -----------------------------------


        // --- 2. BUTTONS (SOUTH) ---
        // Create the final bottom panel container (mainSouthPanel)
        JPanel mainSouthPanel = new JPanel(); 
        mainSouthPanel.setLayout(new BoxLayout(mainSouthPanel, BoxLayout.Y_AXIS)); 
        mainSouthPanel.setOpaque(true); 
        mainSouthPanel.setBackground(new Color(200, 200, 200)); // Light gray background

        // Flow Button Area (Action Buttons)
        // FlowLayout for horizontal alignment of the main action buttons
        dynamicButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10)); // Top padding 10
        dynamicButtonPanel.setOpaque(false); 
        mainSouthPanel.add(dynamicButtonPanel); 
        
        // Navigation Button Area
        JPanel navButtonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10)); // Top padding 10
        navButtonContainer.setOpaque(false);

        backButton = createStyledButton("Back", new Dimension(120, 40)); 
        backButton.setVisible(false);
        backButton.addActionListener(e -> navigateBack());

        JButton homeButton = createStyledButton("Back to Home", new Dimension(150, 40)); 
        homeButton.addActionListener(e -> goHome());
        
        navButtonContainer.add(backButton);
        navButtonContainer.add(homeButton);

        mainSouthPanel.add(navButtonContainer); 
        
        panel.add(mainSouthPanel, BorderLayout.SOUTH);
        // --------------------------

        setContentPane(panel);
        
        // Initial setup
        showInitialOptions();
    }
    
    // --- Navigation Logic ---

    private void navigateBack() {
        if (currentView.equals("image") || currentView.equals("message")) {
            showInitialOptions();
        }
    }

   private void showInitialOptions() {
    // Instead of showing the two initial buttons, 
    // we go straight to the secret message sub-menu.
    showSecretMessageOptions();
    
    // Ensure the Back button is hidden since this is now our starting screen
    backButton.setVisible(false); 
    
    dynamicButtonPanel.revalidate();
    dynamicButtonPanel.repaint();
    
    // Update the title to match the new starting point
    String action = isEncodeFlow ? "Encryption" : "Decryption";
    setTitle("Choose " + action + " Type");
    currentView = "initial"; 
}

    
    private void showSecretMessageOptions() {
        dynamicButtonPanel.removeAll();
        
        String audioText = isEncodeFlow ? "Audio Encryption" : "Audio Decryption";
        String videoText = isEncodeFlow ? "Video Encryption" : "Video Decryption";
        String imageText = isEncodeFlow ? "Image Encryption" : "Image Decryption"; 

        // 1. Audio Button
        JButton audioButton = createStyledButton(audioText, new Dimension(200, 80));
        audioButton.addActionListener(e -> {
            launchPage(isEncodeFlow ? new AudioPageEncode() : new AudioPageDecode());
        });

        // 2. Video Button (NEW)
        JButton videoButton = createStyledButton(videoText, new Dimension(200, 80));
        videoButton.addActionListener(e -> {
            // Placeholder for Future Enhancement: Support for Video Steganography (MP4)
            launchPage(isEncodeFlow ? new VideoPageEncode() : new VideoPageDecode());
        });

        // 3. Image + Message Button
        JButton imageMsgButton = createStyledButton(imageText + " + Message", new Dimension(200, 80));
        imageMsgButton.addActionListener(e -> {
            launchPage(isEncodeFlow ? new ImageAesMessageEncode() : new ImageAesMessageDecode());
        });
        
        dynamicButtonPanel.add(audioButton);
        dynamicButtonPanel.add(videoButton); // Added after Audio
        dynamicButtonPanel.add(imageMsgButton);
        
        backButton.setVisible(true);
        
        dynamicButtonPanel.revalidate();
        dynamicButtonPanel.repaint();
        setTitle("Secret Message Steganography");
        currentView = "message";
    }

    // --- Utility Methods (Unchanged) ---

    private JButton createStyledButton(String text, Dimension buttonSize) {
        JButton button = new JButton(text);
        
        if (buttonSize.height < 50) {
            button.setFont(new Font("Arial", Font.PLAIN, 18));
        } else {
            button.setFont(new Font("Arial", Font.PLAIN, 18)); 
        }

        button.setPreferredSize(buttonSize);
        
        Color purpleColor = new Color(118, 57, 111);
        Border purpleBorder = BorderFactory.createLineBorder(purpleColor, 4);
        
        button.setBorder(purpleBorder);
        button.setForeground(purpleColor);
        button.setOpaque(true);
        
        return button;
    }
    
    private void launchPage(JFrame nextFrame) {
        nextFrame.setVisible(true);
        this.dispose(); 
    }
    
    private void goHome() {
        new Home().setVisible(true);
        this.dispose();
    }
}
