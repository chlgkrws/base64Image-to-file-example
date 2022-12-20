package com.poc.service;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ImageServiceTest {

    /**
     * 테스트 Json 데이터
     */
    private String json = "{\"ops\":[{\"insert\":\"\\n\"},{\"insert\":{\"image\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAgAAAAIACAIAAAB7GkOtAAAAAXNSR0IArs4c6QAHOqFJREFUeNrs4V2SbVuSHeaNNSLURMSN2QqOp0OjOqVk4GeMpSFMmsZ4RBMq60PpMiAgHK5Dot7QChiOJZmFTmnI8kxMQrElpKKc0I/EsLSmRJscgrtDGeQQkUqggOUgnr0zkEgkyiLPPQQOpFE5UABYpoaDszMBYwC4o4IjonDME5MSDQsFE4MichCMsSJDE5EgiEDENUJMSFmBKSgUIFASAFKFMgwAwk/RO98cvfvUXf/8NgZKZQ9LH9xf52Y9epGqxrqdpbIOnu16VSMLQGEkz9S2yGFU00Aiph1tweIfPH77qUVNwuYWFME0Qi4f2S+QhWthcqZwiHDeBE00kAB2cSJcQWUkOG0TkiRXWKCb2ivZI2L0fkDNoQt/CYWA2TafVAzIFI7M3H9EEUCouIzqNx3IVPApiTu5kAPHg+RnSYI9zaiJAICWuAkAnZziEEEkmsxJpoDobBk0/ACUGaZL==\"}},{\"insert\":{\"image\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAgAAAAIACAIAAAB7GkOtAAAAAXNSR0IArs4c6QAHOqFJREFUeNrs4V2SbVuSHeaNNSLURMSN2QqOp0OjOqVk4GeMpSFMmsZ4RBMq60PpMiAgHK5Dot7QChiOJZmFTmnI8kxMQrElpKKc0I/EsLSmRJscgrtDGeQQkUqggOUgnr0zkEgkyiLPPQQOpFE5UABYpoaDszMBYwC4o4IjonDME5MSDQsFE4MichCMsSJDE5EgiEDENUJMSFmBKSgUIFASAFKFMgwAwk/RO98cvfvUXf/8NgZKZQ9LH9xf52Y9epGqxrqdpbIOnu16VSMLQGEkz9S2yGFU00Aiph1tweIfPH77qUVNwuYWFME0Qi4f2S+QhWthcqZwiHDeBE00kAB2cSJcQWUkOG0TkiRXWKCb2ivZI2L0fkDNoQt\"}},{\"insert\":\"\\n\"}]}";

    /**
     * data:image/~~ 부분은 텍스트방식으로도 작성할 수 있기 때문에 prefix를 추가
     */
    private String prefix = "\\{\"insert\":\\{\"image\":\"";

    /**
     * 추출할 base64 이미지 데이터 정규식
     */
    private final String base64ImageRegex = "data:image\\/([a-zA-Z]*);base64,([^\\('|\")]*)";

    /**
     * 변환된 파일을 호출할 API(예제에서는 사용하지 않음)
     */
    private final String imageUri = "/system/tml/images/%s/%s";


    @Test
    void 이미지데이터추출_파일저장() {

        StringBuilder value = new StringBuilder(this.json);
        int prefixLength = getPrefixLength(this.prefix);

        try {
            int weightLength = 0;   // 파일경로로 replace 후 다음 image가 존재하는 index를 계산하기 위한 보정필드(패턴매칭 횟수를 Image 개수 별로 진행하지 않기위함)

            Pattern compile = Pattern.compile(this.prefix + this.base64ImageRegex);
            Matcher matcher = compile.matcher(this.json);

            // 식별된 base64 Image 마다 반복
            while (matcher.find()) {
                String base64ImageGroup = matcher.group();
                // 절대 위치(index)
                int perStartIndex = matcher.start();            // 패턴매칭 시점에서 식별한 이미지 위치
                int startIndex = perStartIndex - weightLength;  // 식별한 이미지 절대위치 (replace로 인해 실제 위치가 변경되기 때문에 위치를 보정함)

                // 데이터 처리 부분
                String subGroup = base64ImageGroup.substring(prefixLength);
                int subGroupLength = subGroup.length();
                String[] splitSubGroup = subGroup.split(",");
                String base64Scheme = splitSubGroup[0];         // 데이터 부분       data:image/~~~~~
                String base64Data = splitSubGroup[1];           // 이미지 부분       iVBORw0KGgoAAAANSUhEUgAA~~~~~~~

                try {
                    String ext = base64Scheme.substring("data:image/".length(), base64Scheme.indexOf(";base64"));       // 이미지 확장자 구하기
                    // 파일 path 구하기
                    String filePath = this.saveImage(base64Data, ext);

                    // 원본 변환하기
                    int start = startIndex + prefixLength;                          // startIndex는 prefix를 포함하기 때문에 위치 조정
                    value.replace(start, start + subGroupLength, filePath);    //

                    // 보정길이 수정 (base64 scheme길이 + 이미지 데이터 길이) - 파일경로 길이
                    weightLength += (subGroupLength) - filePath.length();
                } catch (Exception e) {
                    System.out.println("convertBase64ImageToFilePath :: "+e.toString());        // 실무에서는 log 사용 및 상세하게 예외처리
                }
            }
        } catch (Exception e) {
            System.out.println("convertBase64ImageToFilePath :: "+e.toString());
        }

        // 두 개의 이미지 중, 첫 번째 이미지 변환 시 예외(고의로 발생시킴)가 발생해도 두 번째 이미지 변환에 지장이 없는 것을 확인.
        System.out.println("value = " + value.toString());
    }


    private String saveImage(String base64Data, String ext) throws IOException {
        // base64 to byte
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        Random random = new Random();

        String path = "path/" + "파일명" +random.nextInt()+"." + ext;

        File imageFile = new File(path);                        // 변경하지 않으면 현재 프로젝트에 생성 됨
        // Files.createDirectories(상위폴더경로);                  // 상위폴더가 존재하는 경우 사용
        FileUtils.writeByteArrayToFile(imageFile, decodedBytes);

        return path;
    }


    private int getPrefixLength(String prefix) {
        int length = prefix.length();

        for (int i = 0; i < prefix.length(); i++) {
            if (prefix.charAt(i) == '\\') {
                length--;
            }
        }

        return length;
    }

}