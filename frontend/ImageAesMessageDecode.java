package frontend;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import javax.swing.JScrollPane; 
import javax.swing.JTextArea; 
import java.awt.image.DataBufferByte;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.Base64;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
public class ImageAesMessageDecode extends JFrame {
    
    private String currentShortPassphrase;
    private File selectedFile;   // This will represent the Stego Image
    private File selectedQrFile; // NEW: To store the selected QR Code file
    private JLabel encodedTextLabel;
    private String extractedMessage = ""; // NEW FIELD: To store the extracted secret message

	public static int DECRYPTED_IMAGE_WIDTH = 0; 
	public static int DECRYPTED_IMAGE_HEIGHT = 0;
    
    public ImageAesMessageDecode() {
    setTitle("Upload and Decode (with Message)");
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

        // Browse Files button
        JPanel firstBoxPanel = createDragAndDropPanel();
        /*JButton browseButton = new JButton("Browse Files");
        browseButton.setPreferredSize(new Dimension(120, 30)); 
        browseButton.addActionListener(e -> {
            openFileChooser();
        });
        firstBoxPanel.add(browseButton);

        panel.add(firstBoxPanel, BorderLayout.CENTER);*/
        firstBoxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));	
        JPanel selectionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10));
	selectionButtonPanel.setOpaque(false);
        // Browse Stego Image button
	JButton browseImageButton = new JButton("Select Stego Image");
	browseImageButton.setPreferredSize(new Dimension(400, 60)); 
	browseImageButton.addActionListener(e -> {
	    openFileChooser(true); // true for image
	});

// Browse QR Code button
	JButton browseQrButton = new JButton("Select Session QR");
	browseQrButton.setPreferredSize(new Dimension(400, 60)); 
	browseQrButton.addActionListener(e -> {
	    openFileChooser(false); // false for QR
	});
	
	selectionButtonPanel.add(browseImageButton);
	selectionButtonPanel.add(browseQrButton);

