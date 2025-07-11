package be.nabu.eai.module.smtp;

import be.nabu.libs.types.api.MarshalRuleProvider;
import nabu.protocols.smtp.client.types.EmailAttachment;

public class SMTPMarshalRuleProvider implements MarshalRuleProvider {

	@Override
	public MarshalRule getMarshalRule(Class<?> clazz) {
		if (EmailAttachment.class.isAssignableFrom(clazz)) {
			return MarshalRule.NEVER;
		}
		return null;
	}

}
