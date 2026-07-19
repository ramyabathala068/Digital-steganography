package frontend;

import javax.swing.*;
import java.nio.ByteBuffer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

// JCodec imports for video processing
import org.jcodec.api.FrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

public class VideoPageEncode extends JFrame {

    private String currentShortPassphrase;
    private JLabel statusLabel;
    private File selectedVideoFile;
    private JTextArea messageArea;
    private JButton getKeyButton;

    public VideoPageEncode() {
        setTitle("Upload and Encode Video");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750); // Increased size
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 240, 240));

        // --- Header Section ---
        JLabel headerLabel = new JLabel("UPLOAD VIDEO FILE", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Center Content Section (The Larger White Box) ---
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setPreferredSize(new Dimension(850, 500)); // Increased box size
        controlsPanel.setBackground(Color.WHITE);
        
        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1);
        Border padding = BorderFactory.createEmptyBorder(30, 50, 30, 50);
        controlsPanel.setBorder(BorderFactory.createCompoundBorder(lineBorder, padding));

        // 1. Browse Button
        JButton browseButton = new JButton("Browse Video File (.mp4)");
        browseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        browseButton.addActionListener(e -> openFileChooser());

        // 2. Message Input
        JLabel msgLabel = new JLabel("Enter Secret Text Message:");
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageArea = new JTextArea(5, 20);
        messageArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setMaximumSize(new Dimension(500, 120));

        // 3. Encode Button
        JButton encodeButton = new JButton("ENCODE VIDEO");
        encodeButton.setFont(new Font("Arial", Font.BOLD, 14));
        encodeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        encodeButton.addActionListener(e -> startVideoEncoding());

        // 4. Status Label (Now inside the box)
        statusLabel = new JLabel("Ready to upload video", SwingConstants.CENTER);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        statusLabel.setForeground(Color.BLUE);

        // 5. Button Row (Now inside the box)
        JPanel internalButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        internalButtonRow.setOpaque(false);
        internalButtonRow.setMaximumSize(new Dimension(800, 50));

        getKeyButton = new JButton("Get Key");
        getKeyButton.setEnabled(false);
        getKeyButton.addActionListener(e -> showKeyDialog());

        JButton homeButton = new JButton("Home");
        homeButton.addActionListener(e -> {
            new Home().setVisible(true);
            dispose();
        });

        internalButtonRow.add(getKeyButton);
        internalButtonRow.add(homeButton);

        // Assemble Internal UI
        controlsPanel.add(Box.createVerticalGlue());
        controlsPanel.add(browseButton);
        controlsPanel.add(Box.createVerticalStrut(20));
        controlsPanel.add(msgLabel);
        controlsPanel.add(Box.createVerticalStrut(10));
        controlsPanel.add(scrollPane);
        controlsPanel.add(Box.createVerticalStrut(25));
        controlsPanel.add(encodeButton);
        controlsPanel.add(Box.createVerticalStrut(30));
        controlsPanel.add(statusLabel);
        controlsPanel.add(Box.createVerticalStrut(20));
        controlsPanel.add(internalButtonRow);
        controlsPanel.add(Box.createVerticalGlue());

        centerWrapper.add(controlsPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        add(mainPanel);
        setupDragAndDrop(controlsPanel);
    }

    private void openFileChooser() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedVideoFile = fc.getSelectedFile();
            statusLabel.setText("Selected: " + selectedVideoFile.getName());
        }
    }

    // --- Logic: Video Processing ---
    private void startVideoEncoding() {
    String message = messageArea.getText().trim();
    
    if (selectedVideoFile == null || message.isEmpty()) {
        updateStatus("Error: Selection or message missing.", Color.RED);
        return;
    }

    new Thread(() -> {
        try {
            updateStatus("Generating secure key...", Color.BLUE);
            
            // 1. Generate Key & Encrypt
            this.currentShortPassphrase = generatePassphrase();
            SecretKey aesKey = deriveKey(this.currentShortPassphrase);
            byte[] encryptedData = encryptMessage(message, aesKey);

            // 2. Prepare Data with 4-byte Length Header
            // This ensures the decoder knows exactly when to stop
            ByteBuffer buffer = ByteBuffer.allocate(4 + encryptedData.length);
            buffer.putInt(encryptedData.length); 
            buffer.put(encryptedData);           
            byte[] dataToHide = buffer.array();

            updateStatus("Encoding frames... Please wait.", Color.BLUE);

            File outputDir = new File("frontend/saved");
            if (!outputDir.exists()) outputDir.mkdirs();
            File outputFile = new File(outputDir, "encoded_" + selectedVideoFile.getName());
            
            // 3. Initialize JCodec
            AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(outputFile, 25);
            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(selectedVideoFile));

            int bitPos = 0;
            Picture picture;
            int totalBits = dataToHide.length * 8; 

            // 4. Main Encoding Loop
            while ((picture = grab.getNativeFrame()) != null) {
                BufferedImage frame = AWTUtil.toBufferedImage(picture);
                
                if (bitPos < totalBits) {
                    // Embeds bits into the Blue channel LSB
                    bitPos = embedDataInFrame(frame, dataToHide, bitPos);
                }
                
                encoder.encodeImage(frame);
            }
            
            encoder.finish();

            updateStatus("Encoding Successful!", new Color(0, 120, 0));
            SwingUtilities.invokeLater(() -> getKeyButton.setEnabled(true));

        } catch (Exception ex) {
            ex.printStackTrace();
            updateStatus("Encoding Failed: " + ex.getMessage(), Color.RED);
        }
    }).start();
}
    private int embedDataInFrame(BufferedImage frame, byte[] dataToHide, int bitPos) {
    int width = frame.getWidth();
    int height = frame.getHeight();
    int totalBits = dataToHide.length * 8;
    int blockSize = 4; // Each bit now occupies a 16-pixel square

    for (int y = 0; y < height - blockSize; y += blockSize) {
        for (int x = 0; x < width - blockSize; x += blockSize) {
            if (bitPos >= totalBits) return bitPos;

            int byteIdx = bitPos / 8;
            int bitShift = 7 - (bitPos % 8);
            int bit = (dataToHide[byteIdx] >> bitShift) & 1;

            for (int dy = 0; dy < blockSize; dy++) {
                for (int dx = 0; dx < blockSize; dx++) {
                    int rgb = frame.getRGB(x + dx, y + dy);
                    
                    // Use Bit 6 (Value 64) - This is extremely robust
                    // We apply it to all 3 channels to create a "gray" shift
                    int mask = ~64;
                    int r = ((rgb >> 16) & 0xFF & mask) | (bit == 1 ? 64 : 0);
                    int g = ((rgb >> 8) & 0xFF & mask) | (bit == 1 ? 64 : 0);
                    int b = (rgb & 0xFF & mask) | (bit == 1 ? 64 : 0);

                    int newRgb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    frame.setRGB(x + dx, y + dy, newRgb);
                }
            }
            bitPos++;
        }
    }
    return bitPos;
}
    // --- Helper Methods (Matching Audio Logic) ---
    private String generatePassphrase() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom sr = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for(int i=0; i<12; i++) sb.append(chars.charAt(sr.nextInt(chars.length())));
        return sb.toString();
    }

    private SecretKey deriveKey(String pass) throws Exception {
        byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(pass.getBytes()), 16);
        return new SecretKeySpec(key, "AES");
    }

    private byte[] encryptMessage(String msg, SecretKey key) throws Exception {
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(msg.getBytes());
    }
    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    private void showKeyDialog() {
    if (currentShortPassphrase == null || currentShortPassphrase.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No key generated yet!", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 1. Copy to Clipboard logic
    StringSelection stringSelection = new StringSelection(currentShortPassphrase);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);

    // 2. Show Dialog Box
    String message = "Your Secret Key: " + currentShortPassphrase + "\n\n(The key has been copied to your clipboard)";
    JOptionPane.showMessageDialog(this, message, "Encryption Key", JOptionPane.INFORMATION_MESSAGE);
}

    private void setupDragAndDrop(JPanel target) {
        new DropTarget(target, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    selectedVideoFile = files.get(0);
                    statusLabel.setText("Dropped: " + selectedVideoFile.getName());
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VideoPageEncode().setVisible(true));
    }
}

