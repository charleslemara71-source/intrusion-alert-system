package com.zuvo.service;

import com.zuvo.entity.*;
import com.zuvo.repo.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Turns database entities into safe JSON objects for the browser (never exposes password hashes). */
@Component
public class Views {
    public record UserView(Long id, String fullName, String profilePicUrl, String bio, boolean online) {}
    public record PostView(Long id, String type, String mediaUrl, String caption, String category, String visibility,
                           String createdAt, UserView author, long likeCount, long commentCount, boolean likedByMe,
                           boolean canInteract, boolean mine, boolean discover, boolean curated) {}
    public record CommentView(Long id, String body, String createdAt, UserView author) {}
    public record MessageView(Long id, Long senderId, Long receiverId, String content, String createdAt) {}

    private final UserRepository users;
    private final FollowRepository follows;
    private final PostLikeRepository likes;
    private final CommentRepository comments;
    private final PresenceService presence;

    public Views(UserRepository users, FollowRepository follows, PostLikeRepository likes,
                 CommentRepository comments, PresenceService presence) {
        this.users = users;
        this.follows = follows;
        this.likes = likes;
        this.comments = comments;
        this.presence = presence;
    }

    public String iso(LocalDateTime t) {
        return t == null ? null : t.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    public UserView user(User u) {
        return new UserView(u.id, u.fullName, u.profilePicUrl, u.bio, presence.isOnline(u.id));
    }

    public UserView user(Long id) {
        return users.findById(id).map(this::user)
                .orElse(new UserView(id, "Deleted user", null, null, false));
    }

    public boolean isFollowing(Long follower, Long following) {
        return follows.existsByFollowerIdAndFollowingIdAndStatus(follower, following, Follow.ACCEPTED);
    }

    /** Who can see a post: owner, curated/public posts, or an accepted follower. */
    public boolean canSee(Post p, Long viewer) {
        return p.userId.equals(viewer) || p.curated || "PUBLIC".equals(p.visibility) || isFollowing(viewer, p.userId);
    }

    /** Who can like/comment: owner, curated Discover posts, or an accepted follower. */
    public boolean canInteract(Post p, Long viewer) {
        return p.userId.equals(viewer) || p.curated || isFollowing(viewer, p.userId);
    }

    public PostView post(Post p, Long viewer, boolean discover) {
        return new PostView(p.id, p.type, p.mediaUrl, p.caption, p.category, p.visibility, iso(p.createdAt),
                user(p.userId), likes.countByPostId(p.id), comments.countByPostId(p.id),
                likes.existsByPostIdAndUserId(p.id, viewer), canInteract(p, viewer),
                p.userId.equals(viewer), discover, p.curated);
    }

    public CommentView comment(Comment c) {
        return new CommentView(c.id, c.body, iso(c.createdAt), user(c.userId));
    }

    public MessageView message(Message m) {
        return new MessageView(m.id, m.senderId, m.receiverId, m.content, iso(m.createdAt));
    }
}
