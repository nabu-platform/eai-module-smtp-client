package be.nabu.module.protocol.smtp;

public enum EmailType {
	TEXT("text/plain"),
	HTML("text/html");
	
	private String contentType;

	private EmailType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}
}
