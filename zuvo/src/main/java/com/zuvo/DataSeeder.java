package com.zuvo;

import com.zuvo.entity.Post;
import com.zuvo.entity.User;
import com.zuvo.repo.PostRepository;
import com.zuvo.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Runs at start-up. Creates the "Zuvo Discover" account and a few curated photos/videos so brand-new
 * users (with no followers yet) never see an empty feed.
 * The media below are Cloudinary's public demo files: replace them with your own free-licence clips
 * (e.g. from Pexels, Pixabay or Unsplash - check each licence) uploaded to your Cloudinary account.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PostRepository posts;

    public DataSeeder(UserRepository users, PostRepository posts) {
        this.users = users;
        this.posts = posts;
    }

    @Override
    public void run(String... args) {
        if (users.findByEmailIgnoreCase("discover@zuvo.local").isPresent()) return;
        User u = new User();
        u.fullName = "Zuvo Discover";
        u.dateOfBirth = LocalDate.of(2000, 1, 1);
        u.gender = "Other";
        u.email = "discover@zuvo.local";
        u.passwordHash = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()); // nobody can log in
        u.emailVerified = true;
        u.bio = "Nature, travel and short clips picked for you.";
        u.role = "SYSTEM";
        u = users.save(u);

        String img = "https://res.cloudinary.com/demo/image/upload/";
        String vid = "https://res.cloudinary.com/demo/video/upload/";
        add(u.id, "REEL", vid + "elephants.mp4", "video", "Gentle giants on the move.", "Nature");
        add(u.id, "PHOTO", img + "sample.jpg", "image", "Spring colours.", "Nature");
        add(u.id, "REEL", vid + "sea-turtle.mp4", "video", "Calm waters, slow swim.", "Nature");
        add(u.id, "PHOTO", img + "cld-sample.jpg", "image", "A quiet corner to explore.", "Travel");
        add(u.id, "REEL", vid + "dog.mp4", "video", "Zero effort, maximum joy.", "Comedy");
        add(u.id, "PHOTO", img + "cld-sample-2.jpg", "image", "Fresh and simple.", "Food");
        add(u.id, "QUOTE", null, null, "Small steps every day still take you a long way.", "Art");
        add(u.id, "PHOTO", img + "cld-sample-3.jpg", "image", "Colour and light.", "Art");
    }

    private void add(Long uid, String type, String url, String resType, String caption, String category) {
        Post p = new Post();
        p.userId = uid;
        p.type = type;
        p.mediaUrl = url;
        p.resourceType = resType;
        p.caption = caption;
        p.category = category;
        p.visibility = "PUBLIC";
        p.curated = true;
        posts.save(p);
    }
}
