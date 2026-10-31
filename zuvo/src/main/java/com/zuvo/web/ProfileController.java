package com.zuvo.web;

import com.zuvo.entity.*;
import com.zuvo.repo.*;
import com.zuvo.service.MediaService;
import com.zuvo.service.ModerationService;
import com.zuvo.service.Views;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ProfileController {
    public static final Set<String> CATEGORIES =
            new LinkedHashSet<>(List.of("Nature", "Music", "Sports", "Travel", "Comedy", "Food", "Tech", "Art", "Fitness"));

    public record UpdateProfile(String fullName, String bio, String gender) {}
    public record InterestsRequest(List<String> categories) {}

    private final UserRepository users;
    private final FollowRepository follows;
    private final PostRepository posts;
    private final UserInterestRepository interests;
    private final MediaService media;
    private final ModerationService moderation;
    private final Views views;

    public ProfileController(UserRepository users, FollowRepository follows, PostRepository posts,
                             UserInterestRepository interests, MediaService media,
                             ModerationService moderation, Views views) {
        this.users = users;
        this.follows = follows;
        this.posts = posts;
        this.interests = interests;
        this.media = media;
        this.moderation = moderation;
        this.views = views;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> profile(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        User u = find(id);
        String relation = "NONE";
        if (id.equals(uid)) relation = "SELF";
        else {
            Optional<Follow> f = follows.findByFollowerIdAndFollowingId(uid, id);
            if (f.isPresent() && !Follow.DECLINED.equals(f.get().status)) relation = f.get().status;
        }
        Optional<Follow> back = follows.findByFollowerIdAndFollowingId(id, uid);
        Map<String, Object> m = J.of("user", views.user(u), "posts", posts.countByUserId(id),
                "followers", follows.countByFollowingIdAndStatus(id, Follow.ACCEPTED),
                "following", follows.countByFollowerIdAndStatus(id, Follow.ACCEPTED),
                "relation", relation,
                "followsMe", back.isPresent() && Follow.ACCEPTED.equals(back.get().status),
                "requestedMe", back.isPresent() && Follow.PENDING.equals(back.get().status),
                "incomingRequestId", back.isPresent() && Follow.PENDING.equals(back.get().status) ? back.get().id : null);
        return m;
    }

    @GetMapping("/users/{id}/posts")
    public Map<String, Object> userPosts(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        find(id);
        boolean allowed = id.equals(uid) || views.isFollowing(uid, id);
        List<Views.PostView> out = new ArrayList<>();
        for (Post p : posts.findByUserIdOrderByCreatedAtDesc(id)) {
            if (allowed || views.canSee(p, uid)) out.add(views.post(p, uid, false));
        }
        return J.of("locked", !allowed && out.isEmpty(), "posts", out);
    }

    @GetMapping("/users/search")
    public List<Views.UserView> search(@RequestAttribute("uid") Long uid, @RequestParam String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        return users.findTop20ByFullNameContainingIgnoreCaseAndEmailVerifiedTrue(q.trim()).stream()
                .filter(u -> !u.id.equals(uid) && !"SYSTEM".equals(u.role)).map(views::user).toList();
    }

    @PutMapping("/me")
    public Map<String, Object> update(@RequestAttribute("uid") Long uid, @RequestBody UpdateProfile r) {
        User u = find(uid);
        if (r.fullName() != null) {
            String n = r.fullName().trim().replaceAll("\\s+", " ");
            if (n.length() < 2 || n.length() > 120) throw ApiException.bad("Enter your full name.");
            u.fullName = n;
        }
        if (r.bio() != null) {
            String b = r.bio().trim();
            if (b.length() > 300) throw ApiException.bad("Your bio can be up to 300 characters.");
            if (moderation.isTextFlagged(b)) throw ApiException.bad("Your bio was blocked by our safety check.");
            u.bio = b;
        }
        if (r.gender() != null && !r.gender().isBlank()) u.gender = r.gender();
        users.save(u);
        return J.of("user", views.user(u));
    }

    @PostMapping("/me/picture")
    public Map<String, Object> picture(@RequestAttribute("uid") Long uid, @RequestParam("file") MultipartFile file) {
        User u = find(uid);
        MediaService.Uploaded up = media.upload(file, "avatars", "image");
        if (moderation.isImageFlagged(up.url())) {
            media.delete(up.publicId(), up.resourceType());
            throw ApiException.bad("This picture was blocked by our safety check. Choose another one.");
        }
        u.profilePicUrl = up.url();
        users.save(u);
        return J.of("profilePicUrl", u.profilePicUrl);
    }

    @GetMapping("/me/interests")
    public List<String> myInterests(@RequestAttribute("uid") Long uid) {
        return interests.findByUserId(uid).stream().map(i -> i.category).toList();
    }

    @PostMapping("/me/interests")
    @Transactional
    public Map<String, Object> saveInterests(@RequestAttribute("uid") Long uid, @RequestBody InterestsRequest r) {
        List<String> cats = r.categories() == null ? List.of() : r.categories().stream().filter(CATEGORIES::contains).distinct().toList();
        if (cats.isEmpty()) throw ApiException.bad("Pick at least one topic.");
        interests.deleteByUserId(uid);
        for (String c : cats) interests.save(new UserInterest(uid, c));
        return J.of("categories", cats);
    }
}
