package com.poc.web;

import com.poc.service.ImageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class Base64ImageRestController {

    private final ImageService imageService;

    @PostMapping
    public String convert(@RequestBody Params params) throws IOException {
        String result = this.imageService.convertText(params.getBase64Image());

        log.info("result => {}", result);

        return "OK";
    }

    // 예제용 파라미터 클래스
    @Setter
    @Getter
    public static class Params {
        private String base64Image;
    }
}