// 5. Add this sub-panel to your main drag-and-drop area (the center box)
// Replace 'firstBoxPanel.add(browseQrButton)' with:
firstBoxPanel.setLayout(new GridBagLayout()); 
firstBoxPanel.add(selectionButtonPanel, new GridBagConstraints());
panel.add(firstBoxPanel, BorderLayout.CENTER);

        // Content has been DECODED area
        JPanel secondBoxPanel = createOutlinedBoxPanel(" ");
        JButton downloadButton = new JButton("Download");
        JButton homeButton = new JButton("Home");
        downloadButton.setPreferredSize(new Dimension(120, 30)); 
        downloadButton.addActionListener(e -> {
            decryptFiles();
        });
        homeButton.setPreferredSize(new Dimension(120, 30));
        homeButton.addActionListener(e -> goHome());

        // Initialization for the main status/text label
        encodedTextLabel = new JLabel("Ready for Decryption", SwingConstants.CENTER); 
        encodedTextLabel.setFont(new Font("Arial", Font.BOLD, 30));
        encodedTextLabel.setForeground(Color.BLACK);

        secondBoxPanel.setLayout(new BorderLayout());
        secondBoxPanel.add(encodedTextLabel, BorderLayout.CENTER);
        secondBoxPanel.add(downloadButton, BorderLayout.WEST);
        secondBoxPanel.add(homeButton, BorderLayout.EAST);

        panel.add(secondBoxPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 90, 20, 90));

        setContentPane(panel);
        setLocationRelativeTo(null);
    }
    
    // NEW: Display message/image helper
    private void displayDecryptedImageContent(Path imagePath) {
        String messageContent = "Extracted Secret Message:\n" + 
                                extractedMessage + 
                                "\n\nDecoded image saved:\n" + imagePath;
        
        JTextArea textArea = new JTextArea(messageContent);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, 
            scrollPane, 
            "Decrypted Image Content", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Core Logic (Copied and Modified) ---

   private void decryptFiles() {
    if (selectedFile == null || selectedQrFile == null) {
        JOptionPane.showMessageDialog(this, "Please select both the Stego Image and the QR Code.", "Missing Files", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Step 1: Browse for the Private Key file
    JFileChooser keyChooser = new JFileChooser("saved");
    keyChooser.setDialogTitle("Select Your Private Key (.txt)");
    int result = keyChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
        try {
            // Step 2: Read the key string from the file
            File keyFile = keyChooser.getSelectedFile();
            String privateKeyStr = new String(Files.readAllBytes(keyFile.toPath())).trim();

            // Step 3: Extract AES Session Key from QR Code
            byte[] encryptedKeyPackage = decodeQRCode(selectedQrFile);

            // Step 4: Decode Private Key string to PrivateKey object
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.PrivateKey privateKey = kf.generatePrivate(spec);

            // Step 5: Decrypt the Session Key using RSA
            SecretKey sessionKey = decryptAesKeyWithRsa(encryptedKeyPackage, privateKey);

            // Step 6: Unlock the Stego Image
            byte[] decryptedImage = performAesWithLsb(selectedFile, sessionKey);
            
            if (decryptedImage.length > 0) {
    	    Path imagePath = saveDecryptedImage(decryptedImage);
    
    // Explicitly update the label to show the secret message
    	    encodedTextLabel.setText("Message: " + extractedMessage); 
    	    encodedTextLabel.setForeground(new Color(0, 100, 0)); // Dark Green
    
    	    displayDecryptedImageContent(imagePath);
	}
        } catch (Exception e) {
            encodedTextLabel.setText("DECRYPTION FAILED");
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Decryption Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
    
    // MODIFIED: Logic to dynamically extract the secret message.
    private byte[] extractEncryptedBytesRedundantly(byte[] container) {
    byte[] anchor = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
    
    for (int i = 0; i < container.length - 1000; i++) {
        if (isAnchorAtBitLevel(container, i, anchor)) {
            try {
                int bitOffset = i + (anchor.length * 8); 
                int totalPayloadSize = extractIntAtBitLevel(container, bitOffset);
                bitOffset += 32;

                if (totalPayloadSize > 0 && totalPayloadSize < (container.length / 8)) {
                    return extractBytesAtBitLevel(container, bitOffset, totalPayloadSize);
                }
            } catch (Exception e) { continue; }
        }
    }
    return null;
}

private boolean isAnchorAtBitLevel(byte[] container, int offset, byte[] anchor) {
    if (offset + (anchor.length * 8) > container.length) return false;
    for (int i = 0; i < anchor.length; i++) {
        byte extracted = 0;
        for (int j = 7; j >= 0; j--) {
            int bit = container[offset + (i * 8) + (7 - j)] & 1;
            extracted |= (bit << j);
        }
        if (extracted != anchor[i]) return false;
    }
    return true;
}
    // Helper method to extract a 32-bit integer from the LSBs of the container
private int extractIntAtBitLevel(byte[] container, int bitOffset) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    for (int i = 0; i < Integer.BYTES; i++) {
        byte b = 0;
        for (int j = 7; j >= 0; j--) {
            // Reconstruct the byte bit-by-bit from the LSBs
            // (7-j) ensures we read in the same order we embedded (MSB first)
            int currentBitPos = bitOffset + (i * 8) + (7 - j);
            b |= ((container[currentBitPos] & 1) << j);
        }
        buffer.put(b);
    }
    buffer.flip();
    return buffer.getInt();
}

// Helper method to extract a specific number of bytes from the LSBs of the container
private byte[] extractBytesAtBitLevel(byte[] container, int bitOffset, int size) {
    byte[] extractedData = new byte[size];
    for (int i = 0; i < size; i++) {
        byte b = 0;
        for (int j = 7; j >= 0; j--) {
            int currentBitPos = bitOffset + (i * 8) + (7 - j);
            b |= ((container[currentBitPos] & 1) << j);
        }
        extractedData[i] = b;
    }
    return extractedData;
}
    // --- Utility Methods (Copied from UploadPageDecode.java) ---

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
        JOptionPane.showMessageDialog(this, "Selected file: " + this.selectedFile.getName());
        encodedTextLabel.setText("Ready for Decryption");
        encodedTextLabel.setForeground(Color.BLACK);
    }
    
    private SecretKey deriveAesKey(String passphrase) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = passphrase.getBytes("UTF-8");
        keyBytes = sha.digest(keyBytes);
        
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
    }

    private void openKeyDialog() {
    // We no longer need a text input dialog because decryptFiles() 
    // now uses JFileChooser to select a private_key.txt file.
    
    if (selectedFile == null || selectedQrFile == null) {
        JOptionPane.showMessageDialog(this, 
            "Please select both the Stego Image and the QR Code first.", 
            "Missing Files", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Directly call the new file-based decryption method
    decryptFiles(); 
}

    private boolean isValidKey(String enteredKey) {
        this.currentShortPassphrase = enteredKey.trim();
        return !this.currentShortPassphrase.isEmpty(); 
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
            "Decrypted Text Content", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private byte[] performRsaWithXor(File textFile, String passphrase) {
        try {
            byte[] fileContent = Files.readAllBytes(textFile.toPath());
            byte xorKey = (byte) 0xAA; 
            byte[] aesEncryptedContent = new byte[fileContent.length];
            for (int i = 0; i < fileContent.length; i++) {
                 aesEncryptedContent[i] = (byte) (fileContent[i] ^ xorKey);
            }

            SecretKey aesKey = deriveAesKey(passphrase);
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); 
            cipher.init(Cipher.DECRYPT_MODE, aesKey); 
            byte[] decryptedContent = cipher.doFinal(aesEncryptedContent);
            return decryptedContent;
        } catch (Exception e) {
            System.err.println("AES/XOR Decryption Failed. Check the Passphrase and file integrity.");
            return new byte[0];
        }
    }

   private byte[] performAesWithLsb(File imageFile, SecretKey recoveredAesKey) {
    try {
        BufferedImage stegoImage = ImageIO.read(imageFile);
        byte[] imageBytes = ((java.awt.image.DataBufferByte) stegoImage.getRaster().getDataBuffer()).getData();
        
        // 1. Extract the raw payload (Width + Height + Message)
        byte[] fullPayload = extractEncryptedBytesRedundantly(imageBytes); 
        if (fullPayload == null) return new byte[0];

        java.nio.ByteBuffer payloadBuffer = java.nio.ByteBuffer.wrap(fullPayload);

        // --- THE FIX: REMOVE THE KEY PACKAGE EXTRACTION ---
        // We only read Width, Height, and the Encrypted Message now.
        DECRYPTED_IMAGE_WIDTH = payloadBuffer.getInt();
        DECRYPTED_IMAGE_HEIGHT = payloadBuffer.getInt();
        
        // 2. Extract the Encrypted Message Bytes
        byte[] encryptedMsgBytes = new byte[payloadBuffer.remaining()];
        payloadBuffer.get(encryptedMsgBytes); // No more UnderflowException here!

        // 3. Decrypt the Message using the key we got from the QR Code
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding"); 
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, recoveredAesKey);
        
        byte[] decryptedMessageBytes = cipher.doFinal(encryptedMsgBytes);
        this.extractedMessage = new String(decryptedMessageBytes, "UTF-8").trim();
        
        return imageBytes; 
    } catch (Exception e) {
        e.printStackTrace();
        return new byte[0];
    }
}
    
    private Path saveDecryptedImage(byte[] decryptedImage) {
    Path directoryPath = Paths.get("saved");
    Path imagePath = directoryPath.resolve("decrypted_image.png");
    
    try {
        // Use the metadata found by the Anchor Scanner
        int originalWidth = DECRYPTED_IMAGE_WIDTH; 
        int originalHeight = DECRYPTED_IMAGE_HEIGHT;

        // If the image was cropped, we can't reconstruct the FULL original image.
        // Instead, we save the available pixels as a square or a linear strip 
        // to prevent the "ArrayIndexOutOfBounds" crash.
        int totalPixelsAvailable = decryptedImage.length / 3;
        
        // Use the original width if possible, otherwise fallback to a 1:1 ratio
        int finalWidth = (originalWidth > 0 && originalWidth <= totalPixelsAvailable) ? originalWidth : (int)Math.sqrt(totalPixelsAvailable);
        int finalHeight = totalPixelsAvailable / finalWidth;

        if (finalWidth <= 0 || finalHeight <= 0) {
            throw new IllegalArgumentException("Insufficient data to reconstruct image.");
        }
        
        BufferedImage recoveredImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_3BYTE_BGR);
        byte[] targetPixels = ((DataBufferByte) recoveredImage.getRaster().getDataBuffer()).getData();
        
        // Only copy what actually exists in the decrypted array
        int bytesToCopy = Math.min(decryptedImage.length, targetPixels.length);
        System.arraycopy(decryptedImage, 0, targetPixels, 0, bytesToCopy);
        
        Files.createDirectories(directoryPath); 
        ImageIO.write(recoveredImage, "png", imagePath.toFile()); 

    } catch (Exception e) {
        e.printStackTrace(); // This will now show the REAL error in your console
        JOptionPane.showMessageDialog(null, "Error saving: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
    }
    return imagePath;
}
    private boolean isTextFile(File file) {
        return file.getName().toLowerCase().endsWith(".txt");
    }

    private void openFileChooser(boolean isImage) {
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showOpenDialog(this);

    if (result == JFileChooser.APPROVE_OPTION) {
        if (isImage) {
            this.selectedFile = fileChooser.getSelectedFile(); 
            JOptionPane.showMessageDialog(this, "Stego Image Selected: " + this.selectedFile.getName());
        } else {
            this.selectedQrFile = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this, "QR Code Selected: " + this.selectedQrFile.getName());
        }
        
        // Update label to show current selection status
        String imgStatus = (selectedFile != null) ? "Image: " + selectedFile.getName() : "No Image";
        String qrStatus = (selectedQrFile != null) ? "QR: " + selectedQrFile.getName() : "No QR";
        encodedTextLabel.setText(imgStatus + " | " + qrStatus);
        encodedTextLabel.setForeground(Color.BLUE);
    }
}
    private SecretKey decryptAesKeyWithRsa(byte[] encryptedKeyPackage, java.security.PrivateKey privateKey) throws Exception {
    javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
    rsaCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
    byte[] decryptedKeyBytes = rsaCipher.doFinal(encryptedKeyPackage);
    return new javax.crypto.spec.SecretKeySpec(decryptedKeyBytes, "AES");
}
    private byte[] decodeQRCode(File qrFile) throws Exception {
    BufferedImage bufferedImage = ImageIO.read(qrFile);
    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    
    Result result = new MultiFormatReader().decode(bitmap);
    return Base64.getDecoder().decode(result.getText());
}
}
