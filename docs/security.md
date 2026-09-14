# Integración de darkbit-security

Este proyecto utiliza la librería **darkbit-security** para la gestión de seguridad. A continuación se explica cómo está integrada y cómo replicar la configuración:

## Clases que se deben agregar

- CustomUserDetailsService
- AuthController
- RolePersistenceAdapter
- UserPersistenceAdapter
- PermissionEntity
- RoleEntity
- UserEntity
- UserEntitySecurityMapper
- JpaRoleRepository
- JpaUserRepository
- SecurityConfig

## Configuración en pom.xml

Agrega la siguiente dependencia en la sección `<dependencies>`:

```xml
<dependency>
    <groupId>com.github.team-dark-bit</groupId>
    <artifactId>security-darkbit</artifactId>
    <version>v1.0.1</version>
</dependency>
```

## Configuración en application.yml

Asegúrate de tener las siguientes propiedades:

```yaml
security:
  darkbit:
    enabled: true
```

Además, configura los valores de JWT y otros parámetros de seguridad según tus necesidades:

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 259200000 # 72 horas en milisegundos
```

## Notas

- Revisa cada clase listada para entender su rol en la integración.
- Consulta la documentación oficial de darkbit-security para personalizaciones avanzadas.
- Si tienes dudas, contacta al responsable de la integración o revisa el historial de cambios.

