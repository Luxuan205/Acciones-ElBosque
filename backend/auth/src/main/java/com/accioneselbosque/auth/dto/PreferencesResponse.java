package com.accioneselbosque.auth.dto;

import com.accioneselbosque.auth.model.Language;
import com.accioneselbosque.auth.model.NotifChannel;
import java.time.LocalDateTime;

public record PreferencesResponse(
        NotifChannel notifChannel,
        Language language,
        LocalDateTime updatedAt
) {}
