package org.example.catholicsouvenircustomorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse {
    private int code;
    private String message;
    private Object data;

    public static BaseResponse success(String message, Object data) {
        return BaseResponse.builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static BaseResponse success(String message) {
        return BaseResponse.builder()
                .code(200)
                .message(message)
                .data(null)
                .build();
    }

    public static BaseResponse error(int code, String message) {
        return BaseResponse.builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
