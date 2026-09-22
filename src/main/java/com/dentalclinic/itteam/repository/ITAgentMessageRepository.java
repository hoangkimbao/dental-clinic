package com.dentalclinic.itteam.repository;

import com.dentalclinic.itteam.model.ITAgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITAgentMessageRepository extends JpaRepository<ITAgentMessage, Long> {

    /**
     * Retrieve all top-level root messages (parentMessageId is null) in chronological order.
     * Used for the main channel chat feed.
     */
    List<ITAgentMessage> findByParentMessageIdIsNullOrderBySentAtAsc();

    /**
     * Retrieve all threaded replies for a specific parent message in chronological order.
     * Used for message thread view.
     */
    List<ITAgentMessage> findByParentMessageIdOrderBySentAtAsc(Long parentMessageId);

    /**
     * Check if a message has any child replies.
     */
    boolean existsByParentMessageId(Long parentMessageId);

    /**
     * Count child replies for a parent message.
     */
    long countByParentMessageId(Long parentMessageId);

    /**
     * Retrieve messages received by a specific agent or recipient ID.
     */
    List<ITAgentMessage> findByRecipientIdOrderBySentAtDesc(Long recipientId);

    /**
     * Retrieve messages received by a specific recipient hashtag (e.g. "#it-qa").
     */
    List<ITAgentMessage> findByRecipientHashtagOrderBySentAtDesc(String recipientHashtag);

    /**
     * Check if a message with specific recipient hashtag exists (used for idempotent seeding).
     */
    boolean existsByRecipientHashtag(String recipientHashtag);

    /**
     * Retrieve messages sent by a specific sender ID.
     */
    List<ITAgentMessage> findBySenderIdOrderBySentAtDesc(Long senderId);

    /**
     * Retrieve messages containing a specific hashtag in the parsed hashtags string.
     */
    List<ITAgentMessage> findByParsedHashtagsContainingIgnoreCaseOrderBySentAtDesc(String hashtag);

    /**
     * Find unread messages across the entire team (isRead = false).
     */
    List<ITAgentMessage> findByIsReadFalse();

    /**
     * JPQL alias for findByReadStatusFalse requested in requirements.
     */
    @Query("SELECT m FROM ITAgentMessage m WHERE m.isRead = false ORDER BY m.sentAt DESC")
    List<ITAgentMessage> findByReadStatusFalse();

    /**
     * Find unread messages targeted to a specific recipient ID.
     */
    List<ITAgentMessage> findByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * Find unread messages targeted to a specific recipient hashtag.
     */
    List<ITAgentMessage> findByRecipientHashtagAndIsReadFalse(String recipientHashtag);

    /**
     * Count total unread messages (for UI notification badges).
     */
    long countByIsReadFalse();

    /**
     * JPQL alias for countByReadStatusFalse.
     */
    @Query("SELECT COUNT(m) FROM ITAgentMessage m WHERE m.isRead = false")
    long countByReadStatusFalse();

    /**
     * Count unread messages for a specific recipient.
     */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * Retrieve all messages ordered chronologically (oldest to newest).
     */
    List<ITAgentMessage> findAllByOrderBySentAtAsc();

    /**
     * Retrieve all messages ordered newest first.
     */
    List<ITAgentMessage> findAllByOrderBySentAtDesc();
}
