package cash.ice.emola.service.impl;

import cash.ice.common.constant.EntityMetaKey;
import cash.ice.common.constant.IceCashProfile;
import cash.ice.common.dto.fee.FeesData;
import cash.ice.common.service.KafkaSender;
import cash.ice.emola.config.EmolaProperties;
import cash.ice.emola.dto.EmolaRequest;
import cash.ice.emola.dto.EmolaResponse;
import cash.ice.emola.entity.EmolaPayment;
import cash.ice.emola.repository.EmolaPaymentRepository;
import cash.ice.emola.service.EmolaSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(IceCashProfile.NOT_PROD)
@Slf4j
public class EmolaPaymentServiceUatImpl extends EmolaPaymentServiceImpl {

    public EmolaPaymentServiceUatImpl(EmolaSenderService emolaSenderService, KafkaSender kafkaSender, EmolaPaymentRepository emolaPaymentRepository, EmolaProperties emolaProperties) {
        super(emolaSenderService, kafkaSender, emolaPaymentRepository, emolaProperties);
    }

    @Override
    protected EmolaResponse sendRequest(FeesData feesData, EmolaPayment emolaPayment, EmolaRequest request) {
        if (feesData != null && feesData.getPaymentRequest().getMetaData() != null && feesData.getPaymentRequest().getMetaData().containsKey(EntityMetaKey.Simulate)) {
            return simulateResponse((String) feesData.getPaymentRequest().getMetaData().get(EntityMetaKey.Simulate), request);
        } else {
            return super.sendRequest(feesData, emolaPayment, request);
        }
    }

    private EmolaResponse simulateResponse(String simulateValue, EmolaRequest request) {
        log.debug("  simulating {} response", simulateValue);
        if ("SUCCESS".equals(simulateValue)) {
            return new EmolaResponse().setErrorCode("0").setGwtransid("20210324093455711185_(simulated)")
                    .setOriginalResponseCode(request.getWscode() + "Response").setOriginalErrorCode("0")
                    .setOriginalMessage("queryBeneficiaryName".equals(request.getWscode()) ? "John Doe (simulated)" : "Successfully")
                    .setOriginalRequestId("00204020210425143336")
                    .setOriginalBalance("queryAccountBalance".equals(request.getWscode()) ? "1000" : null)
                    .setOriginalOrgResponseCode("pushUssdQueryTrans".equals(request.getWscode()) ? "01" : null)
                    .setOriginalOrgResponseMessage("pushUssdQueryTrans".equals(request.getWscode()) ? "01| Transaction successful |" : null);
        } else {
            return new EmolaResponse().setErrorCode("1").setDescription("Some error (simulated)")
                    .setOriginalErrorCode("1").setOriginalMessage("Original error message (simulated)");
        }
    }
}
