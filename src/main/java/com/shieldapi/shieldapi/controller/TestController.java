package com.shieldapi.shieldapi.controller;

import com.shieldapi.shieldapi.dto.MessageResponse;
import com.shieldapi.shieldapi.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Test", description = "Protected test endpoint")
public class TestController {

    @Operation(
            summary = "Protected endpoint",
            description = "Returns a success message for authenticated users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authorized",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(hidden = true))
            )
    })
    @GetMapping("/test")
    public MessageResponse test(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        return new MessageResponse("Protected endpoint reached by " + username + " at " + Instant.now());
    }
}
