package cash.ice.emola.config;

import cash.ice.emola.service.EmolaClient;
import cash.ice.emola.service.impl.WsEmolaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;

@Configuration
@EnableWs
@RequiredArgsConstructor
public class EmolaWebServiceConfig extends WsConfigurerAdapter {
    private final EmolaProperties emolaProperties;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPaths("com.viettel.bccsgw.webservice");
        return marshaller;
    }

    @Bean
    public EmolaClient paymentClient(Jaxb2Marshaller marshaller) {
        WsEmolaClient client = new WsEmolaClient();
        client.setDefaultUri(emolaProperties.getPaymentUrl());
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
