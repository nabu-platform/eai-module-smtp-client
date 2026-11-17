package nabu.protocols.smtp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import be.nabu.libs.services.SimpleTransactionContext;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.ServiceAuthorizerProvider;
import be.nabu.libs.services.api.ServiceContext;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.api.ServiceRuntimeTrackerProvider;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.structure.Structure;

public class ServicesAccessTokenProviderTest {
	@Test
	public void invokesSelectedProviderWithItsOwnInputAndCallerContext() throws Exception {
		Structure providerInput = new Structure();
		for (String name : new String[] { "smtpClientId", "username", "from", "context" }) {
			providerInput.add(new SimpleElementImpl<String>(name, new be.nabu.libs.types.simple.String(), providerInput));
		}
		Structure outputType = new Structure();
		outputType.add(new SimpleElementImpl<String>("accessToken", new be.nabu.libs.types.simple.String(), outputType));
		ServiceInterface serviceInterface = (ServiceInterface) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] { ServiceInterface.class }, (proxy, method, args) -> {
				if ("getInputDefinition".equals(method.getName())) {
					return providerInput;
				}
				return "getOutputDefinition".equals(method.getName()) ? outputType : null;
			});
		ServiceContext serviceContext = (ServiceContext) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] { ServiceContext.class }, (proxy, method, args) -> {
				if ("getServiceAuthorizerProvider".equals(method.getName())) {
					return (ServiceAuthorizerProvider) runtime -> null;
				}
				return "getServiceTrackerProvider".equals(method.getName()) ? (ServiceRuntimeTrackerProvider) runtime -> null : null;
			});
		ExecutionContext caller = (ExecutionContext) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] { ExecutionContext.class }, (proxy, method, args) -> {
				if ("getServiceContext".equals(method.getName())) {
					return serviceContext;
				}
				return "getTransactionContext".equals(method.getName()) ? new SimpleTransactionContext() : null;
			});
		AtomicInteger invocations = new AtomicInteger();
		DefinedService provider = (DefinedService) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] { DefinedService.class }, (proxy, method, args) -> {
				if ("getId".equals(method.getName())) {
					return "test.token.provider";
				}
				if ("getServiceInterface".equals(method.getName())) {
					return serviceInterface;
				}
				if ("newInstance".equals(method.getName())) {
					return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ServiceInstance.class }, (instance, operation, parameters) -> {
						if (!"execute".equals(operation.getName())) {
							return null;
						}
						invocations.incrementAndGet();
						assertSame(caller, parameters[0]);
						ComplexContent input = (ComplexContent) parameters[1];
						assertSame(providerInput, input.getType());
						assertEquals("smtp.artifact", input.get("smtpClientId"));
						assertEquals("mail@example.com", input.get("username"));
						assertEquals("sender@example.com", input.get("from"));
						assertEquals("tenant-a", input.get("context"));
						ComplexContent output = outputType.newInstance();
						output.set("accessToken", "test-token");
						return output;
					});
				}
				return null;
			});
		assertEquals("test-token", Services.invokeAccessTokenProvider(provider, "smtp.artifact", "mail@example.com",
			"sender@example.com", "tenant-a", caller));
		assertEquals(1, invocations.get());
	}
}
