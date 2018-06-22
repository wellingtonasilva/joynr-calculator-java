package br.com.wsilva.joynr.calculator;

import java.util.Properties;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;

import io.joynr.provider.Promise;
import io.joynr.runtime.AbstractJoynrApplication;
import io.joynr.runtime.CCInProcessRuntimeModule;
import io.joynr.runtime.JoynrApplication;
import io.joynr.runtime.JoynrApplicationModule;
import io.joynr.runtime.JoynrInjectorFactory;
import io.joynr.runtime.LibjoynrWebSocketRuntimeModule;
import joynr.io.joynr.CalculatorAbstractProvider;
import joynr.io.joynr.CalculatorProxy;
import io.joynr.arbitration.DiscoveryQos;
import io.joynr.arbitration.DiscoveryScope;
import io.joynr.messaging.AtmosphereMessagingModule;
import io.joynr.messaging.MessagingPropertyKeys;
import io.joynr.messaging.mqtt.paho.client.MqttPahoModule;
import io.joynr.messaging.websocket.WebsocketModule;
import joynr.types.ProviderQos;
import joynr.types.ProviderScope;

/**
 * Hello world!
 *
 */
public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);
	//private static final String LOCAL_DOMAIN = "joynr.domain.local";
	public static final String PROPERTY_JOYNR_DOMAIN_LOCAL = "joynr.domain.local";
	private static Semaphore executionSemaphore = new Semaphore(0);
	public static final String APP_CONFIG_PROVIDER_DOMAIN = "javademoapp.provider.domain";
    private static final String STATIC_PERSISTENCE_FILE = "consumer-joynr.properties";
    
    @Inject
    @Named(APP_CONFIG_PROVIDER_DOMAIN)
    private static String providerDomain;

	public static void main(String[] args) throws InterruptedException {
		DiscoveryScope tmpDiscoveryScope = DiscoveryScope.LOCAL_AND_GLOBAL;
	    String host = "localhost";
	    int port = 4242;
	    String providerDomain = "domain";
	    String transport = null;
	    
	    // joynr config properties are used to set joynr configuration at compile time. They are set on the
        // JoynInjectorFactory.
        Properties joynrConfig = new Properties();
        Module runtimeModule = getRuntimeModule(transport, host, port, joynrConfig);
        
        // Set a custom static persistence file (default is joynr.properties in the working dir) to store
        // joynr configuration. It allows for changing the joynr configuration at runtime. Custom persistence
        // files support running the consumer and provider applications from within the same directory.
        joynrConfig.setProperty(MessagingPropertyKeys.PERSISTENCE_FILE, STATIC_PERSISTENCE_FILE);

        // How to use custom infrastructure elements:

        // 1) Set them programmatically at compile time using the joynr configuration properties at the
        // JoynInjectorFactory. E.g. uncomment the following lines to set a certain joynr server
        // instance.
        // joynrConfig.setProperty(MessagingPropertyKeys.BOUNCE_PROXY_URL, "http://localhost:8080/bounceproxy/");
        // joynrConfig.setProperty(MessagingPropertyKeys.DISCOVERYDIRECTORYURL, "http://localhost:8080/discovery/channels/discoverydirectory_channelid/");
        joynrConfig.setProperty(PROPERTY_JOYNR_DOMAIN_LOCAL, "radioapp_consumer_local_domain");

        // Application-specific configuration properties are injected to the application by setting
        // them on the JoynApplicationModule.
        Properties appConfig = new Properties();
        appConfig.setProperty(APP_CONFIG_PROVIDER_DOMAIN, providerDomain);
        final DiscoveryScope discoveryScope = tmpDiscoveryScope;
        
        JoynrApplication joynrApplication = new JoynrInjectorFactory(joynrConfig, runtimeModule).createApplication(new JoynrApplicationModule(CalculatorApplication.class,
                appConfig) {
				@Override
				protected void configure() {
					super.configure();
					bind(DiscoveryScope.class).toInstance(discoveryScope);
				}
		});
        joynrApplication.run();
        executionSemaphore.acquire();
        joynrApplication.shutdown();

//        
//		JoynrInjectorFactory joynrInjectorFactory = new JoynrInjectorFactory(new CCInProcessRuntimeModule());
//        JoynrApplication helloWorldApplication = joynrInjectorFactory.createApplication(CalculatorApplication.class);
//        helloWorldApplication.run();
//
//        // Wait for the application to finish, shutdown and exit
//        executionSemaphore.acquire();
//        helloWorldApplication.shutdown();
//        logger.info("Done.");
	}

	static class CalculatorProviderImpl extends CalculatorAbstractProvider {
		@Override
		public Promise<CalcularSomaDeferred> calcularSoma(Integer a, Integer b) {
			CalcularSomaDeferred deferred = new CalcularSomaDeferred();
			deferred.resolve(a + b);
			return new Promise<>(deferred);
		}
	}

	static class CalculatorApplication extends AbstractJoynrApplication {
		private CalculatorProviderImpl provider = new CalculatorProviderImpl();

		@Override
		public void run() {
			// Register the provider
			ProviderQos providerQos = new ProviderQos();
			providerQos.setScope(ProviderScope.LOCAL);
			this.runtime.registerProvider(PROPERTY_JOYNR_DOMAIN_LOCAL, provider, providerQos);

			// Create a consumer proxy for the provider
			DiscoveryQos discoveryQos = new DiscoveryQos();
			discoveryQos.setDiscoveryScope(DiscoveryScope.LOCAL_AND_GLOBAL);
			CalculatorProxy proxy = runtime.getProxyBuilder(PROPERTY_JOYNR_DOMAIN_LOCAL, CalculatorProxy.class)
					.setDiscoveryQos(discoveryQos).build();

			// Print the result and exit
			Integer result = proxy.calcularSoma(10, 2);
			System.out.println("############### SERVER RESULT: " + result + " ###############");
			logger.info("RESULT: " + result);
			executionSemaphore.release();
		}
	}
	
	  private static Module getRuntimeModule(String transport, String host, int port, Properties joynrConfig) {
	        Module runtimeModule;
	        if (transport != null) {
	            if (transport.contains("websocket")) {
	                joynrConfig.setProperty(WebsocketModule.PROPERTY_WEBSOCKET_MESSAGING_HOST, host);
	                joynrConfig.setProperty(WebsocketModule.PROPERTY_WEBSOCKET_MESSAGING_PORT, "" + port);
	                joynrConfig.setProperty(WebsocketModule.PROPERTY_WEBSOCKET_MESSAGING_PROTOCOL, "ws");
	                joynrConfig.setProperty(WebsocketModule.PROPERTY_WEBSOCKET_MESSAGING_PATH, "");
	                runtimeModule = new LibjoynrWebSocketRuntimeModule();
	            } else {
	                runtimeModule = new CCInProcessRuntimeModule();
	            }

	            Module backendTransportModules = Modules.EMPTY_MODULE;
	            if (transport.contains("http")) {
	                backendTransportModules = Modules.combine(backendTransportModules, new AtmosphereMessagingModule());
	            }

	            if (transport.contains("mqtt")) {
	                joynrConfig.put("joynr.messaging.mqtt.brokerUri", "tcp://localhost:1883");
	                joynrConfig.put(MessagingPropertyKeys.PROPERTY_MESSAGING_PRIMARYGLOBALTRANSPORT, "mqtt");
	                backendTransportModules = Modules.combine(backendTransportModules, new MqttPahoModule());
	            }

	            return Modules.override(runtimeModule).with(backendTransportModules);
	        }

	        return Modules.override(new CCInProcessRuntimeModule()).with(new MqttPahoModule());
	    }
}
