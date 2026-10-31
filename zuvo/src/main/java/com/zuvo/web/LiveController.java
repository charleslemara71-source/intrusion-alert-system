package com.zuvo.web;

import com.zuvo.entity.LiveRequest;
import com.zuvo.entity.LiveStream;
import com.zuvo.entity.User;
import com.zuvo.repo.LiveRequestRepository;
import com.zuvo.repo.LiveStreamRepository;
import com.zuvo.repo.UserRepository;
import com.zuvo.service.LiveKitTokenService;
import com.zuvo.service.PresenceService;
import com.zuvo.service.Views;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** Live streams (host controls who may join) and 1-to-1 voice/video calls, both powered by LiveKit. */
@RestController
@RequestMapping("/api")
public class LiveController {
    public record StartRequest(String title) {}
    public record DecisionRequest(String decision) {}
    public record CallRequest(boolean video) {}

    private final LiveStreamRepository streams;
    private final LiveRequestRepository requests;
    private final UserRepository users;
    private final LiveKitTokenService livekit;
    private final PresenceService presence;
    private final Views views;

    public LiveController(LiveStreamRepository streams, LiveRequestRepository requests, UserRepository users,
                          LiveKitTokenService livekit, PresenceService presence, Views views) {
        this.streams = streams;
        this.requests = requests;
        this.users = users;
        this.livekit = livekit;
        this.presence = presence;
        this.views = views;
    }

    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private LiveStream live(Long id) {
        LiveStream s = streams.findById(id).orElseThrow(() -> ApiException.notFound("Live stream not found."));
        if (!"LIVE".equals(s.status)) throw ApiException.conflict("This live has ended.");
        return s;
    }

    private Map<String, Object> view(LiveStream s) {
        return J.of("id", s.id, "title", s.title, "room", s.roomName, "host", views.user(s.hostId),
                "startedAt", views.iso(s.startedAt));
    }

    // ------------------------------------------------------------------ live streaming

    @PostMapping("/live/start")
    public Map<String, Object> start(@RequestAttribute("uid") Long uid, @RequestBody StartRequest r) {
        if (!presence.isOnline(uid)) throw ApiException.forbidden("You're offline. Reconnect to go live.");
        User me = user(uid);
        LiveStream s = streams.findFirstByHostIdAndStatus(uid, "LIVE").orElse(null);
        if (s == null) {
            s = new LiveStream();
            s.hostId = uid;
            s.roomName = "zuvo-live-" + UUID.randomUUID().toString().substring(0, 8);
            String t = r.title() == null ? "" : r.title().trim();
            s.title = t.isEmpty() ? me.fullName + "'s live" : t.substring(0, Math.min(120, t.length()));
            s = streams.save(s);
            presence.broadcast(J.of("type", "live_started", "stream", view(s)));
        }
        return J.of("stream", view(s), "url", livekit.url(),
                "token", livekit.token(s.roomName, "u" + uid, me.fullName, true, true));
    }

    @GetMapping("/live")
    public List<Map<String, Object>> active() {
        return streams.findByStatusOrderByStartedAtDesc("LIVE").stream().map(this::view).toList();
    }

    /** Join as a viewer (or as a guest again if the host already accepted me). */
    @PostMapping("/live/{id}/watch")
    public Map<String, Object> watch(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        LiveStream s = live(id);
        User me = user(uid);
        boolean host = s.hostId.equals(uid);
        LiveRequest req = requests.findByStreamIdAndUserId(id, uid).orElse(null);
        boolean guest = req != null && "ACCEPTED".equals(req.status);
        return J.of("stream", view(s), "url", livekit.url(), "isHost", host,
                "myRequest", req == null ? null : req.status,
                "token", livekit.token(s.roomName, "u" + uid, me.fullName, host || guest, host));
    }

    @PostMapping("/live/{id}/request")
    public Map<String, Object> requestToJoin(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        LiveStream s = live(id);
        if (s.hostId.equals(uid)) throw ApiException.bad("You are the host.");
        LiveRequest req = requests.findByStreamIdAndUserId(id, uid).orElse(null);
        if (req != null && !"DECLINED".equals(req.status)) return J.of("status", req.status);
        if (req == null) {
            req = new LiveRequest();
            req.streamId = id;
            req.userId = uid;
        }
        req.status = "PENDING";
        req.createdAt = LocalDateTime.now();
        req = requests.save(req);
        presence.sendTo(s.hostId, J.of("type", "live_request", "streamId", id,
                "request", J.of("id", req.id, "status", req.status, "user", views.user(uid))));
        return J.of("status", req.status);
    }

