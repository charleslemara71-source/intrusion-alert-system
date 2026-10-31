package com.zuvo.repo;

import com.zuvo.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m where (m.senderId = :a and m.receiverId = :b) or (m.senderId = :b and m.receiverId = :a) order by m.createdAt asc")
    List<Message> thread(@Param("a") Long a, @Param("b") Long b);

    @Query("select m from Message m where m.senderId = :u or m.receiverId = :u order by m.createdAt desc")
    List<Message> involving(@Param("u") Long userId);

    long countByReceiverIdAndSenderIdAndSeenFalse(Long receiverId, Long senderId);
}
