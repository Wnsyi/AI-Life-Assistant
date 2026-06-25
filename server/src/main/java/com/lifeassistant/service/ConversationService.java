package com.lifeassistant.service;

import com.lifeassistant.model.Conversation;
import com.lifeassistant.model.Message;
import com.lifeassistant.repository.ConversationRepository;
import com.lifeassistant.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Conversation createConversation(Long userId, String title) {
        Conversation c = new Conversation();
        c.setUserId(userId);
        c.setTitle(title != null ? title : "新对话");
        c.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(c);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation c = conversationRepository.findById(conversationId).orElse(null);
        if (c == null || !c.getUserId().equals(userId)) {
            log.warn("无权删除或对话不存在: userId={}, convId={}", userId, conversationId);
            return;
        }
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(c);
        log.info("已删除对话: id={}", conversationId);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public Message saveMessage(Long conversationId, String role, String content) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(m);
    }

    /**
     * 把数据库中的消息历史拼成 Prompt 用的对话文本
     */
    public String buildContextFromMessages(Long conversationId) {
        List<Message> messages = getMessages(conversationId);
        if (messages.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("以下是与用户的历史对话：\n");
        for (Message m : messages) {
            if ("user".equals(m.getRole())) {
                sb.append("用户: ").append(m.getContent()).append("\n");
            } else if ("assistant".equals(m.getRole())) {
                sb.append("助手: ").append(m.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 更新对话标题
     */
    public Conversation updateTitle(Long userId, Long conversationId, String title) {
        Conversation c = conversationRepository.findById(conversationId).orElse(null);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改或对话不存在");
        }
        c.setTitle(title.length() > 50 ? title.substring(0, 50) : title);
        return conversationRepository.save(c);
    }

    /**
     * 如果是第一条用户消息，自动用消息内容做标题
     */
    public void autoTitleIfNew(Long conversationId, String firstMessage) {
        Conversation c = conversationRepository.findById(conversationId).orElse(null);
        if (c != null && isAutoTitle(c.getTitle())) {
            String title = firstMessage.length() > 30 ? firstMessage.substring(0, 30) : firstMessage;
            c.setTitle(title);
            conversationRepository.save(c);
        }
    }

    /** 判断标题是不是自动生成的（还没被用户改过） */
    private boolean isAutoTitle(String title) {
        if (title == null || title.isBlank()) return true;
        if (title.startsWith("新对话") || title.startsWith("新對話")) return true;
        // 数据库编码可能导致中文变乱码，也当作自动标题
        return title.length() <= 3 && title.contains("对") || title.contains("�");
    }
}
