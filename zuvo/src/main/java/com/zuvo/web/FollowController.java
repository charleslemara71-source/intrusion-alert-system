package com.zuvo.web;

import com.zuvo.entity.Follow;
import com.zuvo.entity.User;
import com.zuvo.repo.FollowRepository;
import com.zuvo.repo.UserRepository;
import com.zuvo.service.PresenceService;
import com.zuvo.service.Views;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/follows")
public class FollowController {
    public record RespondRequest(boolean accept) {}

    private final FollowRepository follows;
    private final UserRepository users;
    private final PresenceService presence;
    private final Views views;

    public FollowController(FollowRepository follows, UserRepository users, PresenceService presence, Views views) {
        this.follows = follows;
        this.users = users;
        this.presence = presence;
        this.views = views;
    }

    /** Send a follow request to another user. */
    @PostMapping("/{userId}")
    public Map<String, Object> request(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        if (uid.equals(userId)) throw ApiException.bad("You can't follow yourself.");
        User target = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        if ("SYSTEM".equals(target.role)) throw ApiException.bad("This account can't be followed.");
        Follow f = follows.findByFollowerIdAndFollowingId(uid, userId).orElse(null);
        if (f != null) {
            if (Follow.ACCEPTED.equals(f.status)) throw ApiException.conflict("You already follow this person.");
            if (Follow.PENDING.equals(f.status)) throw ApiException.conflict("Your request is already waiting for a reply.");
            if (f.respondedAt != null && f.respondedAt.isAfter(LocalDateTime.now().minusHours(24)))
                throw ApiException.tooMany("You can ask again after 24 hours.");
        } else {
            f = new Follow();
            f.followerId = uid;
            f.followingId = userId;
        }
        f.status = Follow.PENDING;
        f.createdAt = LocalDateTime.now();
        f.respondedAt = null;
        f = follows.save(f);
        presence.sendTo(userId, J.of("type", "follow_request", "id", f.id, "from", views.user(uid)));
        return J.of("status", f.status);
    }

    /** Requests waiting for my answer. */
    @GetMapping("/requests")
    public List<Map<String, Object>> incoming(@RequestAttribute("uid") Long uid) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Follow f : follows.findByFollowingIdAndStatusOrderByCreatedAtDesc(uid, Follow.PENDING))
            out.add(J.of("id", f.id, "user", views.user(f.followerId), "createdAt", views.iso(f.createdAt)));
        return out;
    }

    /** Accept or decline a request that was sent to me. */
    @PostMapping("/{followId}/respond")
    public Map<String, Object> respond(@RequestAttribute("uid") Long uid, @PathVariable Long followId,
                                       @RequestBody RespondRequest r) {
        Follow f = follows.findById(followId).orElseThrow(() -> ApiException.notFound("Request not found."));
        if (!f.followingId.equals(uid)) throw ApiException.forbidden("This request is not for you.");
        if (!Follow.PENDING.equals(f.status)) throw ApiException.conflict("This request was already answered.");
        f.status = r.accept() ? Follow.ACCEPTED : Follow.DECLINED;
        f.respondedAt = LocalDateTime.now();
        follows.save(f);
        presence.sendTo(f.followerId, J.of("type", "follow_response", "accepted", r.accept(), "by", views.user(uid)));
        return J.of("status", f.status);
    }

    /** Unfollow, or cancel my pending request. */
    @DeleteMapping("/{userId}")
    public Map<String, Object> unfollow(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        follows.findByFollowerIdAndFollowingId(uid, userId).ifPresent(follows::delete);
        return J.of("ok", true);
    }

    /** Remove someone who follows me. */
    @DeleteMapping("/followers/{userId}")
    public Map<String, Object> removeFollower(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        follows.findByFollowerIdAndFollowingId(userId, uid).ifPresent(follows::delete);
        return J.of("ok", true);
    }
}
