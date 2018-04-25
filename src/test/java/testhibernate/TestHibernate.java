package testhibernate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestHibernate {
    static EntityManagerFactory emf;

    @BeforeClass
    public static void createEmf() {
        emf = HibernateUtil.getSessionFactory();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        createSampeData(em);
        em.getTransaction().commit();
        em.close();
    }

    @AfterClass
    public static void shutdown() {
        HibernateUtil.shutdown();
    }


    @Test
    public void testEntityManager() {
        for (Field field : Room.class.getClass().getDeclaredFields()) {
            System.out.println(field.getName());
        }
        for (Method m : Room.class.getClass().getDeclaredMethods()) {
            System.out.println(m.getName()+"()");
        }

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        {
            User user = em.find(User.class, "User 1");
            assertNotNull(user);
            Map<String, String> properties = user.getProperties();
            String val = properties.get("prop 0");
            assertEquals("value 0", val);
        }
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        {
            Room room = em.find(Room.class, "Room 1");
            assertNotNull(room);
            User admin = room.getAdmin();
            assertNotNull(admin);
            Map<String, String> properties = admin.getProperties();
            String val = properties.get("prop 0");
            assertEquals("value 0", val);
            List<User> users = room.getUsers();
            assertFalse(users.isEmpty());
            User user = users.get(2);
            properties = user.getProperties();
            assertEquals("value 0", properties.get("prop 0"));
        }
        em.getTransaction().commit();
        em.close();
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testEnhancement() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Room room = em.find(Room.class, "Room 1");

        // Try to access a field 'users' directly using reflection
        Field field = room.getClass().getDeclaredField("users");
        field.setAccessible(true);
        List<User> users = (List<User>) field.get(room);
        // Without enhancement instead of null a proxy list will be returned (PersistentBag)
        assertNull(users);
        // Access via getter will query database and return users entities
        users = room.getUsers();
        assertFalse(users.isEmpty());

        em.getTransaction().commit();
        em.close();
    }



    private static void createSampeData(EntityManager em) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            users.add(createUser(i));

        Room room1 = new Room();
        room1.setName("Room 1");
        room1.setUsers(new ArrayList<>());
        for (int i = 0; i < 3; i++)
            room1.getUsers().add(users.get(i));
        room1.setAdmin(users.get(0));

        Room room2 = new Room();
        room2.setName("Room 2");
        room2.setUsers(new ArrayList<>());
        for (int i = 3; i < 10; i++)
            room2.getUsers().add(users.get(i));
        room2.setAdmin(users.get(1));
        em.persist(room1);
        em.persist(room2);
    }

    private static User createUser(int idx) {
        User user = new User();
        user.setAge(10 + idx);
        user.setName("User " + idx);
        int max = (int) (Math.random() * 10) + 1;
        user.setProperties(new HashMap<>());
        for (int i = 0; i < max; i++) {
            user.getProperties().put("prop " + i, "value " + i);
        }
        return user;
    }
}
