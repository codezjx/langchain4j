package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.moderation.ModerationRequest;
import dev.ai4j.openai4j.moderation.ModerationResponse;
import dev.ai4j.openai4j.moderation.ModerationResult;
import dev.langchain4j.data.document.DocumentSegment;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.output.Result;
import lombok.Builder;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.model.input.structured.StructuredPromptProcessor.toPrompt;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class OpenAiModerationModel implements ModerationModel {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final OpenAiClient client;
    private final String modelName;

    @Builder
    public OpenAiModerationModel(String apiKey, String modelName, Duration timeout) {
        this.client = OpenAiClient.builder()
                .apiKey(apiKey)
                .callTimeout(timeout == null ? DEFAULT_TIMEOUT : timeout)
                .connectTimeout(timeout == null ? DEFAULT_TIMEOUT : timeout)
                .readTimeout(timeout == null ? DEFAULT_TIMEOUT : timeout)
                .writeTimeout(timeout == null ? DEFAULT_TIMEOUT : timeout)
                .build();
        this.modelName = modelName;
    }

    @Override
    public Result<Moderation> moderate(String text) {
        return moderateInternal(singletonList(text));
    }

    private Result<Moderation> moderateInternal(List<String> inputs) {

        ModerationRequest request = ModerationRequest.builder()
                .model(modelName)
                .input(inputs)
                .build();

        ModerationResponse response = client.moderation(request).execute();

        int i = 0;
        for (ModerationResult moderationResult : response.results()) {
            if (moderationResult.isFlagged()) {
                return Result.from(Moderation.flagged(inputs.get(i)));
            }
            i++;
        }

        return Result.from(Moderation.notFlagged());
    }

    @Override
    public Result<Moderation> moderate(Prompt prompt) {
        return moderate(prompt.text());
    }

    @Override
    public Result<Moderation> moderate(Object structuredPrompt) {
        return moderate(toPrompt(structuredPrompt));
    }

    @Override
    public Result<Moderation> moderate(ChatMessage message) {
        return moderate(message.text());
    }

    @Override
    public Result<Moderation> moderate(List<ChatMessage> messages) {
        List<String> inputs = messages.stream()
                .map(ChatMessage::text)
                .collect(toList());

        return moderateInternal(inputs);
    }

    @Override
    public Result<Moderation> moderate(DocumentSegment documentSegment) {
        return moderate(documentSegment.text());
    }
}