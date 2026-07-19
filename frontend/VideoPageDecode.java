package frontend;

import javax.swing.*;
import java.nio.ByteBuffer;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

// JCodec for Video Decoding
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

public class VideoPageDecode extends JFrame {
    private File selectedVideoFile;
    private JLabel statusLabel;

    public VideoPageDecode() {
        setTitle("Decode Video File");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 240, 240));

        // Header
        JLabel headerLabel = new JLabel("DECODE VIDEO FILE", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Center Content Section (The Box) ---
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setPreferredSize(new Dimension(800, 400));
        controlsPanel.setBackground(Color.WHITE);
        
        // Black Border UI per your request
        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1);
        Border padding = BorderFactory.createEmptyBorder(30, 50, 30, 50);
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(lineBorder, padding));

        // Browse Button
        JButton browseButton = new JButton("Browse Video File (.mp4)");
        browseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        browseButton.addActionListener(e -> openFileChooser());

        // Inside the border: Status and Buttons
        statusLabel = new JLabel("Ready to upload video", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        actionRow.setOpaque(false);

        JButton homeButton = new JButton("Home");
        homeButton.setPreferredSize(new Dimension(120, 40));
        homeButton.addActionListener(e -> {
            new Home().setVisible(true);
            dispose();
        });

        JButton decodeButton = new JButton("DECODE");
        decodeButton.setPreferredSize(new Dimension(120, 40));
        decodeButton.addActionListener(e -> requestKeyAndDecode());

        actionRow.add(homeButton);
        actionRow.add(decodeButton);

        controlsPanel.add(Box.createVerticalGlue());
        controlsPanel.add(browseButton);
        controlsPanel.add(Box.createVerticalStrut(40));
        controlsPanel.add(statusLabel);
        controlsPanel.add(Box.createVerticalStrut(40));
        controlsPanel.add(actionRow);
        controlsPanel.add(Box.createVerticalGlue());

        centerWrapper.add(controlsPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedVideoFile = fc.getSelectedFile();
            statusLabel.setText("File selected: " + selectedVideoFile.getName());
        }
    }

    private void requestKeyAndDecode() {
        if (selectedVideoFile == null) {
            updateStatus("Please select a video file first!", Color.RED);
            return;
        }

        String key = JOptionPane.showInputDialog(this, "Enter 12-character Secret Key:");
        if (key == null || key.length() != 12) {
            JOptionPane.showMessageDialog(this, "Invalid Key! Must be 12 characters.");
            return;
        }

        new Thread(() -> {
            try {
                updateStatus("Extracting data from video...", Color.BLUE);
                
                // 1. Call the new extraction function
                byte[] encryptedMessage = extractDataFromVideo(selectedVideoFile);
                
                if (encryptedMessage == null) {
                    throw new Exception("No data found in video.");
                }

                // 2. Derive Key and Decrypt
                SecretKey aesKey = deriveKey(key);
                String message = decryptMessage(encryptedMessage, aesKey);
                
                updateStatus("Decryption Successful!", new Color(0, 150, 0));
                
                // Show the hidden message in a popup
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Hidden Message:\n" + message, "Decoded Message", JOptionPane.INFORMATION_MESSAGE)
                );
                
            } catch (Exception ex) {
                ex.printStackTrace();
                updateStatus("Decryption Failed: Mismatched data or wrong key.", Color.RED);
            }
        }).start();
    }

    // --- NEW FUNCTION: The Extraction Logic ---
    private byte[] extractDataFromVideo(File videoFile) throws Exception {
    FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));
    Picture picture;
    int bitPos = 0, lengthInBytes = 0;
    boolean lengthFound = false;
    byte[] encryptedData = null;
    int blockSize = 4; 

    while ((picture = grab.getNativeFrame()) != null) {
        BufferedImage frame = AWTUtil.toBufferedImage(picture);
        for (int y = 0; y < frame.getHeight() - blockSize; y += blockSize) {
            for (int x = 0; x < frame.getWidth() - blockSize; x += blockSize) {
                
                int votesForOne = 0;
                int totalVotes = blockSize * blockSize * 3; // 48 votes per bit

                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int rgb = frame.getRGB(x + dx, y + dy);
                        // Check Bit 6 (Value 64) in R, G, and B
                        if (((rgb >> 16) & 64) != 0) votesForOne++; 
                        if (((rgb >> 8) & 64) != 0) votesForOne++;  
                        if ((rgb & 64) != 0) votesForOne++;         
                    }
                }

                int bit = (votesForOne > (totalVotes / 2)) ? 1 : 0;

                if (!lengthFound) {
                    lengthInBytes = (lengthInBytes << 1) | bit;
                    if (++bitPos == 32) {
                        if (lengthInBytes <= 0 || lengthInBytes > 10000) throw new Exception("Corruption too high.");
                        encryptedData = new byte[lengthInBytes];
                        lengthFound = true;
                        bitPos = 0;
                    }
                } else {
                    int byteIdx = bitPos / 8;
                    int bitShift = 7 - (bitPos % 8);
                    if (bit == 1) encryptedData[byteIdx] |= (1 << bitShift);
                    if (++bitPos == lengthInBytes * 8) return encryptedData;
                }
            }
        }
    }
    return encryptedData;
}
    // --- Helper Methods (Mirroring Encode Logic) ---
    private SecretKey deriveKey(String pass) throws Exception {
        byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(pass.getBytes()), 16);
        return new SecretKeySpec(key, "AES");
    }

    private String decryptMessage(byte[] encrypted, SecretKey key) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(encrypted)).trim();
    }

    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }
}
