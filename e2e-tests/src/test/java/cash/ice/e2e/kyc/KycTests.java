package cash.ice.e2e.kyc;

import cash.ice.RestHelper;
import cash.ice.Wrapper;
import cash.ice.e2e.LoginObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KycTests {
    private static final String CLIENT_ID = "kyc";
    private static final String CLIENT_SECRET = "Fyq5rfcuVtSxz3yquMZje0IOFqnQIRjH";
    private static final String USERNAME = "e2eTestUser";
    private static final String PASSWORD = "password";

    private final RestHelper rest = new RestHelper();

    private LoginObject managerLogin;
    private LoginObject userLogin;
    private Wrapper userWrapper;
    private Wrapper faceWrapper;
    private String faceId;

    @BeforeAll
    public void init() {
        System.out.println("  init()");
        managerLogin = login("manager", "manager111");
        System.out.println("  manager token: " + managerLogin);
        userWrapper = rest.sendPostRequest("/user", managerLogin.getAccessToken(), Map.of(
                "manager", false,
                "username", USERNAME,
                "password", PASSWORD,
                "firstName", USERNAME,
                "lastName", USERNAME,
                "email", "e2etestuser@ice.cash"
        ));
        System.out.println("  user register response: " + userWrapper);
        userLogin = login(USERNAME, PASSWORD);
        System.out.println("  user token: " + userLogin);

        faceWrapper = rest.sendPostMultipartRequest("/face/upload", userLogin.getAccessToken(), body -> {
            body.add("file", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
            body.add("entityId", 1);
        });
        System.out.println("  face upload response: " + faceWrapper);
        faceId = faceWrapper.getStr("faceId");
    }

    @AfterAll
    public void destroy() {
        System.out.println("  destroy()");
        if (faceId != null) {
            System.out.println("  deleting face: " + faceId);
            rest.delete("face", faceId, userLogin.getAccessToken());
        }
        if (userLogin != null) {
            logout(userLogin.getRefreshToken());
        }
        if (userWrapper != null) {
            System.out.println("  deleting user: " + userWrapper);
            rest.delete("user", userWrapper.getStr("userId"), managerLogin.getAccessToken());
        }
        if (managerLogin != null) {
            logout(managerLogin.getRefreshToken());
        }
    }

    @Test
    public void testUpload() {
        assertThat(userWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(userWrapper.getStr("userId")).isNotNull();
        assertThat(userWrapper.getStr("message")).isEqualTo("Registration processed successfully");
        assertThat(userWrapper.getStr("date")).isNotNull();

        assertThat(userLogin.getAccessToken()).isNotNull();
        assertThat(userLogin.getRefreshToken()).isNotNull();

        assertThat(faceWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(faceWrapper.getStr("faceId")).isNotNull();
        assertThat(faceWrapper.getStr("uploadTime")).isNotNull();
    }

    @Test
    public void testRefreshToken() {
        Wrapper refresh = rest.sendPostRequest("/login/token/refresh", Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "refreshToken", userLogin.getRefreshToken()
        ));
        System.out.println("  user refresh token: " + refresh);
        userLogin = new LoginObject(refresh.getStr("access_token"), refresh.getStr("refresh_token"));
        assertThat(userLogin.getAccessToken()).isNotNull();
        assertThat(userLogin.getRefreshToken()).isNotNull();
    }

    @Test
    public void testUploadAsync() {
        Wrapper uploadWrapper = rest.sendPostMultipartRequest("/face/upload/async", userLogin.getAccessToken(), body -> {
            body.add("file", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
            body.add("entityId", 1);
        });
        System.out.println("  face upload async: " + uploadWrapper);
        assertThat(uploadWrapper.getStr("status")).isEqualTo("PROCESSING");
        assertThat(uploadWrapper.getStr("faceId")).isNotNull();
        assertThat(uploadWrapper.getStr("uploadTime")).isNotNull();

        String faceId = uploadWrapper.getStr("faceId");
        try {
            Wrapper uploadStatus = null;
            for (int i = 0; i < 10; i++) {
                sleep(1000);
                uploadStatus = rest.sendGetRequest(String.format("/face/%s/status", faceId), null, userLogin.getAccessToken());
                System.out.printf("  %s) status of face upload async %s%n", i + 1, uploadStatus);
                if ("SUCCESS".equals(uploadStatus.getStr("status"))) {
                    break;
                }
            }
            assertThat(uploadStatus.getStr("status")).isEqualTo("SUCCESS");
            assertThat(uploadStatus.getStr("faceId")).isNotNull();
            assertThat(uploadStatus.getStr("uploadTime")).isNotNull();
        } finally {
            rest.delete("face", faceId, userLogin.getAccessToken());
        }
    }

    @Test
    public void testCompareFaceId() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest(String.format("/face/%s/compare", faceId), userLogin.getAccessToken(), body -> {
            body.add("file", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
        });
        System.out.println("  compare face response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareWrapper.toBool("photoMatch")).isTrue();
        assertThat(compareWrapper.getDbl("photoConfidence")).isEqualTo(100.0);
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();
    }

    @Test
    public void testCompareFaceIdAsync() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest(String.format("/face/%s/compare/async", faceId), userLogin.getAccessToken(), body -> {
            body.add("file", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
        });
        System.out.println("  compare face async response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("PROCESSING");
        assertThat(compareWrapper.getStr("compareId")).isNotNull();
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();

        String compareId = compareWrapper.getStr("compareId");
        Wrapper compareStatus = null;
        for (int i = 0; i < 10; i++) {
            sleep(1000);
            compareStatus = rest.sendGetRequest(String.format("/face/compare/%s/status", compareId), null, userLogin.getAccessToken());
            System.out.printf("  %s) status of compare face async %s", i + 1, compareStatus);
            if ("SUCCESS".equals(compareStatus.getStr("status"))) {
                break;
            }
        }
        assertThat(compareStatus.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareStatus.toBool("photoMatch")).isTrue();
        assertThat(compareStatus.getDbl("photoConfidence")).isEqualTo(100.0);
        assertThat(compareStatus.getStr("compareId")).isNotNull();
        assertThat(compareStatus.getStr("compareTime")).isNotNull();
    }

    @Test
    public void testCompareImages() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest("/face/images/compare", userLogin.getAccessToken(), body -> {
            body.add("file1", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
            body.add("file2", rest.getByteArrayResource("photo.jpeg", "SomePhoto2.png"));
        });
        System.out.println("  compare images response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareWrapper.toBool("photoMatch")).isTrue();
        assertThat(compareWrapper.getDbl("photoConfidence")).isEqualTo(100.0);
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();
    }

    @Test
    public void testCompareImagesAsync() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest("/face/images/compare/async", userLogin.getAccessToken(), body -> {
            body.add("file1", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
            body.add("file2", rest.getByteArrayResource("photo.jpeg", "SomePhoto2.png"));
        });
        System.out.println("  compare images async response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("PROCESSING");
        assertThat(compareWrapper.getStr("compareId")).isNotNull();
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();

        String compareId = compareWrapper.getStr("compareId");
        Wrapper compareStatus = null;
        for (int i = 0; i < 10; i++) {
            sleep(1000);
            compareStatus = rest.sendGetRequest(String.format("/face/compare/%s/status", compareId), null, userLogin.getAccessToken());
            System.out.printf("  %s) status of compare images async %s", i + 1, compareStatus);
            if ("SUCCESS".equals(compareStatus.getStr("status"))) {
                break;
            }
        }
        assertThat(compareStatus.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareStatus.toBool("photoMatch")).isTrue();
        assertThat(compareStatus.getDbl("photoConfidence")).isEqualTo(100.0);
        assertThat(compareStatus.getStr("compareId")).isNotNull();
        assertThat(compareStatus.getStr("compareTime")).isNotNull();
    }

    @Test
    public void testCompareVideo() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest(String.format("/face/%s/video/compare", faceId), userLogin.getAccessToken(), body -> {
            body.add("videoFile", rest.getByteArrayResource("video.mp4", "SomeVideo.mp4"));
        });
        System.out.println("  compare video response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareWrapper.toBool("videoMatch")).isTrue();
        assertThat(compareWrapper.getDbl("videoConfidence")).isGreaterThan(99.0);
        assertThat(compareWrapper.getInt("videoTimestamp")).isEqualTo(3455);
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();
    }

    @Test
    public void testCompareVideoAndPhotoAsync() {
        Wrapper compareWrapper = rest.sendPostMultipartRequest(String.format("/face/%s/video/compare/async", faceId), userLogin.getAccessToken(), body -> {
            body.add("videoFile", rest.getByteArrayResource("video.mp4", "SomeVideo.mp4"));
            body.add("imageFile", rest.getByteArrayResource("photo.jpeg", "SomePhoto.png"));
        });
        System.out.println("  compare video async response: " + compareWrapper);
        assertThat(compareWrapper.getStr("status")).isEqualTo("PROCESSING");
        assertThat(compareWrapper.getStr("compareId")).isNotNull();
        assertThat(compareWrapper.getStr("compareTime")).isNotNull();

        String compareId = compareWrapper.getStr("compareId");
        Wrapper compareStatus = null;
        for (int i = 0; i < 100; i++) {
            sleep(3000);
            compareStatus = rest.sendGetRequest(String.format("/face/compare/%s/status", compareId), null, userLogin.getAccessToken());
            System.out.printf("  %s) status of compare video async %s", i + 1, compareStatus);
            if ("SUCCESS".equals(compareStatus.getStr("status"))) {
                break;
            }
        }
        assertThat(compareStatus.getStr("status")).isEqualTo("SUCCESS");
        assertThat(compareStatus.toBool("videoMatch")).isTrue();
        assertThat(compareStatus.getDbl("videoConfidence")).isGreaterThan(99.0);
        assertThat(compareStatus.getInt("videoTimestamp")).isEqualTo(3455);
        assertThat(compareStatus.toBool("photoMatch")).isTrue();
        assertThat(compareStatus.getDbl("photoConfidence")).isEqualTo(100.0);
        assertThat(compareStatus.getStr("compareId")).isNotNull();
        assertThat(compareStatus.getStr("compareTime")).isNotNull();
    }

    private LoginObject login(String username, String password) {
        Wrapper resp = rest.sendPostRequest("/login", Map.of(
                "username", username,
                "password", password,
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET));
        return new LoginObject(resp.getStr("access_token"), resp.getStr("refresh_token"));
    }

    private void logout(String refreshToken) {
        rest.sendSimplePostRequest("/login/token/invalidate", Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "refreshToken", refreshToken
        ));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
