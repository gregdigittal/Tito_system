package cash.ice.fee.service.impl;

import cash.ice.fee.dto.group.GroupApiPaymentRequest;
import cash.ice.fee.service.GroupApiPaymentQueryService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GroupApiPaymentQueryServiceImpl implements GroupApiPaymentQueryService {
    private final static MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");

    private final Gson gson;

    @Value("${ice.cash.group-api}")
    private String groupApiUrl;

    @Override
    public String query(GroupApiPaymentRequest paymentRequest) throws IOException {
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(groupApiUrl)
                .method("POST", RequestBody.create(mediaType, getRequestString(paymentRequest)))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        Response response = client.newCall(request).execute();
        Objects.requireNonNull(response.body());
        return response.body().string();
    }

    private String getRequestString(GroupApiPaymentRequest paymentRequest) {
        return "MAC=8888888888888&Arguments=" + gson.toJson(paymentRequest);
    }

}
