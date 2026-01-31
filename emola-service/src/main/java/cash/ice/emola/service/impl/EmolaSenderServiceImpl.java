package cash.ice.emola.service.impl;

import cash.ice.emola.config.EmolaProperties;
import cash.ice.emola.dto.EmolaRequest;
import cash.ice.emola.dto.EmolaResponse;
import cash.ice.emola.service.EmolaClient;
import cash.ice.emola.service.EmolaSenderService;
import com.viettel.bccsgw.webservice.GwOperation;
import com.viettel.bccsgw.webservice.GwOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmolaSenderServiceImpl implements EmolaSenderService {
    protected final EmolaClient emolaClient;
    private final EmolaProperties emolaProperties;

    @Override
    public EmolaResponse sendRequest(EmolaRequest emolaRequest) {

//        GwOperation request = new GwOperation();
//        request.setInput(new GwOperation.Input());
//        request.getInput().setUsername("user1");
//        request.getInput().setPassword("pass1");
//        request.getInput().setWscode("pushUssdMessage");
//        GwOperation.Input.Param param = new GwOperation.Input.Param();
//        param.setName("par1");
//        param.setValue("val1");
//        request.getInput().getParam().addAll(List.of(param));
//        GwOperationResponse response = sendRequest(request, emolaProperties.getPaymentUrl());
//        System.out.println("Response: " + response.getResult().getOriginal() + ", " + response);

        return null;
    }

    protected GwOperationResponse sendRequest(GwOperation request, String paymentUrl) {
        return emolaClient.sendPayment(request, paymentUrl);
    }

}
