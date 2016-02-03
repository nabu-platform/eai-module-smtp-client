package be.nabu.eai.module.smtp;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class SMTPClientManager extends JAXBArtifactManager<SMTPClientConfiguration, SMTPClientArtifact> {

	public SMTPClientManager() {
		super(SMTPClientArtifact.class);
	}

	@Override
	protected SMTPClientArtifact newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new SMTPClientArtifact(id, container, repository);
	}

}
