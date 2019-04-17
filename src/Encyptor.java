import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encyptor {

  private String key;
  private String initVector = "OnzeEigenVector1";

  Encyptor() {
    this.key = "DitRaadNiemand98";
  }

  public String encrypt(String value) {
    try {
      IvParameterSpec ivspec = new IvParameterSpec(initVector.getBytes("UTF-8"));
      SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivspec);

      byte[] encrypted = cipher.doFinal(value.getBytes());

      return new String(Base64.getEncoder().encode(encrypted));
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  public String decrypt(String encrypted) {
    try {
      IvParameterSpec ivspec = new IvParameterSpec(initVector.getBytes("UTF-8"));
      SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

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
