package testhibernate;

import com.ea.agentloader.AgentLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContextWrapper;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.cfg.Environment;

import javax.sql.DataSource;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public class HibernateUtil {
    static {
        AgentLoader.loadAgentClass(JPAAgent.class.getName(), "");
    }

    public static class JPAAgent
    {
        public static void agentmain(String agentArgs, Instrumentation inst) throws Throwable
        {
            inst.addTransformer(new HibernateEnhancementTransformer());
        }

        public static class HibernateEnhancementTransformer implements ClassFileTransformer {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer)
            {
                Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer(new EnhancementContextWrapper(
                        new DefaultEnhancementContext(), loader));
                return enhancer.enhance(className, classfileBuffer);
            }
        }
    }



    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Create registry builder
                StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

                // Hibernate settings equivalent to hibernate.cfg.xml's properties
                Map<String, Object> settings = new HashMap<>();
                settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                settings.put(Environment.DATASOURCE, dataSource());

                settings.put("hibernate.hbm2ddl.auto", "create");

                /*settings.put("hibernate.enhancer.enableDirtyTracking", "true");
                settings.put("hibernate.enhancer.enableLazyInitialization", "true");
                settings.put("hibernate.enhancer.enableAssociationManagement", "true");
                settings.put("hibernate.ejb.use_class_enhancer", "true");*/

                // Apply settings
                registryBuilder.applySettings(settings);

                // Create registry
                registry = registryBuilder.build();

                // Create MetadataSources
                MetadataSources sources = new MetadataSources(registry);
                sources.addAnnotatedClass(User.class)
                        .addAnnotatedClass(Room.class);

                // Create Metadata
                Metadata metadata = sources.getMetadataBuilder()
                        .build();

                // Create SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }


    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }


    private static DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl("jdbc:h2:mem:test?user=sa");
        config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(config);
    }
}
