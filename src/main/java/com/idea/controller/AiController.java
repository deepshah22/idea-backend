package com.idea.controller;

import com.idea.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI endpoints.
 *
 * POST /ai/chat   — ask a question, get a grounded LLM response
 * POST /ai/ingest — add a document to the vector store (admin only)
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI")
public class AiController {

    private final AiChatService aiChatService;

    record ChatRequest(
        @NotBlank @Size(max = 1000) String question
    ) {}

    record ChatResponse(
        String answer,
        String question
    ) {}

    record IngestRequest(
        @NotBlank String id,
        @NotBlank String content,
        Map<String, Object> metadata
    ) {}

    @PostMapping("/chat")
    @Operation(summary = "Ask a question — grounded in your vector store data")
    public ResponseEntity<ChatResponse> chat(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ChatRequest req
    ) {
        // use the authenticated user's email as the cache namespace
        String answer = aiChatService.chat(req.question(), principal.getUsername());
        return ResponseEntity.ok(new ChatResponse(answer, req.question()));
    }

    @PostMapping("/ingest")
    @Operation(summary = "Add a document to the vector store (admin)")
    public ResponseEntity<Void> ingest(
        @Valid @RequestBody IngestRequest req
    ) {
        aiChatService.ingest(req.id(), req.content(), req.metadata());
        return ResponseEntity.ok().build();
    }
}
