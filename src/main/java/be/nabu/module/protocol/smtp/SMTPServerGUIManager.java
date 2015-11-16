package be.nabu.module.protocol.smtp;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class SMTPServerGUIManager extends BaseJAXBGUIManager<SMTPServerConfiguration, SMTPServerArtifact> {

	public SMTPServerGUIManager() {
		super("SMTP Server", SMTPServerArtifact.class, new SMTPServerManager(), SMTPServerConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected SMTPServerArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new SMTPServerArtifact(entry.getId(), entry.getContainer());
	}

	@Override
	public String getCategory() {
		return "Protocols";
	}
}
