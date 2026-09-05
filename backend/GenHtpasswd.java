import java.security.SecureRandom;
import java.security.MessageDigest;

/**
 * 生成 Nginx auth_basic 所需的 htpasswd 条目（apr1 / MD5-crypt 算法）。
 * 仅用于本地上线前生成密码文件，不参与业务运行。
 */
public class GenHtpasswd {
    public static void main(String[] args) throws Exception {
        // verify <password> <salt> <expected-hash>：校验实现正确性
        if (args.length == 4 && "verify".equals(args[0])) {
            String computed = apr1WithSalt(args[1], args[2]);
            System.out.println(computed.equals(args[3]) ? "OK" : "MISMATCH: " + computed);
            return;
        }
        String user = args.length > 0 ? args[0] : "admin";
        String pwd = args.length > 1 ? args[1] : "admin123";
        System.out.println(user + ":" + apr1(pwd));
    }

    /** Apache $apr1$ MD5-crypt（与 openssl passwd -apr1 等价） */
    static String apr1(String password) throws Exception {
        SecureRandom random = new SecureRandom();
        String salt = String.format("erp%05d", random.nextInt(100000));
        salt = salt.substring(0, 8);
        return apr1WithSalt(password, salt);
    }

    static String apr1WithSalt(String password, String salt) throws Exception {
        String pwSalt = password + salt;
        byte[] ctx = MessageDigest.getInstance("MD5").digest(pwSalt.getBytes());

        MessageDigest md1 = MessageDigest.getInstance("MD5");
        md1.update(password.getBytes());
        md1.update(salt.getBytes());
        md1.update(password.getBytes());
        byte[] finalBuf = md1.digest();

        for (int i = password.length(); i > 0; i -= 16) {
            md1.update(finalBuf, 0, Math.min(16, i));
        }
        java.util.Arrays.fill(finalBuf, (byte) 0);

        for (int i = password.length(); i > 0; i >>= 1) {
            if ((i & 1) == 1) md1.update((byte) 0);
            else md1.update(password.getBytes(), 0, 1);
        }
        byte[] final2 = md1.digest();

        // 迭代混淆（与 Apache 实现一致的顺序）
        for (int i = 0; i < 1000; i++) {
            md1.reset();
            if ((i & 1) == 1) md1.update(password.getBytes());
            else md1.update(ctx, 0, 16);
            if (i % 3 != 0) md1.update(salt.getBytes());
            if (i % 7 != 0) md1.update(password.getBytes());
            if ((i & 1) == 1) md1.update(ctx, 0, 16);
            else md1.update(final2, 0, 16);
            ctx = md1.digest();
        }

        StringBuilder sb = new StringBuilder("$apr1$").append(salt).append('$');
        final char[] itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        int[] order = {0, 6, 12, 1, 7, 13, 2, 8, 14, 3, 9, 15, 4, 10, 5, 11};
        for (int i = 0; i < 15; i += 3) {
            long l = ((ctx[order[i]] & 0xff) << 16) | ((ctx[order[i + 1]] & 0xff) << 8) | (ctx[order[i + 2]] & 0xff);
            for (int j = 0; j < 4; j++) sb.append(itoa64[(int) ((l >> (6 * j)) & 0x3f)]);
        }
        long l = ctx[order[15]] & 0xff;
        for (int j = 0; j < 2; j++) sb.append(itoa64[(int) ((l >> (6 * j)) & 0x3f)]);
        return sb.toString();
    }
}
