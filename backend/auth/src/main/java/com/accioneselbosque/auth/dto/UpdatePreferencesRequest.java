package com.accioneselbosque.auth.dto;

import com.accioneselbosque.auth.model.Language;
import com.accioneselbosque.auth.model.NotifChannel;

public record UpdatePreferencesRequest(
        NotifChannel notifChannel,
        Language language
) {}
