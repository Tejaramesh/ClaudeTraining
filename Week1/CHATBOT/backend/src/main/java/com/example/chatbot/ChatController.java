package com.example.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

	private final ChatClient chatClient;
	private final ConcurrentHashMap<String, List<Message>> conversationHistory;

	public ChatController(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.conversationHistory = new ConcurrentHashMap<>();
	}

	@PostMapping("/chat")
	public ChatResponse chat(@RequestBody ChatRequest request) {
		conversationHistory.computeIfAbsent(request.conversationId(), k -> new ArrayList<>());
		List<Message> messages = conversationHistory.get(request.conversationId());

		messages.add(new UserMessage(request.message()));

		List<Message> fullMessages = new ArrayList<>();
		fullMessages.add(new SystemMessage(
			"You are a helpful assistant. Reply in 1-3 sentences only. Never show your planning, reasoning, options, steps, or framework. Just answer the question directly and naturally."));
		fullMessages.addAll(messages);

		String reply = chatClient.prompt().messages(fullMessages).call().content();
		reply = stripAllPlanning(reply);

		messages.add(new AssistantMessage(reply));

		return new ChatResponse(reply);
	}

	private String stripAllPlanning(String text) {
		// Remove meta-labels and framework
		String cleaned = text
			.replaceAll("(?i)Question:\\s*", "")
			.replaceAll("(?i)Answer:\\s*", "")
			// Remove planning sections
			.replaceAll("(?mi)(Plan|Option|Step|Note|Reason|Think|Consider)\\s*:.*", "")
			// Remove bullet-list planning (2+ consecutive)
			.replaceAll("(?m)(^[•*\\-]\\s+[^\\n]*\\n?){2,}", "")
			// Remove meta-text opening lines
			.replaceAll("(?i)^(The user|This is|Here's?|Let me).*?\\.\\s*", "")
			.trim();

		// Split into sentences and take first 3
		String[] sentences = cleaned.split("(?<=[.!?])\\s+");
		StringBuilder result = new StringBuilder();
		int count = 0;

		for (String sent : sentences) {
			String trimmed = sent.trim();

			// Skip meta/planning sentences
			if (trimmed.matches("(?i).*(Plan|Option|Step|Note|Reason).*") ||
				trimmed.matches("(?i).*(Confidence|Mental|Sandbox).*") ||
				trimmed.matches("^[•*\\-].*") ||
				trimmed.length() < 5) {
				continue;
			}

			result.append(trimmed).append(" ");
			count++;
			if (count >= 3) break;
		}

		String answer = result.toString().trim();
		return answer.isEmpty() ? "I'm here to help!" : answer;
	}

	record ChatRequest(String conversationId, String message) {
	}

	record ChatResponse(String reply) {
	}

}
