package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.ChatMessage;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.services.ChatService;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    private final ChatService chatService;
    private final UserService userService;

    // отдельный чат по продукту
    @GetMapping("/chat/{userId}")
    public String chat(@PathVariable Long userId, Model model, Principal principal) {
        User me = userService.getUserByPrincipal(principal);
        User other = userService.getUserByID(userId);
        if (other == null || other.getId().equals(me.getId())) {
            return "redirect:/orders";
        }

        chatService.markRead(me, other);

        model.addAttribute("currentUser", me);
        model.addAttribute("otherUser", other);
        model.addAttribute("otherOnline", chatService.isOnline(other));
        model.addAttribute("messages", chatService.getConversation(me, other));
        return "chat";
    }

    // отправка сообщения
    @PostMapping("/chat/{userId}/send")
    @ResponseBody
    public ResponseEntity<String> send(@PathVariable Long userId, @RequestParam String text, Principal principal) {
        User me = userService.getUserByPrincipal(principal);
        User other = userService.getUserByID(userId);
        if (other == null || other.getId().equals(me.getId()) || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ChatMessage saved = chatService.sendMessage(me, other, text);
        return html(renderBubble(saved, me.getId()));
    }

    // опрос новых сообщений раз в несколько секунд
    @GetMapping("/chat/{userId}/poll")
    @ResponseBody
    public ResponseEntity<String> poll(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "0") Long afterId,
                                        Principal principal) {
        User me = userService.getUserByPrincipal(principal);
        User other = userService.getUserByID(userId);
        if (other == null || other.getId().equals(me.getId())) {
            return ResponseEntity.badRequest().build();
        }
        chatService.markRead(me, other);

        List<ChatMessage> newMessages = chatService.getConversationAfter(me, other, afterId);
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : newMessages) {
            sb.append(renderBubble(m, me.getId()));
        }
        return html(sb.toString());
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    private String renderBubble(ChatMessage m, Long myId) {
        boolean mine = m.getSender().getId().equals(myId);
        String cls = mine ? "chat-bubble chat-bubble--mine" : "chat-bubble chat-bubble--theirs";
        return "<div class=\"" + cls + "\" data-id=\"" + m.getId() + "\">"
                + "<div class=\"chat-bubble__text\">" + HtmlUtils.htmlEscape(m.getText()) + "</div>"
                + "<div class=\"chat-bubble__time mono\">" + m.getSentAt().format(TIME_FMT) + "</div>"
                + "</div>";
    }
}
