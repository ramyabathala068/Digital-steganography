package frontend;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.List;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.Toolkit;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.Base64;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class ImageAesMessageEncode extends JFrame {
    private String currentShortPassphrase; 
    private JLabel statusLabel;
    private File selectedFile;
    private JTextArea messageArea; 

    public ImageAesMessageEncode() {
        setTitle("Upload and Encode (with Message)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        int width = 1200;
        int height = 650;
        setSize(width, height);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout());

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel headerLabel = new JLabel("UPLOAD FILES (WITH MESSAGE)");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        headerPanel.add(headerLabel);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // --- CENTER PANEL STRUCTURE (Simplified Vertical Layout) ---
        JPanel centerPanelContainer = createDragAndDropPanel(); // This returns a JPanel with BorderLayout by default
        
        // --- Center Controls Panel: Uses BoxLayout (Y_AXIS) for simple vertical stacking ---
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // --- Components ---
        JButton browseButton = new JButton("Browse Files");
        browseButton.setPreferredSize(new Dimension(180, 30)); 
        browseButton.addActionListener(e -> openFileChooser());
        browseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel messageLabel = new JLabel("Enter Secret Text Message:", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        messageArea = new JTextArea("Your Secret Message");
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        // JScrollPane with a fixed size wrapper panel (GUARANTEES SIZE)
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        JPanel messagePanelWrapper = new JPanel(new BorderLayout());
        messagePanelWrapper.setPreferredSize(new Dimension(300, 100)); // Fixed size
        messagePanelWrapper.setMaximumSize(new Dimension(300, 100)); // Prevents stretching
        messagePanelWrapper.add(messageScrollPane, BorderLayout.CENTER);
        messagePanelWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton encryptButton = new JButton("ENCODE FILES");
        encryptButton.setPreferredSize(new Dimension(200, 40));
        encryptButton.addActionListener(e -> startEncryption(messageArea.getText()));
        encryptButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 5. Assemble all components in the controlsPanel
        controlsPanel.add(Box.createVerticalGlue()); // Push content to the center
        controlsPanel.add(browseButton);
        controlsPanel.add(Box.createVerticalStrut(25));
        controlsPanel.add(messageLabel);
        controlsPanel.add(Box.createVerticalStrut(10));
        controlsPanel.add(messagePanelWrapper); // Use the guaranteed-size wrapper
        controlsPanel.add(Box.createVerticalStrut(25));
        controlsPanel.add(encryptButton);
        controlsPanel.add(Box.createVerticalGlue());
        
        // Center the controlsPanel inside the main drag-and-drop area
        centerPanelContainer.setLayout(new GridBagLayout()); 
        centerPanelContainer.add(controlsPanel, new GridBagConstraints());
        
        panel.add(centerPanelContainer, BorderLayout.CENTER);
        // --- END CENTER PANEL STRUCTURE ---


        // Content has been ENCODED button
        JPanel secondBoxPanel = createOutlinedBoxPanel(" ");
        /*JButton downloadButton = new JButton("Get Key");
        downloadButton.setPreferredSize(new Dimension(120, 30));
        downloadButton.addActionListener(e -> {
            openKeyDialog();
        });*/
        
        JLabel encodedTextLabel = new JLabel("Encryption Status", SwingConstants.CENTER);
        encodedTextLabel.setFont(new Font("Arial", Font.BOLD, 30));
        encodedTextLabel.setForeground(Color.BLACK);
        
        statusLabel = new JLabel("Ready to upload files", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        statusLabel.setForeground(Color.BLUE);

        JPanel centerContainer = new JPanel(new GridLayout(2, 1));
        centerContainer.setOpaque(false); 
        centerContainer.add(encodedTextLabel);
        centerContainer.add(statusLabel);

        secondBoxPanel.setLayout(new BorderLayout());
        secondBoxPanel.add(centerContainer, BorderLayout.CENTER);
        //secondBoxPanel.add(downloadButton, BorderLayout.SOUTH);

        JButton homeButton = new JButton("Home");
        homeButton.setPreferredSize(new Dimension(120, 30));
        homeButton.addActionListener(e -> {
            goHome();
        });
        secondBoxPanel.add(homeButton, BorderLayout.WEST);

        panel.add(secondBoxPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 90, 20, 90));

        setContentPane(panel);
        setLocationRelativeTo(null);
    }
    
    private void startEncryption(String message) {
    if (selectedFile == null) {
        updateStatus("Please select a carrier image first.", Color.RED);
        return;
    }

    // Step 1: Browse for the Public Key file
    JFileChooser keyChooser = new JFileChooser("saved");
    keyChooser.setDialogTitle("Select Recipient's Public Key (.txt)");
    int result = keyChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
        try {
            // Step 2: Read the key string from the file
            File keyFile = keyChooser.getSelectedFile();
            String pubKeyStr = new String(Files.readAllBytes(keyFile.toPath())).trim();
            
            // Step 3: Convert string to PublicKey object
            byte[] publicBytes = Base64.getDecoder().decode(pubKeyStr);
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(publicBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey recipientPublicKey = keyFactory.generatePublic(keySpec);

            // Step 4: Call the main encryption logic
            encryptAndSaveFile(selectedFile, message.trim(), recipientPublicKey);
        } catch (Exception e) {
            updateStatus("Invalid Public Key file format.", Color.RED);
            e.printStackTrace();
        }
    }
}

    /*private void encryptAndSaveFile(File file, String secretMessage, java.security.PublicKey recipientPublicKey) {
    updateStatus("Starting Hybrid Encryption...", Color.BLUE);
    try {
        // 1. Generate random AES Key
        this.currentShortPassphrase = generateUniquePassphrase();
        SecretKey derivedAesKey = deriveAesKey(this.currentShortPassphrase);

        // 2. Encrypt the Message with AES
        byte[] rawMsgBytes = secretMessage.getBytes("UTF-8");
        int paddedLength = ((rawMsgBytes.length + 15) / 16) * 16;
        byte[] paddedMessage = new byte[paddedLength];
        System.arraycopy(rawMsgBytes, 0, paddedMessage, 0, rawMsgBytes.length);

        javax.crypto.Cipher aesCipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding"); 
        aesCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, derivedAesKey); 
        byte[] encryptedMsg = aesCipher.doFinal(paddedMessage);

        // 3. Encrypt the AES key itself with RSA
        byte[] encryptedKeyPackage = encryptAesKeyWithRsa(derivedAesKey, recipientPublicKey);

        // 4. Prepare Total Payload (Key Size + Key + Width + Height + Message)
        java.nio.ByteBuffer payloadBuffer = java.nio.ByteBuffer.allocate(
            Integer.BYTES * 3 + encryptedKeyPackage.length + encryptedMsg.length);
        
        payloadBuffer.putInt(encryptedKeyPackage.length); 
        payloadBuffer.put(encryptedKeyPackage);
        payloadBuffer.putInt(800); // Placeholder for Width
        payloadBuffer.putInt(600); // Placeholder for Height
        payloadBuffer.put(encryptedMsg);

        // 5. Embed the hybrid package
        BufferedImage originalImage = ImageIO.read(file);
        BufferedImage compatibleImage = new BufferedImage(
            originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        byte[] imageBytes = ((java.awt.image.DataBufferByte) compatibleImage.getRaster().getDataBuffer()).getData();
        embedMessage(imageBytes, payloadBuffer.array()); 

        Path filePath = saveEncryptedImage(compatibleImage);
        updateStatus("Hybrid Image saved!", Color.GREEN.darker());
    } catch (Exception e) {
        updateStatus("Encryption FAILED.", Color.RED);
        e.printStackTrace();
    }
}*/
    private void encryptAndSaveFile(File file, String secretMessage, java.security.PublicKey recipientPublicKey) {
    updateStatus("Starting Double Security Encryption...", Color.BLUE);
    try {
        // 1. Generate the Session Key (USED FOR BOTH IMAGE AND QR)
        this.currentShortPassphrase = generateUniquePassphrase();
        SecretKey sessionKey = deriveAesKey(this.currentShortPassphrase);

        // 2. Encrypt the Message & Embed in Image using LSB
        BufferedImage stegoImage = performAesWithLsbForDoubleSecurity(file, sessionKey, secretMessage);
        
        if (stegoImage == null) {
            throw new Exception("Image processing failed.");
        }

        // 3. Save the Stego-Image
        Path imagePath = saveEncryptedImage(stegoImage);

        // 4. Encrypt the SAME session key with the RSA Public Key
        byte[] encryptedKeyPackage = encryptAesKeyWithRsa(sessionKey, recipientPublicKey);

        // 5. Generate the QR Code containing the RSA-encrypted session key
        generateQRCode(encryptedKeyPackage, "saved/session_key_qr.png");

        updateStatus("Image & QR Code Generated!", Color.GREEN.darker());
        
        JOptionPane.showMessageDialog(this, 
            "Encryption Complete!\n\n1. Stego-Image: " + imagePath.toAbsolutePath() + 
            "\n2. Session Key QR: saved/session_key_qr.png", 
            "Success", JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
        updateStatus("Encryption FAILED.", Color.RED);
        e.printStackTrace();
    }
}
    
    
    private BufferedImage performAesWithLsbForDoubleSecurity(File imageFile, SecretKey aesKey, String secretMessage) {
    try {
        BufferedImage originalImage = ImageIO.read(imageFile);
        BufferedImage compatibleImage = new BufferedImage(
            originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        byte[] imageBytes = ((DataBufferByte) compatibleImage.getRaster().getDataBuffer()).getData();
        byte[] rawMsgBytes = secretMessage.getBytes("UTF-8");

        // 1. MANUAL PADDING: Ensure length is a multiple of 16 for NoPadding
        int paddedLength = ((rawMsgBytes.length + 15) / 16) * 16;
        byte[] paddedMessage = new byte[paddedLength];
        System.arraycopy(rawMsgBytes, 0, paddedMessage, 0, rawMsgBytes.length);

        // 2. ENCRYPT THE MESSAGE
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); 
        cipher.init(Cipher.ENCRYPT_MODE, aesKey); 
        byte[] encryptedMsg = cipher.doFinal(paddedMessage);

        // 3. PREPARE TOTAL PAYLOAD (Width + Height + EncryptedMessage)
        ByteBuffer payloadBuffer = ByteBuffer.allocate(Integer.BYTES * 2 + encryptedMsg.length);
        payloadBuffer.putInt(originalImage.getWidth());
        payloadBuffer.putInt(originalImage.getHeight());
        payloadBuffer.put(encryptedMsg);
        
        // 4. EMBED REDUNDANTLY
        embedMessage(imageBytes, payloadBuffer.array()); 
        
        return compatibleImage; 
    } catch (Exception e) {
        e.printStackTrace();
        return null; 
    }
}
    private void embedMessage(byte[] container, byte[] payload) {
    int numSectors = 9; 
    byte[] anchor = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
    
    // Length of the TOTAL payload buffer (metadata + message)
    byte[] lengthBytes = ByteBuffer.allocate(Integer.BYTES).putInt(payload.length).array();

    for (int s = 0; s < numSectors; s++) {
        int offset = (container.length / numSectors) * s;
        offset = embedBytes(container, anchor, offset);
        offset = embedBytes(container, lengthBytes, offset);
        embedBytes(container, payload, offset);
    }
}

private int embedBytes(byte[] container, byte[] data, int offset) {
    for (byte b : data) {
        for (int i = 7; i >= 0; i--) {
            container[offset] = (byte) ((container[offset] & 0xFE) | ((b >> i) & 1));
            offset++;
        }
    }
    return offset;
}
    
    // --- Utility Methods (Copied from UploadPageEncode.java) ---

    // Locate and replace this method:
private void goHome() {
    dispose(); 
    // new Home().setVisible(true); // Replaced System.exit(0)
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
        panel.setBorder(BorderFactory.createEmptyBorder(90, 90, 90, 90)); 

        DropTarget dropTarget = new DropTarget(panel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = dtde.getTransferable();
                try {
                    List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    handleDroppedFiles(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        panel.setDropTarget(dropTarget);

        return panel;
    }

    private void handleDroppedFiles(List<File> files) {
        if (files.isEmpty()) return;
        selectedFile = files.get(0);
        updateStatus("File selected: " + selectedFile.getName(), Color.BLACK);
    }

    private void openKeyDialog() {
        if (currentShortPassphrase == null) {
            JOptionPane.showMessageDialog(this, "Please encode a file first to generate a key.", "Key Missing", JOptionPane.ERROR_MESSAGE);
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

    private void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            updateStatus("File selected: " + selectedFile.getName(), Color.BLACK);
            
            if (isTextFile(selectedFile)) {
                 messageArea.setText(selectedFile.getName() + " content will be encrypted.");
            } else {
                 messageArea.setText("Your Secret Message");
            }
        }
    }
    
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
    
    private boolean isTextFile(File file) {
        return file.getName().toLowerCase().endsWith(".txt");
    }

    private byte[] performRsaWithXor(File textFile, SecretKey aesKey) {
    try {
        byte[] fileContent = Files.readAllBytes(Paths.get(textFile.getAbsolutePath()));
        
        // 1. MANUAL PADDING: Ensure text content is a multiple of 16 for NoPadding
        int paddedLength = ((fileContent.length + 15) / 16) * 16;
        byte[] paddedContent = new byte[paddedLength];
        System.arraycopy(fileContent, 0, paddedContent, 0, fileContent.length);

        // 2. AES ENCRYPTION
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); 
        cipher.init(Cipher.ENCRYPT_MODE, aesKey); 
        byte[] encryptedContent = cipher.doFinal(paddedContent);
        
        // 3. XOR OPERATION
        byte[] xorEncryptedContent = xorOperation(encryptedContent); 
        
        Path filePath = Paths.get("saved/xor_encrypted_data.txt");
        saveXorEncryptedData(xorEncryptedContent, filePath);
        return xorEncryptedContent;
    } catch (Exception e) {
        e.printStackTrace();
        return new byte[0];
    }
}

    private byte[] xorOperation(byte[] data) {
        byte xorKey = (byte) 0xAA;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ xorKey);
        }
        return data;
    }
    
    private Path saveXorEncryptedData(byte[] xorEncryptedData, Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, xorEncryptedData, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private Path saveEncryptedImage(BufferedImage stegoImage) {
    Path directoryPath = Paths.get("saved");
    File outputFile = directoryPath.resolve("image.png").toFile();
    try {
        Files.createDirectories(directoryPath); 
        // ImageIO.write adds the PNG headers necessary for visibility
        ImageIO.write(stegoImage, "png", outputFile); 
    } catch (Exception e) {
        e.printStackTrace();
    }
    return outputFile.toPath();
    }
    private byte[] encryptAesKeyWithRsa(SecretKey aesKey, java.security.PublicKey publicKey) throws Exception {
    javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsaCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
    return rsaCipher.doFinal(aesKey.getEncoded());
}
    private void generateQRCode(byte[] encryptedKey, String filePath) throws Exception {
    // Convert encrypted bytes to Base64 for QR compatibility
    String qrData = Base64.getEncoder().encodeToString(encryptedKey);
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 300, 300);
    
    Path path = Paths.get(filePath);
    Files.createDirectories(path.getParent());
    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
}
}
