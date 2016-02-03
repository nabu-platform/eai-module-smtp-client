package be.nabu.eai.module.smtp;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class SMTPClientArtifact extends JAXBArtifact<SMTPClientConfiguration> {

	public SMTPClientArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "smtp-server.xml", SMTPClientConfiguration.class);
	}

}
