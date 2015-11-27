package nabu.utils.protocols;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.utils.mime.api.Header;

@XmlRootElement(name = "attachment")
public class Attachment {
	private String name, contentType;
	private InputStream content;
	private Boolean inline;
	private List<Header> headers;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public InputStream getContent() {
		return content;
	}
	public void setContent(InputStream content) {
		this.content = content;
	}
	public Boolean getInline() {
		return inline;
	}
	public void setInline(Boolean inline) {
		this.inline = inline;
	}
	public List<Header> getHeaders() {
		return headers;
	}
	public void setHeaders(List<Header> headers) {
		this.headers = headers;
	}
}
