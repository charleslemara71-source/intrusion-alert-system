package com.zuvo.web;

import com.zuvo.entity.Follow;
import com.zuvo.entity.Message;
import com.zuvo.repo.FollowRepository;
import com.zuvo.repo.MessageRepository;
import com.zuvo.service.ModerationService;
import com.zuvo.service.PresenceService;
import com.zuvo.service.Views;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class MessageController {
    public record SendRequest(String text) {}

    private final MessageRepository messages;
    private final FollowRepository follows;
    private final ModerationService moderation;
    private final PresenceService presence;
    private final Views views;

    public MessageController(MessageRepository messages, FollowRepository follows, ModerationService moderation,
                             PresenceService presence, Views views) {
        this.messages = messages;
        this.follows = follows;
        this.moderation = moderation;
        this.presence = presence;
        this.views = views;
    }

    /** People I can chat with (an accepted follow in either direction) plus people I already chatted with. */
    @GetMapping("/contacts")
    public List<Map<String, Object>> contacts(@RequestAttribute("uid") Long uid) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Follow f : follows.findAcceptedInvolving(uid)) ids.add(f.followerId.equals(uid) ? f.followingId : f.followerId);
        Map<Long, Message> last = new LinkedHashMap<>();
        for (Message m : messages.involving(uid)) last.putIfAbsent(m.senderId.equals(uid) ? m.receiverId : m.senderId, m);
        ids.addAll(last.keySet());

        List<Map<String, Object>> out = new ArrayList<>();
        for (Long id : ids) {
            Message m = last.get(id);
            out.add(J.of("user", views.user(id), "last", m == null ? null : views.message(m),
                    "unread", messages.countByReceiverIdAndSenderIdAndSeenFalse(uid, id)));
        }
        out.sort(Comparator.comparing((Map<String, Object> x) -> {
            Views.MessageView lm = (Views.MessageView) x.get("last");
            return lm == null ? "" : lm.createdAt();
        }).reversed());
        return out;
    }

    @GetMapping("/messages/{userId}")
    public List<Views.MessageView> thread(@RequestAttribute("uid") Long uid, @PathVariable Long userId) {
        List<Message> list = messages.thread(uid, userId);
        List<Message> unseen = list.stream().filter(m -> m.receiverId.equals(uid) && !m.seen).toList();
        unseen.forEach(m -> m.seen = true);
        if (!unseen.isEmpty()) messages.saveAll(unseen);
        return list.stream().map(views::message).toList();
    }

    @PostMapping("/messages/{userId}")
    public Views.MessageView send(@RequestAttribute("uid") Long uid, @PathVariable Long userId,
                                  @RequestBody SendRequest r) {
        if (uid.equals(userId)) throw ApiException.bad("You can't message yourself.");
        if (!views.isFollowing(uid, userId) && !views.isFollowing(userId, uid))
            throw ApiException.forbidden("You can message someone once a follow request between you is accepted.");
        String text = r.text() == null ? "" : r.text().trim();
        if (text.isEmpty()) throw ApiException.bad("Write a message first.");
        if (text.length() > 2000) throw ApiException.bad("Messages can be up to 2000 characters.");
        if (moderation.isTextFlagged(text)) throw ApiException.bad("This message was blocked by our AI safety check.");
        Message m = new Message();
        m.senderId = uid;
        m.receiverId = userId;
        m.content = text;
        m = messages.save(m);
        Views.MessageView v = views.message(m);
        presence.sendTo(userId, J.of("type", "message", "message", v, "from", views.user(uid)));
        return v;
    }
}