    @GetMapping("/live/{id}/requests")
    public List<Map<String, Object>> listRequests(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        LiveStream s = live(id);
        if (!s.hostId.equals(uid)) throw ApiException.forbidden("Only the host can see join requests.");
        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveRequest r : requests.findByStreamIdOrderByCreatedAtAsc(id))
            out.add(J.of("id", r.id, "status", r.status, "user", views.user(r.userId)));
        return out;
    }

    /** The host accepts, declines, or leaves a request pending. */
    @PostMapping("/live/requests/{reqId}/decision")
    public Map<String, Object> decide(@RequestAttribute("uid") Long uid, @PathVariable Long reqId,
                                      @RequestBody DecisionRequest d) {
        LiveRequest r = requests.findById(reqId).orElseThrow(() -> ApiException.notFound("Request not found."));
        LiveStream s = live(r.streamId);
        if (!s.hostId.equals(uid)) throw ApiException.forbidden("Only the host can answer join requests.");
        String decision = d.decision() == null ? "" : d.decision().toUpperCase();
        r.status = switch (decision) {
            case "ACCEPT" -> "ACCEPTED";
            case "DECLINE" -> "DECLINED";
            case "PENDING" -> "PENDING";
            default -> throw ApiException.bad("Choose accept, decline or pending.");
        };
        requests.save(r);
        presence.sendTo(r.userId, J.of("type", "live_decision", "streamId", s.id, "decision", r.status));
        return J.of("id", r.id, "status", r.status);
    }

    /** After the host accepts me, I get a token that allows my camera and microphone. */
    @PostMapping("/live/{id}/publish-token")
    public Map<String, Object> publishToken(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        LiveStream s = live(id);
        User me = user(uid);
        boolean ok = s.hostId.equals(uid) || requests.findByStreamIdAndUserId(id, uid)
                .map(r -> "ACCEPTED".equals(r.status)).orElse(false);
        if (!ok) throw ApiException.forbidden("The host has not accepted your request.");
        return J.of("url", livekit.url(), "token", livekit.token(s.roomName, "u" + uid, me.fullName, true, s.hostId.equals(uid)));
    }

    @PostMapping("/live/{id}/end")
    public Map<String, Object> end(@RequestAttribute("uid") Long uid, @PathVariable Long id) {
        LiveStream s = live(id);
        if (!s.hostId.equals(uid)) throw ApiException.forbidden("Only the host can end the live.");
        s.status = "ENDED";
        s.endedAt = LocalDateTime.now();
        streams.save(s);
        presence.broadcast(J.of("type", "live_ended", "streamId", id));
        return J.of("ok", true);
    }

    // ------------------------------------------------------------------ 1-to-1 calls

    private String callRoom(Long a, Long b) {
        return "zuvo-call-" + Math.min(a, b) + "-" + Math.max(a, b);
    }

    private void checkContact(Long a, Long b) {
        if (a.equals(b)) throw ApiException.bad("You can't call yourself.");
        if (!views.isFollowing(a, b) && !views.isFollowing(b, a))
            throw ApiException.forbidden("You can call someone once a follow request between you is accepted.");
    }

    @PostMapping("/calls/{userId}/start")
    public Map<String, Object> startCall(@RequestAttribute("uid") Long uid, @PathVariable Long userId,
                                         @RequestBody CallRequest r) {
        checkContact(uid, userId);
        if (!presence.isOnline(uid)) throw ApiException.forbidden("You're offline.");
        if (!presence.isOnline(userId)) throw ApiException.conflict("This person is offline right now.");
        User me = user(uid);
        presence.sendTo(userId, J.of("type", "incoming_call", "from", views.user(me), "video", r.video()));
        return J.of("url", livekit.url(), "video", r.video(),
                "token", livekit.token(callRoom(uid, userId), "u" + uid, me.fullName, true, false));
    }

    @PostMapping("/calls/{userId}/join")
    public Map<String, Object> joinCall(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        checkContact(uid, userId);
        User me = user(uid);
        return J.of("url", livekit.url(),
                "token", livekit.token(callRoom(uid, userId), "u" + uid, me.fullName, true, false));
    }

    @PostMapping("/calls/{userId}/decline")
    public Map<String, Object> decline(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        presence.sendTo(userId, J.of("type", "call_declined", "by", views.user(uid)));
        return J.of("ok", true);
    }

    @PostMapping("/calls/{userId}/end")
    public Map<String, Object> endCall(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        presence.sendTo(userId, J.of("type", "call_ended", "by", views.user(uid)));
        return J.of("ok", true);
    }
}
