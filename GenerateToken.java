import java.util.HashMap;

public class GenerateToken {
    public static void main(String[] args) {
        // 配置信息
        String secretKey = "smx";
        long ttlMillis = 7200000; // 2小时
        
        // 创建claims
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("user_id", 1); // 用户ID
        
        // 生成token
        String token = JwtUtil.createJWT(secretKey, ttlMillis, claims);
        System.out.println("Generated token: " + token);
    }
}

class JwtUtil {
    /**
     * 生成jwt
     * 使用Hs256算法, 私匙使用固定秘钥
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, java.util.Map<String, Object> claims) {
        // 指定签名的时候使用的签名算法，也就是header那部分
        io.jsonwebtoken.SignatureAlgorithm signatureAlgorithm = io.jsonwebtoken.SignatureAlgorithm.HS256;

        // 生成JWT的时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        java.util.Date exp = new java.util.Date(expMillis);

        // 设置jwt的body
        io.jsonwebtoken.JwtBuilder builder = io.jsonwebtoken.Jwts.builder()
                // 如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                .setClaims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(signatureAlgorithm, secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                // 设置过期时间
                .setExpiration(exp);

        return builder.compact();
    }
}
