package org.cakesolutions.scaladata.example;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author janmachacek
 */
@Entity
public class User {
	@Id @GeneratedValue
	private Long id;
	@Version
	private int version;
	private String username;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("User");
		sb.append("{id=").append(id);
		sb.append(", version=").append(version);
		sb.append(", username='").append(username).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
