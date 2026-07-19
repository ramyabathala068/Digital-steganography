package frontend;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.awt.Toolkit;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder; 

public class AudioPageEncode extends JFrame {

    private String currentShortPassphrase;
    private JLabel statusLabel;
    private File selectedAudioFile;

    public AudioPageEncode() {
        setTitle("Upload and Encode Audio");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int width = 1200;
        int height = 650;
        setSize(width, height);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel headerLabel = new JLabel("UPLOAD AUDIO FILE");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        headerPanel.add(headerLabel);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Center Panel (Drag & Drop + Controls) - This acts as a container
        JPanel centerPanel = createDragAndDropPanel();
        
        // --- Center Components ---
        JButton browseButton = new JButton("Browse Audio File (.wav)");
        browseButton.setPreferredSize(new Dimension(200, 30));
        browseButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Keep centering
        browseButton.addActionListener(e -> openFileChooser());
        
        JLabel messageLabel = new JLabel("Enter Secret Text Message:", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Keep centering
        
        JTextArea messageArea = new JTextArea("Your Secret Message");
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        // Wrap JTextArea in a JScrollPane and a fixed-size wrapper to prevent stretching
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        JPanel messagePanelWrapper = new JPanel(new BorderLayout());
        messagePanelWrapper.setPreferredSize(new Dimension(300, 100));
        messagePanelWrapper.setMaximumSize(new Dimension(300, 100));
        messagePanelWrapper.add(messageScrollPane, BorderLayout.CENTER);
        messagePanelWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton encryptButton = new JButton("ENCODE AUDIO");
        encryptButton.setPreferredSize(new Dimension(200, 40));
        encryptButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Keep centering
        encryptButton.addActionListener(e -> encryptAndSaveAudio(messageArea.getText()));
        
        // Vertical arrangement for controls (The container to receive the border)
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        
        // --- START BORDER IMPLEMENTATION (Modified for large size) ---
        // 1. The visible line border
        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1);
        
        // 2. Large internal padding to push the components to the center of the large box.
        // This padding now defines the size of the final bordered area.
        // We are using large padding to simulate the area previously occupied by the centerPanel's fixed border.
        Border internalSpacing = BorderFactory.createEmptyBorder(70, 40, 70, 40); 
        
        // 3. Combine them
        Border finalBorder = BorderFactory.createCompoundBorder(lineBorder, internalSpacing);
        controlsPanel.setBorder(finalBorder); 
        // --- END BORDER IMPLEMENTATION ---

        controlsPanel.add(Box.createVerticalGlue());
        controlsPanel.add(browseButton);
        controlsPanel.add(Box.createVerticalStrut(25));
        controlsPanel.add(messageLabel);
        controlsPanel.add(Box.createVerticalStrut(10));
        controlsPanel.add(messagePanelWrapper); // Use the wrapper
        controlsPanel.add(Box.createVerticalStrut(25));
        controlsPanel.add(encryptButton);
        controlsPanel.add(Box.createVerticalGlue());
        
        // --- CRUCIAL CHANGE: Make controlsPanel fill centerPanel (REMOVES GridBagLayout centering) ---
        centerPanel.setLayout(new BorderLayout()); // Ensure centerPanel uses BorderLayout
        centerPanel.add(controlsPanel, BorderLayout.CENTER);
        // ---
        
        panel.add(centerPanel, BorderLayout.CENTER);

        // Footer / Status Panel
        JPanel secondBoxPanel = createOutlinedBoxPanel("Output Status");
        
        JLabel encodedTextLabel = new JLabel("Audio encoding status", SwingConstants.CENTER);
        encodedTextLabel.setFont(new Font("Arial", Font.BOLD, 30));
        encodedTextLabel.setForeground(Color.BLACK);
        
        statusLabel = new JLabel("Ready to upload audio", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        statusLabel.setForeground(Color.BLUE);
        
        JButton getKeyButton = new JButton("Get Key");
        getKeyButton.setPreferredSize(new Dimension(120, 30));
        getKeyButton.addActionListener(e -> openKeyDialog());
        
        JButton homeButton = new JButton("Home");
        homeButton.setPreferredSize(new Dimension(120, 30));
        homeButton.addActionListener(e -> goHome());

        JPanel centerContainer = new JPanel(new GridLayout(2, 1));
        centerContainer.setOpaque(false); 
        centerContainer.add(encodedTextLabel);
        centerContainer.add(statusLabel); 

        secondBoxPanel.setLayout(new BorderLayout());
        secondBoxPanel.add(centerContainer, BorderLayout.CENTER);
        secondBoxPanel.add(getKeyButton, BorderLayout.SOUTH);
        secondBoxPanel.add(homeButton, BorderLayout.WEST);

        panel.add(secondBoxPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 90, 20, 90));

        setContentPane(panel);
        setLocationRelativeTo(null);
    }
    
    // --- CORE LOGIC IMPLEMENTATION (Unchanged) ---

