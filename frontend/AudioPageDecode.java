package frontend;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioPageDecode extends JFrame {
    
    private String currentShortPassphrase;
    private File selectedAudioFile;
    private JLabel statusLabel;

    public AudioPageDecode() {
        setTitle("Decode Audio File");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int width = 1200;
        int height = 650;
        
        setSize(width, height);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel headerLabel = new JLabel("DECODE AUDIO FILE");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 40)); 
        headerPanel.add(headerLabel);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Center Panel (Drag & Drop + Browse)
        JPanel firstBoxPanel = createDragAndDropPanel();
        JButton browseButton = new JButton("Browse Audio File (.wav)");
        browseButton.setPreferredSize(new Dimension(200, 30)); 
        browseButton.addActionListener(e -> openFileChooser());
        firstBoxPanel.add(browseButton);

        panel.add(firstBoxPanel, BorderLayout.CENTER);

        // Footer / Status Panel
        JPanel secondBoxPanel = createOutlinedBoxPanel("Output Status");
        
        statusLabel = new JLabel("Ready to upload audio", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 30));
        statusLabel.setForeground(Color.BLACK);
        
        // Buttons
        JButton decodeButton = new JButton("DECODE");
        JButton homeButton = new JButton("Home");
        
        decodeButton.setPreferredSize(new Dimension(120, 30)); 
        decodeButton.addActionListener(e -> openKeyDialog());
        
        homeButton.setPreferredSize(new Dimension(120, 30));
        homeButton.addActionListener(e -> goHome());

        secondBoxPanel.setLayout(new BorderLayout());
        secondBoxPanel.add(statusLabel, BorderLayout.CENTER);
        secondBoxPanel.add(decodeButton, BorderLayout.EAST);
        secondBoxPanel.add(homeButton, BorderLayout.WEST);

        panel.add(secondBoxPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 90, 20, 90));

        setContentPane(panel);
        setLocationRelativeTo(null);
    }
    
    // --- CORE LOGIC IMPLEMENTATION ---

    private SecretKey deriveAesKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = passphrase.getBytes("UTF-8");
        keyBytes = sha.digest(keyBytes);
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
    }

    private byte[] extractMessageFromWav(File modifiedAudioFile) throws Exception {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(modifiedAudioFile)) {
            
            byte[] audioBytes = audioInputStream.readAllBytes();

            // 1. EXTRACT LENGTH (4 bytes)
            int length = 0;
            int offset = 0;
            
            for (int i = 0; i < 4; i++) { 
                byte currentByte = 0;
                for (int bit = 7; bit >= 0; bit--) {
                    int lsb = audioBytes[offset] & 1;
                    currentByte = (byte) (currentByte | (lsb << bit));
                    offset++;
                }
                length = (length << 8) | (currentByte & 0xFF);
            }

            if (length <= 0 || length > audioBytes.length / 8) {
                throw new IOException("Could not extract valid message length (" + length + ") from audio file.");
            }

            // 2. EXTRACT ENCRYPTED MESSAGE
            byte[] extractedMessage = new byte[length];
            
            for (int i = 0; i < length; i++) { 
                byte currentByte = 0;
                for (int bit = 7; bit >= 0; bit--) {
                    int lsb = audioBytes[offset] & 1;
                    currentByte = (byte) (currentByte | (lsb << bit));
                    offset++;
                }
                extractedMessage[i] = currentByte;
            }

            return extractedMessage;
        }
    }
    
    private void decryptAudio(String keyToUse) {
        if (selectedAudioFile == null) {
            updateStatus("Please select an encoded audio file first.", Color.RED);
            return;
        }
        
        try {
            updateStatus("Extracting hidden data via LSB...", Color.BLUE);
            
            byte[] encryptedMessage = extractMessageFromWav(selectedAudioFile);
            SecretKey aesKey = deriveAesKey(keyToUse);
            
            updateStatus("Decrypting message...", Color.ORANGE);
            
            // Decrypt the message
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); 
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessage);
            
            String decryptedMessage = new String(decryptedMessageBytes, "UTF-8");
            
            // Save the decoded message to a file
            Path savePath = saveDecryptedText(decryptedMessage, selectedAudioFile.getName());
            
            // Display Result
            displayDecryptedText(decryptedMessage);
            updateStatus("Decryption complete. Message saved to " + savePath.getFileName(), Color.GREEN.darker());

        } catch (javax.crypto.BadPaddingException e) {
            updateStatus("DECRYPTION FAILED: INCORRECT KEY.", Color.RED);
            JOptionPane.showMessageDialog(this, "The key is incorrect. Please check the key and try again.", "Key Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            updateStatus("DECRYPTION FAILED.", Color.RED);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Decryption failed: " + e.getMessage(), "Fatal Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private Path saveDecryptedText(String decryptedText, String originalFileName) throws IOException {
        // --- CRITICAL PATH CHANGE ---
        Path directoryPath = Paths.get("saved");
        Files.createDirectories(directoryPath);
        
        String outputFileName = "decoded_" + originalFileName.replace(".wav", ".txt");
        Path filePath = directoryPath.resolve(outputFileName);
        
        Files.write(filePath, decryptedText.getBytes());
        return filePath;
    }

    // --- UI and Utility Methods ---

    private void updateStatus(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setForeground(color);
            statusLabel.setText(message);
        });
    }
    
    private void displayDecryptedText(String decryptedText) {
        JTextArea textArea = new JTextArea(decryptedText);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, 
            scrollPane, 
            "Decrypted Audio Message Content", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openKeyDialog() {
        JDialog keyDialog = new JDialog(this, "Key Information", true);
        keyDialog.setSize(450, 200);
        keyDialog.setResizable(false);
        keyDialog.setLocationRelativeTo(this);

        JPanel keyPanel = new JPanel(new BorderLayout());
        JTextArea keyTextArea = new JTextArea(5, 30);
        keyTextArea.setEditable(true); 
        JScrollPane scrollPane = new JScrollPane(keyTextArea);

        JButton confirmButton = new JButton("Confirm Key");
        confirmButton.setPreferredSize(new Dimension(150, 30));
        confirmButton.addActionListener(e -> {
            String enteredKey = keyTextArea.getText();
            
            if (isValidKey(enteredKey)) {
                keyDialog.dispose();
                decryptAudio(this.currentShortPassphrase); 
            } else {
                JOptionPane.showMessageDialog(this, "Please paste the session key.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        keyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        keyPanel.add(new JLabel("Paste the unique session key here:", SwingConstants.CENTER), BorderLayout.NORTH);
        keyPanel.add(scrollPane, BorderLayout.CENTER);
        keyPanel.add(confirmButton, BorderLayout.SOUTH);

        keyDialog.setContentPane(keyPanel);
        keyDialog.setVisible(true);
    }

    private boolean isValidKey(String enteredKey) {
        this.currentShortPassphrase = enteredKey.trim();
        return !this.currentShortPassphrase.isEmpty(); 
    }
    
    private void goHome() {
        dispose(); 
        new Home().setVisible(true);
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

    private JPanel createDragAndDropPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(90, 90, 90, 90)); 
        
        DropTarget dropTarget = new DropTarget(panel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = dtde.getTransferable();
                try {
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() > 0) {
                        File file = files.get(0);
                        if (file.getName().toLowerCase().endsWith(".wav")) {
                             selectedAudioFile = file;
                             updateStatus("File selected: " + file.getName(), Color.BLACK);
                        } else {
                            updateStatus("Please drop a WAV file.", Color.RED);
                        }
                    }
                } catch (Exception e) {
                    updateStatus("Drop Failed.", Color.RED);
                }
            }
        });
        panel.setDropTarget(dropTarget);

        return panel;
    }

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
