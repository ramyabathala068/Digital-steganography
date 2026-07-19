package frontend;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KeyGenerator {
    public static void main(String[] args) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

            // Ensure directory exists and save files
            java.nio.file.Path path = Paths.get("saved");
            Files.createDirectories(path);
            Files.write(path.resolve("public_key.txt"), publicKey.getBytes());
            Files.write(path.resolve("private_key.txt"), privateKey.getBytes());

            System.out.println("Success! Keys saved to 'saved/public_key.txt' and 'saved/private_key.txt'.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
