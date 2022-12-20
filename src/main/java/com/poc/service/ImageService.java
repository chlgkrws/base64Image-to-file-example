package com.poc.service;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Service
public class ImageService {


    /**
     * 1.Base64 Image url 형식 데이터를 파일로 변환하는 예제코드
     */
    public String convertText(String base64Image) throws IOException {
        if (StringUtils.isEmpty(base64Image)) {
            return base64Image;
        }

        String[] splitGroup = base64Image.split(",");

        // 데이터 추출
        String base64Scheme = splitGroup[0];
        String base64Data = splitGroup[1];

        // 확장자 구하기
        String ext = base64Scheme.substring("data:image/".length(), base64Scheme.indexOf(";base64"));

        // base64 to byte
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        String path = "path/" + "파일명." + ext;
        File imageFile = new File(path);                        // 변경하지 않으면 현재 프로젝트에 생성 됨
        // Files.createDirectories(상위폴더경로);                  // 상위폴더가 존재하는 경우 사용
        FileUtils.writeByteArrayToFile(imageFile, decodedBytes);

        return path;
    }

    /**
     * 2.Json 데이터 내에 존재하는 이미지를 추출하여 파일로 변환하는 예제코드는 ImageServiceTest 참조
     */

}
