package com.zuvo.web;

import com.zuvo.entity.*;
import com.zuvo.repo.*;
import com.zuvo.service.MediaService;
import com.zuvo.service.ModerationService;
import com.zuvo.service.PresenceService;
import com.zuvo.service.Views;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PostController {
    public record CommentRequest(String text) {}

    private static final Set<String> TYPES = Set.of("PHOTO", "VIDEO", "REEL", "QUOTE");

    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final FollowRepository follows;
    private final UserInterestRepository interests;
    private final UserRepository users;
    private final MediaService media;
    private final ModerationService moderation;
    private final PresenceService presence;
    private final Views views;

    public PostController(PostRepository posts, CommentRepository comments, PostLikeRepository likes,
                          FollowRepository follows, UserInterestRepository interests, UserRepository users,
                          MediaService media, ModerationService moderation, PresenceService presence, Views views) {
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.follows = follows;
        this.interests = interests;
        this.users = users;
        this.media = media;
        this.moderation = moderation;
        this.presence = presence;
        this.views = views;
    }

    private Post find(Long id) {
        return posts.findById(id).orElseThrow(() -> ApiException.notFound("Post not found."));
    }

    private List<Long> followingIds(Long uid) {
        List<Long> ids = new ArrayList<>();
        for (Follow f : follows.findByFollowerIdAndStatus(uid, Follow.ACCEPTED)) ids.add(f.followingId);
        return ids;
    }

    /** Create a photo, video, reel or quote. The user must be logged in AND connected (online). */
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Views.PostView create(@RequestAttribute("uid") Long uid,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String caption,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(defaultValue = "FOLLOWERS") String visibility,
                                 @RequestParam(required = false) MultipartFile file) {
        if (!presence.isOnline(uid))
            throw ApiException.forbidden("You're offline. Reconnect to the internet to post.");
        type = type == null ? "" : type.toUpperCase();
        if (!TYPES.contains(type)) throw ApiException.bad("Choose what you want to post.");
        if (!visibility.equals("PUBLIC") && !visibility.equals("FOLLOWERS")) visibility = "FOLLOWERS";
        if (category != null && !category.isBlank() && !ProfileController.CATEGORIES.contains(category))
            throw ApiException.bad("Choose a valid topic.");
        caption = caption == null ? "" : caption.trim();
        if (caption.length() > 1000) throw ApiException.bad("Captions can be up to 1000 characters.");
        if (type.equals("QUOTE") && caption.isBlank()) throw ApiException.bad("Write your quote first.");
        if (moderation.isTextFlagged(caption))
            throw ApiException.bad("Your text was blocked by our AI safety check. Please change it and try again.");

        Post p = new Post();
        p.userId = uid;
        p.type = type;
        p.caption = caption;
        p.category = (category == null || category.isBlank()) ? null : category;
        p.visibility = visibility;

        if (!type.equals("QUOTE")) {
            MediaService.Uploaded up = media.upload(file, "posts", type.equals("PHOTO") ? "image" : "video");
            if (type.equals("PHOTO") && moderation.isImageFlagged(up.url())) {
                media.delete(up.publicId(), up.resourceType());
                throw ApiException.bad("This photo was blocked by our AI safety check.");
            }
            p.mediaUrl = up.url();
            p.mediaPublicId = up.publicId();
            p.resourceType = up.resourceType();
        }
        return views.post(posts.save(p), uid, false);
    }

    /** Home feed: my posts + people I follow. New users also get Discover content matched to their interests. */
    @GetMapping("/feed")
    public Map<String, Object> feed(@RequestAttribute("uid") Long uid, @RequestParam(defaultValue = "0") int page) {
        List<Long> following = followingIds(uid);
        List<Long> ids = new ArrayList<>(following);
        ids.add(uid);
        List<Views.PostView> out = new ArrayList<>();
        List<Post> mine = posts.findByUserIdInOrderByCreatedAtDesc(ids, PageRequest.of(page, 20));
        for (Post p : mine) out.add(views.post(p, uid, false));

        boolean sparse = following.isEmpty() || mine.size() < 10;
        if (page == 0 && sparse) {
            Set<String> wanted = new HashSet<>();
            for (UserInterest i : interests.findByUserId(uid)) wanted.add(i.category);
            List<Post> discover = new ArrayList<>(posts.findDiscover(ids, PageRequest.of(0, 30)));
            discover.sort(Comparator.comparingInt(p -> wanted.contains(p.category) ? 0 : 1)); // interests first
            for (Post p : discover.subList(0, Math.min(20, discover.size()))) out.add(views.post(p, uid, true));
        }
        return J.of("posts", out, "discoverMode", sparse, "hasFollowing", !following.isEmpty());
    }

    @GetMapping("/reels")
    public List<Views.PostView> reels(@RequestAttribute("uid") Long uid, @RequestParam(defaultValue = "0") int page) {
        List<Long> ids = followingIds(uid);
        ids.add(uid);
        List<Views.PostView> out = new ArrayList<>();
        for (Post p : posts.findReels(ids, PageRequest.of(page, 12))) out.add(views.post(p, uid, false));
        return out;
    }

    /** Like / unlike. Allowed on my own posts, Discover posts, and posts of people who accepted my follow request. */
    @PostMapping("/posts/{id}/like")
    public Map<String, Object> like(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        Post p = find(id);
        if (!views.canInteract(p, uid))
            throw ApiException.forbidden("Send a follow request. You can like posts once it's accepted.");
        Optional<PostLike> existing = likes.findByPostIdAndUserId(id, uid);
        boolean liked;
        if (existing.isPresent()) {
            likes.delete(existing.get());
            liked = false;
        } else {
            PostLike l = new PostLike();
            l.postId = id;
            l.userId = uid;
            likes.save(l);
            liked = true;
            if (!p.userId.equals(uid))
                presence.sendTo(p.userId, J.of("type", "notify", "text", name(uid) + " liked your post"));
        }
        return J.of("liked", liked, "likeCount", likes.countByPostId(id));
    }

    @GetMapping("/posts/{id}/comments")
    public List<Views.CommentView> comments(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        Post p = find(id);
        if (!views.canSee(p, uid)) throw ApiException.forbidden("Follow this person to see their comments.");
        return comments.findByPostIdOrderByCreatedAtAsc(id).stream().map(views::comment).toList();
    }

    /** Add a comment. It is checked by the AI moderation API, then saved in MySQL. */
    @PostMapping("/posts/{id}/comments")
    public Views.CommentView comment(@RequestAttribute("uid") Long uid, @PathVariable Long id,
                                     @RequestBody CommentRequest r) {
        Post p = find(id);
        if (!views.canInteract(p, uid))
            throw ApiException.forbidden("Send a follow request. You can comment once it's accepted.");
        String text = r.text() == null ? "" : r.text().trim();
        if (text.isEmpty()) throw ApiException.bad("Write a comment first.");
        if (text.length() > 1000) throw ApiException.bad("Comments can be up to 1000 characters.");
        if (moderation.isTextFlagged(text))
            throw ApiException.bad("Your comment was blocked by our AI safety check because it may be harmful.");
        Comment c = new Comment();
        c.postId = id;
        c.userId = uid;
        c.body = text;
        c = comments.save(c);
        if (!p.userId.equals(uid))
            presence.sendTo(p.userId, J.of("type", "notify", "text", name(uid) + " commented on your post"));
        return views.comment(c);
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    public Map<String, Object> delete(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        Post p = find(id);
        if (!p.userId.equals(uid)) throw ApiException.forbidden("You can only delete your own posts.");
        comments.deleteByPostId(id);
        likes.deleteByPostId(id);
        posts.delete(p);
        media.delete(p.mediaPublicId, p.resourceType);
        return J.of("ok", true);
    }

    private String name(Long uid) {
        return users.findById(uid).map(u -> u.fullName).orElse("Someone");
    }
}
