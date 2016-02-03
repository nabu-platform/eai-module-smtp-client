package be.nabu.eai.module.smtp;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class SMTPClientGUIManager extends BaseJAXBGUIManager<SMTPClientConfiguration, SMTPClientArtifact> {

	public SMTPClientGUIManager() {
		super("SMTP Client", SMTPClientArtifact.class, new SMTPClientManager(), SMTPClientConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected SMTPClientArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new SMTPClientArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Protocols";
	}
}
