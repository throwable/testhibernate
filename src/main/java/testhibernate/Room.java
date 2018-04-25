package testhibernate;

import javax.persistence.*;
import java.util.List;

@Entity
public class Room {
    @Id
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<User> users;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User admin;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }


    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }
}
