package scalad.example;

import javax.persistence.*;

/**
 * @author janmachacek
 */
@Entity
public class UserAddress {
	@Id @GeneratedValue
	private Long id;
	@Version
	private int version;
	@ManyToOne(fetch = FetchType.LAZY)
	private User user;
	private String line1;
	private String line2;
	private String line3;

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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getLine1() {
		return line1;
	}

	public void setLine1(String line1) {
		this.line1 = line1;
	}

	public String getLine2() {
		return line2;
	}

	public void setLine2(String line2) {
		this.line2 = line2;
	}

	public String getLine3() {
		return line3;
	}

	public void setLine3(String line3) {
		this.line3 = line3;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("UserAddress");
		sb.append("{id=").append(id);
		sb.append(", version=").append(version);
		sb.append(", line1='").append(line1).append('\'');
		sb.append(", line2='").append(line2).append('\'');
		sb.append(", line3='").append(line3).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
