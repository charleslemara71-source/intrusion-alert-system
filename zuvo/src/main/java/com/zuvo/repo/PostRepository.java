package com.zuvo.repo;

import com.zuvo.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdInOrderByCreatedAtDesc(Collection<Long> userIds, Pageable pageable);
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);

    @Query("select p from Post p where (p.curated = true or p.visibility = 'PUBLIC') and p.userId not in :ids order by p.createdAt desc")
    List<Post> findDiscover(@Param("ids") Collection<Long> excludeUserIds, Pageable pageable);

    @Query("select p from Post p where p.type in ('REEL','VIDEO') and (p.curated = true or p.visibility = 'PUBLIC' or p.userId in :ids) order by p.createdAt desc")
    List<Post> findReels(@Param("ids") Collection<Long> visibleUserIds, Pageable pageable);
}
