import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class Encyptor {

  private String key;
  private String initVector = "OnzeEigenVector1";
  private IvParameterSpec ivspec;
  private SecretKeySpec skeySpec;

  Encyptor() {
    this.key = "DitRaadNiemand98";
    this.ivspec = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
    this.skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
  }

  String encrypt(String value) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivspec);

      byte[] encrypted = cipher.doFinal(value.getBytes());

      return new String(Base64.getEncoder().encode(encrypted));
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  String decrypt(String encrypted) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivspec);

      byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));

      return new String(original);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
