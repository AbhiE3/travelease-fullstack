package com.travelease.backend.shared.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successBuildsEnvelopeWithDataAndMessage() {
        ApiResponse<String> response = ApiResponse.success("payload", "all good");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.message()).isEqualTo("all good");
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void errorBuildsEnvelopeWithCodeAndMessageAndEmptyDetails() {
        ApiResponse<Void> response = ApiResponse.error("SOME_CODE", "something went wrong");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("SOME_CODE");
        assertThat(response.error().message()).isEqualTo("something went wrong");
        assertThat(response.error().details()).isEmpty();
    }
}
