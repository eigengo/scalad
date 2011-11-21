package scalad.example;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Set<UserAddress> addresses = new HashSet<UserAddress>();
	
	public void addAddress(UserAddress userAddress) {
		userAddress.setUser(this);
		this.addresses.add(userAddress);
	}

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

	public Set<UserAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<UserAddress> addresses) {
		this.addresses = addresses;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("User");
		sb.append("{id=").append(id);
		sb.append(", version=").append(version);
		sb.append(", username='").append(username).append('\'');
		sb.append(", addresses=").append(addresses);
		sb.append('}');
		return sb.toString();
	}
}
