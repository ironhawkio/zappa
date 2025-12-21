import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "zappa123";
        String encodedPassword = encoder.encode(password);
        System.out.println("Encoded password: " + encodedPassword);
    }
}
