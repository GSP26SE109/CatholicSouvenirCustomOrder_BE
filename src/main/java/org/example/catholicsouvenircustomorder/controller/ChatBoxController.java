package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.service.ChatBoxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatBoxController {
    private final ChatBoxService chatboxService;

    @PostMapping
    public ResponseEntity<BaseResponse> chat(@RequestBody String request) {

        String reply = chatboxService.askAI(request);

        return ResponseEntity.ok(BaseResponse.success("Trả lời thành công thành công", reply));
    }
}
