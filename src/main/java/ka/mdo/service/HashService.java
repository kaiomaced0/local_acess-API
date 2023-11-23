package ka.mdo.service;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HashService {

    private String salt = "#blahhxyz9232";
    private Integer iterationCount = 223;
    private Integer keylength = 512;

    public String getHashSenha(String senha) {
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
                    .generateSecret(
                            new PBEKeySpec(senha.toCharArray(), salt.getBytes(), iterationCount, keylength)
                    )
                    .getEncoded();
            return Base64.getEncoder().encodeToString(result);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        HashService service = new HashService();
        System.out.println();
        System.out.println(service.getHashSenha("123"));
    }
}
