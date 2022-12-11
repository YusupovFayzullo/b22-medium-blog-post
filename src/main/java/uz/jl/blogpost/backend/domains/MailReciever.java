package uz.jl.blogpost.backend.domains;

import java.time.LocalDateTime;

public class MailReciever implements Entity{


	private  String id;
	private String code;
	private LocalDateTime localDateTime;

	public MailReciever(String id, String code, LocalDateTime localDateTime) {
		this.id = id;
		this.code = code;
		this.localDateTime = localDateTime;
	}

	public MailReciever() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}

	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}
}
