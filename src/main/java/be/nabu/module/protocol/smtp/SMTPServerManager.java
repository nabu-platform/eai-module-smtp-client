package be.nabu.module.protocol.smtp;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class SMTPServerManager extends JAXBArtifactManager<SMTPServerConfiguration, SMTPServerArtifact> {

	public SMTPServerManager() {
		super(SMTPServerArtifact.class);
	}

	@Override
	protected SMTPServerArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new SMTPServerArtifact(id, container);
	}

}
