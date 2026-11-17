package nabu.protocols.smtp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.junit.Test;

import javax.jws.WebParam;

import be.nabu.eai.module.smtp.SMTPClientConfiguration;
import be.nabu.eai.module.smtp.api.SMTPAccessTokenProvider;

public class ServicesXoauth2Test {
	private static class Client extends AuthenticatingSMTPClient {
		private final List<String> commands = new ArrayList<String>();
		private int reply;

		Client(int reply) {
			this.reply = reply;
		}

		@Override
		public int sendCommand(String command) throws IOException {
			commands.add(command);
			return commands.size() == 1 ? reply : 535;
		}
	}

	@Test
	public void exposesOptionalNamedProviderContext() throws Exception {
		SMTPClientConfiguration configuration = new SMTPClientConfiguration();
		assertNull(configuration.getContext());
		configuration.setContext("tenant-a");
		assertEquals("tenant-a", configuration.getContext());

		Method method = SMTPAccessTokenProvider.class.getMethod("getAccessToken",
			String.class, String.class, String.class, String.class);
		Annotation[][] annotations = method.getParameterAnnotations();
		String[] names = { "smtpClientId", "username", "from", "context" };
		for (int i = 0; i < names.length; i++) {
			assertEquals(names[i], ((WebParam) annotations[i][0]).name());
		}
	}

	@Test
	public void extractsSingleEffectiveSender() {
		assertEquals("sender@example.com", Services.effectiveSender("sender@example.com"));
		assertEquals("sender@example.com", Services.effectiveSender("Sender Name <sender@example.com>"));
		assertEquals("sender@example.com", Services.effectiveSender("sender@example.com (Sender Name)"));
	}

	@Test
	public void rejectsInvalidAndMultipleSenders() {
		for (String from : new String[] { null, "", "not-an-address", "a;@example.com",
				"a@example.com, b@example.com", "Group: a@example.com;", "a@example.com;",
				"a@example.com\r\nBcc: evil@example.com", "Name <a@example.com>\nBcc: evil@example.com" }) {
			try {
				Services.effectiveSender(from);
				fail("Invalid sender accepted: " + from);
			}
			catch (IllegalArgumentException e) {
				assertEquals("XOAUTH2 requires a single valid sender address", e.getMessage());
			}
		}
	}

	@Test
	public void acceptsOnly235AndBuildsUtf8Payload() throws Exception {
		Client client = new Client(235);
		Services.authXoauth2(client, "usér@example.com", "raw-bearer");
		assertEquals(1, client.commands.size());
		String encoded = client.commands.get(0).substring("AUTH XOAUTH2 ".length());
		assertEquals("user=usér@example.com\u0001auth=Bearer raw-bearer\u0001\u0001",
			new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
	}

	@Test
	public void completesChallengeAndFailsWithoutFallback() throws Exception {
		Client client = new Client(334);
		try {
			Services.authXoauth2(client, "mail@example.com", "secret");
			fail("334 is not a successful authentication");
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("stage=auth, reply=334"));
			assertTrue(!e.getMessage().contains("secret"));
			assertNull(e.getCause());
		}
		assertEquals(2, client.commands.size());
		assertEquals("", client.commands.get(1));
	}

	@Test
	public void rejectsServerFailureWithoutSecondCommand() throws Exception {
		Client client = new Client(535);
		try {
			Services.authXoauth2(client, "mail@example.com", "secret");
			fail("535 is not a successful authentication");
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("stage=auth, reply=535"));
			assertTrue(!e.getMessage().contains("secret"));
			assertNull(e.getCause());
		}
		assertEquals(1, client.commands.size());
	}

	@Test
	public void reportsOnlySafeFailureFields() {
		String secret = "SENTINEL_PROVIDER_SECRET";
		IllegalStateException provider = Services.xoauth2Failure("token-provider", 0,
			new IllegalStateException(secret));
		assertEquals("SMTP XOAUTH2 send failed (stage=token-provider, category=operation error)", provider.getMessage());
		assertNull(provider.getCause());
		assertTrue(!provider.toString().contains(secret));

		IllegalStateException timeout = Services.xoauth2Failure("DATA completion (delivery uncertain)", 0,
			new SocketTimeoutException(secret));
		assertTrue(timeout.getMessage().contains("stage=DATA completion (delivery uncertain)"));
		assertTrue(timeout.getMessage().contains("category=timeout"));
		assertTrue(!timeout.toString().contains(secret));

		IllegalStateException rejected = Services.xoauth2Failure("recipients", 550, null);
		assertTrue(rejected.getMessage().contains("stage=recipients, reply=550, category=SMTP rejection"));
		assertTrue(!Services.xoauth2Failure("auth", 0, null).getMessage().contains("reply="));
	}

	@Test
	public void rejectsMissingOrInjectedUsernameBeforeAuth() throws Exception {
		for (String username : new String[] { null, "", " ", "mail@example.com\r\nAUTH PLAIN" }) {
			Client client = new Client(235);
			try {
				Services.authXoauth2(client, username, "secret");
				fail("Invalid username accepted");
			}
			catch (IllegalArgumentException e) {
				assertTrue(!e.getMessage().contains("secret"));
			}
			assertTrue(client.commands.isEmpty());
		}
	}

	@Test
	public void rejectsInvalidCredentialsBeforeAuth() throws Exception {
		for (String token : new String[] { null, "", "x\r\nAUTH PLAIN", " token " }) {
			Client client = new Client(235);
			try {
				Services.authXoauth2(client, "mail@example.com", token);
				fail("Invalid token accepted");
			}
			catch (IllegalArgumentException e) {
				assertTrue(!e.getMessage().contains("token"));
			}
			assertTrue(client.commands.isEmpty());
		}
	}
}