    private void encryptAndSaveAudio(String message) {
        if (selectedAudioFile == null) {
            updateStatus("Please select a WAV audio file first.", Color.RED);
            return;
        }
        if (message.trim().isEmpty()) {
            updateStatus("Please enter a secret message.", Color.RED);
            return;
        }

        try {
            updateStatus("Starting Audio Encoding...", Color.BLUE);
            
            this.currentShortPassphrase = generateUniquePassphrase();
            SecretKey derivedAesKey = deriveAesKey(this.currentShortPassphrase);
            
            byte[] messageBytes = message.getBytes("UTF-8");
            byte[] encryptedMessage = encryptMessage(messageBytes, derivedAesKey);

            updateStatus("Embedding encrypted data via LSB...", Color.ORANGE);
            
            byte[] modifiedAudioBytes = embedMessageInWav(selectedAudioFile, encryptedMessage);
            
            Path filePath = saveEncodedAudio(modifiedAudioBytes, selectedAudioFile.getName());
            
            updateStatus("Audio encoding complete. Get Key to proceed.", Color.GREEN.darker());
            JOptionPane.showMessageDialog(this, "Audio encrypted and saved:\n" + filePath, "Encryption Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            updateStatus("Audio encoding FAILED.", Color.RED);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Encoding failed: " + e.getMessage(), "Fatal Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] embedMessageInWav(File audioFile, byte[] messageToHide) throws Exception {
        
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            
            AudioFormat format = audioInputStream.getFormat();
            if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && 
                !format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                throw new UnsupportedAudioFileException("Only PCM WAV formats are supported for LSB.");
            }
            
            // Prepare message with length prefix (4 bytes)
            int messageLength = messageToHide.length;
            
            byte[] lengthPrefix = new byte[4];
            lengthPrefix[0] = (byte) (messageLength >> 24);
            lengthPrefix[1] = (byte) (messageLength >> 16);
            lengthPrefix[2] = (byte) (messageLength >> 8);
            lengthPrefix[3] = (byte) messageLength;

            byte[] allDataToEmbed = new byte[lengthPrefix.length + messageToHide.length];
            System.arraycopy(lengthPrefix, 0, allDataToEmbed, 0, lengthPrefix.length);
            System.arraycopy(messageToHide, 0, allDataToEmbed, lengthPrefix.length, messageToHide.length);
            
            long requiredFrames = (long)allDataToEmbed.length * 8; 
            
            byte[] audioBytes = audioInputStream.readAllBytes();
            
            if (requiredFrames > audioBytes.length) {
                throw new IllegalArgumentException("Audio file is too small. Required bytes: " + requiredFrames + ", Available bytes: " + audioBytes.length);
            }

            // LSB Embedding loop
            int audioByteOffset = 0; 
            for (byte messageByte : allDataToEmbed) {
                for (int bit = 7; bit >= 0; bit--) {
                    int messageBit = (messageByte >> bit) & 1;

                    audioBytes[audioByteOffset] = (byte) (audioBytes[audioByteOffset] & 0xFE);
                    audioBytes[audioByteOffset] = (byte) (audioBytes[audioByteOffset] | messageBit);
                    
                    audioByteOffset++;
                }
            }
            
            return audioBytes;
            
        } catch (UnsupportedAudioFileException e) {
             throw new Exception("Unsupported audio file format. Please use uncompressed WAV.", e);
        }
    }
    
    private byte[] encryptMessage(byte[] data, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        return cipher.doFinal(data);
    }
    
    private Path saveEncodedAudio(byte[] modifiedAudioBytes, String originalFileName) throws IOException, UnsupportedAudioFileException {
        // --- CRITICAL PATH CHANGE ---
        Path directoryPath = Paths.get("saved");
        Files.createDirectories(directoryPath);
        
        File originalFile = selectedAudioFile;
        AudioInputStream originalAIS = AudioSystem.getAudioInputStream(originalFile);
        AudioFormat format = originalAIS.getFormat();
        originalAIS.close();

        String outputFileName = "encoded_" + originalFileName.replace(".wav", ".wav");
        Path filePath = directoryPath.resolve(outputFileName);
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(modifiedAudioBytes);
             AudioInputStream newAIS = new AudioInputStream(bais, format, modifiedAudioBytes.length / format.getFrameSize())) {
            
            AudioSystem.write(newAIS, AudioFileFormat.Type.WAVE, filePath.toFile());
            return filePath;
        }
    }
    
    // --- Utility Methods (Unchanged) ---
    
    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setForeground(color);
            statusLabel.setText(message);
        });
    }

    private String generateUniquePassphrase() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private SecretKey deriveAesKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = passphrase.getBytes("UTF-8");
        keyBytes = sha.digest(keyBytes);
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
    }

    private String generateKey() {
        return this.currentShortPassphrase; 
    }
    
    private void openKeyDialog() {
        if (currentShortPassphrase == null) {
            JOptionPane.showMessageDialog(this, "Please encode an audio file first to generate a key.", "Key Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String key = generateKey(); 
        StringSelection stringSelection = new StringSelection(key);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        
        JOptionPane.showMessageDialog(this, 
            "Unique Session Key copied to clipboard:\n" + key, 
            "Key Information", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void goHome() {
        //dispose(); 
        
        new Home().setVisible(true); 
        //System.exit(0); 
    }

    private JPanel createOutlinedBoxPanel(String title) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2d.dispose();
            }
        };
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
        return panel;
    }

    // --- MODIFIED METHOD ---
    private JPanel createDragAndDropPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        // Remove fixed padding here so that the controlsPanel can expand into this area
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Minimal internal padding 
        return panel;
    }
    // --- END MODIFIED METHOD ---

    private void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
            }
            public String getDescription() {
                return "WAV Audio Files (*.wav)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedAudioFile = fileChooser.getSelectedFile();
            updateStatus("File selected: " + selectedAudioFile.getName(), Color.BLACK);
        }
    }
}
