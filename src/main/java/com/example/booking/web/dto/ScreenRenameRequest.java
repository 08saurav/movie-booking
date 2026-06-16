package com.example.booking.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Update payload for a screen: only the name can be changed (see {@code Screen} for why). */
public record ScreenRenameRequest(@NotBlank @Size(max = 100) String name) {
}
