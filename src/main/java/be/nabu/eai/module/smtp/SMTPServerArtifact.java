package be.nabu.eai.module.smtp;

import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ResourceContainer;

public class SMTPServerArtifact extends JAXBArtifact<SMTPServerConfiguration> {

	public SMTPServerArtifact(String id, ResourceContainer<?> directory) {
		super(id, directory, "smtp-server.xml", SMTPServerConfiguration.class);
	}

}
