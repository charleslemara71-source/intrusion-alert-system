package com.zuvo.service;

import com.zuvo.entity.EmailVerification;
import com.zuvo.entity.User;
import com.zuvo.repo.EmailVerificationRepository;
import com.zuvo.repo.UserRepository;
import com.zuvo.web.ApiException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthService {
    public record RegisterRequest(String fullName, String dateOfBirth, String gender, String email, String password) {}

    private static final Set<String> GENDERS = Set.of("Male", "Female", "Other", "Prefer not to say");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository users;
    private final EmailVerificationRepository codes;
    private final EmailService email;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, long[]> failedLogins = new ConcurrentHashMap<>(); // email -> {count, firstFailMillis}

    public AuthService(UserRepository users, EmailVerificationRepository codes, EmailService email) {
        this.users = users;
        this.codes = codes;
        this.email = email;
    }

    public User register(RegisterRequest r) {
        String name = r.fullName() == null ? "" : r.fullName().trim().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 120) throw ApiException.bad("Enter your full name.");
        String mail = r.email() == null ? "" : r.email().trim().toLowerCase();
        if (!EMAIL.matcher(mail).matches()) throw ApiException.bad("Enter a valid email address.");
        if (r.password() == null || r.password().length() < 8)
            throw ApiException.bad("Your password must be at least 8 characters.");
        if (r.gender() == null || !GENDERS.contains(r.gender())) throw ApiException.bad("Choose a gender.");

        LocalDate dob;
        try {
            dob = LocalDate.parse(r.dateOfBirth());
        } catch (Exception e) {
            throw ApiException.bad("Enter a valid date of birth.");
        }
        if (dob.isAfter(LocalDate.now()) || dob.isBefore(LocalDate.now().minusYears(120)))
            throw ApiException.bad("Enter a valid date of birth.");
        // The 18+ rule is enforced HERE on the server, so it cannot be bypassed in the browser.
        if (Period.between(dob, LocalDate.now()).getYears() < 18)
            throw ApiException.bad("You must be 18 or older to create an account.");

        Optional<User> existing = users.findByEmailIgnoreCase(mail);
        if (existing.isPresent() && existing.get().emailVerified)
            throw ApiException.conflict("This email is already registered. Log in instead.");
        User u = existing.orElseGet(User::new);
        u.fullName = name;
        u.dateOfBirth = dob;
        u.gender = r.gender();
        u.email = mail;
        u.passwordHash = encoder.encode(r.password());
        u = users.save(u);
        issueCode(u);
        return u;
    }

    /** Creates a random 5-digit code, stores only its hash, and emails it. */
    public void issueCode(User u) {
        Optional<EmailVerification> last = codes.findTopByUserIdOrderByIdDesc(u.id);
        if (last.isPresent() && last.get().createdAt.isAfter(LocalDateTime.now().minusSeconds(45)))
            throw ApiException.tooMany("Wait a moment before asking for another code.");
        codes.findTopByUserIdAndUsedFalseOrderByIdDesc(u.id).ifPresent(old -> {
            old.used = true;
            codes.save(old);
        });
        String code = String.valueOf(10000 + random.nextInt(90000));
        EmailVerification ev = new EmailVerification();
        ev.userId = u.id;
        ev.codeHash = encoder.encode(code);
        ev.expiresAt = LocalDateTime.now().plusMinutes(10);
        codes.save(ev);
        email.sendCode(u.email, u.fullName.split(" ")[0], code);
    }

    public void resend(String mail) {
        User u = users.findByEmailIgnoreCase(mail == null ? "" : mail.trim())
                .orElseThrow(() -> ApiException.bad("Account not found."));
        if (u.emailVerified) throw ApiException.bad("This email is already verified. Log in instead.");
        issueCode(u);
    }

    // NOTE: not @Transactional on purpose - the attempt counter must be saved even when we throw an error.
    public User verify(String mail, String code) {
        User u = users.findByEmailIgnoreCase(mail == null ? "" : mail.trim())
                .orElseThrow(() -> ApiException.bad("Account not found."));
        if (u.emailVerified) return u;
        EmailVerification ev = codes.findTopByUserIdAndUsedFalseOrderByIdDesc(u.id)
                .orElseThrow(() -> ApiException.bad("There is no active code. Request a new one."));
        if (ev.expiresAt.isBefore(LocalDateTime.now())) throw ApiException.bad("This code has expired. Request a new one.");
        if (ev.attempts >= 5) throw ApiException.tooMany("Too many wrong attempts. Request a new code.");
        ev.attempts++;
        if (code == null || !encoder.matches(code.trim(), ev.codeHash)) {
            codes.save(ev);
            int left = 5 - ev.attempts;
            throw ApiException.bad(left > 0 ? "Incorrect code. " + left + " attempts left."
                                            : "Too many wrong attempts. Request a new code.");
        }
        ev.used = true;
        codes.save(ev);
        u.emailVerified = true;
        return users.save(u);
    }

    public User login(String mail, String password) {
        String key = mail == null ? "" : mail.trim().toLowerCase();
        long[] f = failedLogins.get(key);
        if (f != null && f[0] >= 5 && System.currentTimeMillis() - f[1] < 5 * 60_000)
            throw ApiException.tooMany("Too many failed attempts. Try again in a few minutes.");
        Optional<User> found = users.findByEmailIgnoreCase(key);
        if (found.isEmpty() || password == null || !encoder.matches(password, found.get().passwordHash)) {
            long[] n = (f == null || System.currentTimeMillis() - f[1] >= 5 * 60_000)
                    ? new long[]{1, System.currentTimeMillis()} : new long[]{f[0] + 1, f[1]};
            failedLogins.put(key, n);
            throw ApiException.unauthorized("Incorrect email or password.");
        }
        User u = found.get();
        if (!u.emailVerified) throw ApiException.forbidden("Verify your email first. We can send you a new code.");
        failedLogins.remove(key);
        return u;
    }
}
