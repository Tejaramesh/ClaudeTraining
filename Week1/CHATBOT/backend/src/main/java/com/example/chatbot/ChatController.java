package com.example.chatbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
		fullMessages.add(new SystemMessage("Answer the question directly and concisely."));
		fullMessages.addAll(messages);

		String rawReply = chatClient.prompt().messages(fullMessages).call().content();
		String cleanReply = stripLeadingReasoning(rawReply);

		messages.add(new AssistantMessage(cleanReply));

		return new ChatResponse(cleanReply);
	}

	private static final Pattern REASONING_OPENER = Pattern.compile(
		"^(the\\s+(user|question|text|instructions?|prompt|system)\\b).*", Pattern.CASE_INSENSITIVE);

	private String stripLeadingReasoning(String rawReply) {
		String trimmed = rawReply.trim();
		String[] sentences = trimmed.split("(?<=[.!?])\\s+");

		int start = 0;
		while (start < sentences.length && REASONING_OPENER.matcher(sentences[start].trim()).matches()) {
			start++;
		}

		if (start == 0 || start >= sentences.length) {
			return trimmed;
		}

		return String.join(" ", Arrays.asList(sentences).subList(start, sentences.length)).trim();
	}

	record ChatRequest(String conversationId, String message) {
	}

	record ChatResponse(String reply) {
	}

}
