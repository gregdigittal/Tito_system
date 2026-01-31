package cash.ice.onemoney.config;

import cash.ice.onemoney.service.OnemoneyClient;
import cash.ice.onemoney.service.impl.WsOnemoneyClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.XsdSchemaCollection;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;

@Configuration
@EnableWs
@RequiredArgsConstructor
public class WebServiceConfig extends WsConfigurerAdapter {
    private final OnemoneyProperties onemoneyProperties;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPaths("com.huawei.cps.cpsinterface.api_requestmgr",
                "com.huawei.cps.cpsinterface.api_resultmgr",
                "com.huawei.cps.cpsinterface.common",
                "com.huawei.cps.cpsinterface.request",
                "com.huawei.cps.cpsinterface.response",
                "com.huawei.cps.cpsinterface.result",
                "com.huawei.cps.synccpsinterface.api_requestmgr",
                "com.huawei.cps.synccpsinterface.request",
                "com.huawei.cps.synccpsinterface.result");
        return marshaller;
    }

    @Bean
    public OnemoneyClient paymentClient(Jaxb2Marshaller marshaller) {
        WsOnemoneyClient client = new WsOnemoneyClient();
        client.setDefaultUri(onemoneyProperties.getPaymentUrl());
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet,
                onemoneyProperties.getResult().getLocationUri() + "/*");
    }

    @Bean(name = "onemoneyresult")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchemaCollection paymentsSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName(onemoneyProperties.getResult().getPortTypeName());
        wsdl11Definition.setLocationUri(onemoneyProperties.getResult().getLocationUri());
        wsdl11Definition.setTargetNamespace("http://cps.huawei.com/cpsinterface/api_resultmgr");
        wsdl11Definition.setSchemaCollection(paymentsSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchemaCollection paymentsSchema() {
        CommonsXsdSchemaCollection commonsXsdSchemaCollection = new CommonsXsdSchemaCollection(
                new ClassPathResource("xsd/result-api.xsd")
        );
        commonsXsdSchemaCollection.setInline(true);
        return commonsXsdSchemaCollection;
    }
}
