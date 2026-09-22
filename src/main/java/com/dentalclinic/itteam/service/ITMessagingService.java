package com.dentalclinic.itteam.service;

import com.dentalclinic.itteam.dto.DispatchMessageRequest;
import com.dentalclinic.itteam.model.ITAgentActivity;
import com.dentalclinic.itteam.model.ITAgentMessage;
import com.dentalclinic.itteam.model.ITAgentProfile;
import com.dentalclinic.itteam.repository.ITAgentActivityRepository;
import com.dentalclinic.itteam.repository.ITAgentMessageRepository;
import com.dentalclinic.itteam.repository.ITAgentProfileRepository;
import com.dentalclinic.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service implementing the Inter-Agent Messaging & Hashtag Engine.
 * Extracts mentions (#it-*), routes messages to recipient agents,
 * triggers MENTIONED activity audit logs, and handles threaded replies.
 */
@Service
public class ITMessagingService {

    private static final Logger log = LoggerFactory.getLogger(ITMessagingService.class);

    // Hashtag parser regex: matches #it-backend, #it-frontend, #it-qa, #it-devops, #it-security case-insensitively
    private static final Pattern HASHTAG_PATTERN = Pattern.compile(
            "(?i)#it-(backend|frontend|qa|devops|security)\\b"
    );

    private final ITAgentMessageRepository messageRepository;
    private final ITAgentProfileRepository profileRepository;
    private final ITAgentActivityRepository activityRepository;
    private final NineRouterAiClient nineRouterAiClient;

    public ITMessagingService(ITAgentMessageRepository messageRepository,
                              ITAgentProfileRepository profileRepository,
                              ITAgentActivityRepository activityRepository,
                              NineRouterAiClient nineRouterAiClient) {
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.activityRepository = activityRepository;
        this.nineRouterAiClient = nineRouterAiClient;
    }

    /**
     * Extracts valid IT agent hashtags from raw message content.
     * Deduplicates multiple occurrences and handles surrounding punctuation.
     *
     * @param content Message text
     * @return Set of unique lowercase hashtags (e.g. ["#it-backend", "#it-qa"])
     */
    public Set<String> extractHashtags(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }

        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return tags;
    }

    /**
     * Dispatches an inter-agent message, parses hashtags, links recipients,
     * logs MENTIONED activities for all tagged agents, and triggers AI response when applicable.
     */
    @Transactional
    public ITAgentMessage dispatchMessage(DispatchMessageRequest request, User senderUser) {
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("Message body cannot be empty or whitespace");
        }

        String rawBody = request.getMessageBody().trim();
        Set<String> hashtags = extractHashtags(rawBody);
        String parsedHashtags = String.join(",", hashtags);

        // Sender details
        Long senderId = senderUser != null ? senderUser.getId() : 1L;
        String senderName = senderUser != null
                ? (senderUser.getFullName() != null ? senderUser.getFullName() : senderUser.getUsername())
                : "Owner / Administrator";
        String senderType = (senderUser != null && senderUser.getRole() != null)
                ? senderUser.getRole().name()
                : "ROLE_ADMIN";

        // Recipient resolution: First matched hashtag agent profile
        Long recipientId = null;
        String recipientHashtag = request.getRecipientHashtag();

        ITAgentProfile primaryRecipientAgent = null;
        if (!hashtags.isEmpty()) {
            String firstTag = hashtags.iterator().next();
            Optional<ITAgentProfile> profileOpt = profileRepository.findByHashtag(firstTag);
            if (profileOpt.isPresent()) {
                primaryRecipientAgent = profileOpt.get();
                recipientId = primaryRecipientAgent.getId();
                recipientHashtag = primaryRecipientAgent.getHashtag();
            }
        }

        if (recipientHashtag == null || recipientHashtag.isBlank()) {
            recipientHashtag = "BROADCAST";
        }

        // Create and save the message
        ITAgentMessage message = new ITAgentMessage(
                senderId,
                senderName,
                senderType,
                recipientId,
                recipientHashtag,
                rawBody,
                parsedHashtags,
                request.getParentMessageId()
        );

        ITAgentMessage savedMessage = messageRepository.save(message);

        // Generate MENTIONED activity records for EACH uniquely tagged agent
        for (String tag : hashtags) {
            profileRepository.findByHashtag(tag).ifPresent(profile -> {
                ITAgentActivity activity = new ITAgentActivity(
                        profile.getId(),
                        profile.getAgentCode(),
                        "MENTIONED",
                        rawBody,
                        "Mentioned by " + senderName + " in message #" + savedMessage.getId(),
                        "message:" + savedMessage.getId()
                );
                activityRepository.save(activity);
                log.info("📢 Triggered MENTIONED activity for agent {} ({})", profile.getDisplayName(), tag);
            });
        }

        // If top-level message addressed to an agent, generate AI persona reply in thread
        if (primaryRecipientAgent != null && request.getParentMessageId() == null) {
            try {
                String aiReplyContent = nineRouterAiClient.getAgentResponse(primaryRecipientAgent, rawBody);
                if (aiReplyContent != null && !aiReplyContent.isBlank()) {
                    ITAgentMessage aiReplyMessage = new ITAgentMessage(
                            primaryRecipientAgent.getId(),
                            primaryRecipientAgent.getDisplayName(),
                            "AGENT",
                            senderId,
                            primaryRecipientAgent.getHashtag(),
                            aiReplyContent,
                            primaryRecipientAgent.getHashtag(),
                            savedMessage.getId()
                    );
                    messageRepository.save(aiReplyMessage);
                }
            } catch (Exception ex) {
                log.warn("Failed to generate threaded AI reply: {}", ex.getMessage());
            }
        }

        return savedMessage;
    }

    /**
     * Retrieve messages, supporting threaded conversation retrieval (by parentMessageId),
     * hashtag filtering, or the main top-level feed.
     */
    @Transactional(readOnly = true)
    public List<ITAgentMessage> getMessages(Long parentMessageId, String hashtag) {
        if (parentMessageId != null) {
            return messageRepository.findByParentMessageIdOrderBySentAtAsc(parentMessageId);
        }
        if (hashtag != null && !hashtag.isBlank()) {
            return messageRepository.findByParsedHashtagsContainingIgnoreCaseOrderBySentAtDesc(hashtag);
        }
        return messageRepository.findByParentMessageIdIsNullOrderBySentAtAsc();
    }
}
