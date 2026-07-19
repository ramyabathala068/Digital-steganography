package Algo;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Color;

public class VideoLSB {

    /**
     * Encodes a message into a video by modifying the LSB of pixels in the frames.
     */
    public static void encodeVideo(File source, File dest, String message) throws Exception {
        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(source));
        // Create an encoder with 25 frames per second
        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(dest, 25);
        
        Picture picture;
        boolean messageHidden = false;
        
        while (null != (picture = grab.getNativeFrame())) {
            BufferedImage frame = AWTUtil.toBufferedImage(picture);
            
            // For simplicity, we hide the entire message in the very first frame
            // In a more advanced version, you could spread bits across multiple frames
            if (!messageHidden) {
                embedMessage(frame, message);
                messageHidden = true;
            }
            
            encoder.encodeImage(frame);
        }
        encoder.finish();
    }

    /**
     * Decodes a message from an encoded video file.
     */
    public static String decodeVideo(File source) throws Exception {
        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(source));
        Picture picture = grab.getNativeFrame(); // Grab the first frame
        
        if (picture != null) {
            BufferedImage frame = AWTUtil.toBufferedImage(picture);
            return extractMessage(frame);
        }
        return "No message found.";
    }

    private static void embedMessage(BufferedImage image, String message) {
        // Add a terminator to know where the message ends during decoding
        String endMessage = message + "\0"; 
        int messageIndex = 0;
        int bitIndex = 0;

        for (int y = 0; y < image.getHeight() && messageIndex < endMessage.length(); y++) {
            for (int x = 0; x < image.getWidth() && messageIndex < endMessage.length(); x++) {
                Color pixel = new Color(image.getRGB(x, y));
                int red = pixel.getRed();

                // Get the specific bit of the current character
                int bit = (endMessage.charAt(messageIndex) >> bitIndex) & 1;

                // Modify the Least Significant Bit of the Red channel
                red = (red & 254) | bit;

                image.setRGB(x, y, new Color(red, pixel.getGreen(), pixel.getBlue()).getRGB());

                bitIndex++;
                if (bitIndex == 8) {
                    bitIndex = 0;
                    messageIndex++;
                }
            }
        }
    }

    private static String extractMessage(BufferedImage image) {
        StringBuilder message = new StringBuilder();
        int bitIndex = 0;
        char currentByte = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y));
                int red = pixel.getRed();

                // Extract the LSB from the Red channel
                int bit = red & 1;
                currentByte |= (bit << bitIndex);

                bitIndex++;
                if (bitIndex == 8) {
                    if (currentByte == '\0') {
                        return message.toString();
                    }
                    message.append(currentByte);
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
        }
        return message.toString();
    }
}
