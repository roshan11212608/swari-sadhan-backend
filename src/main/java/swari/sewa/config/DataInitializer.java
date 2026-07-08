package swari.sewa.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import swari.sewa.common.enums.UserRole;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.user.entity.ShopOwner;
import swari.sewa.module.shop.repository.ShopRepository;
import swari.sewa.module.user.repository.ShopOwnerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShopRepository shopRepository;
    private final ShopOwnerRepository shopOwnerRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
        initializeShops();
    }

    private void initializeUsers() {
        try {
            // Check and create admin user
            if (!userRepository.existsByEmail("admin@swari.com")) {
                log.info("Creating admin user...");
                User superAdmin = User.builder()
                        .email("admin@swari.com")
                        .password(passwordEncoder.encode("Admin@123"))
                        .firstName("Super")
                        .lastName("Admin")
                        .phoneNumber("977-9841234567")
                        .role(UserRole.SUPERADMIN)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();
                userRepository.save(superAdmin);
                log.info("Admin user created successfully!");
            } else {
                log.info("Admin user already exists.");
            }

            // Check and create shop owner user
            if (!userRepository.existsByEmail("owner@shop.com")) {
                log.info("Creating shop owner user...");
                User shopOwner = User.builder()
                        .email("owner@shop.com")
                        .password(passwordEncoder.encode("Owner@123"))
                        .firstName("Shop")
                        .lastName("Owner")
                        .phoneNumber("977-9841234568")
                        .role(UserRole.SHOP_OWNER)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();
                userRepository.save(shopOwner);
                log.info("Shop owner user created successfully!");
            } else {
                log.info("Shop owner user already exists.");
            }

            // Check and create customer user
            if (!userRepository.existsByEmail("user@demo.com")) {
                log.info("Creating customer user...");
                User customer = User.builder()
                        .email("user@demo.com")
                        .password(passwordEncoder.encode("User@123"))
                        .firstName("Demo")
                        .lastName("User")
                        .phoneNumber("977-9841234569")
                        .role(UserRole.PUBLIC)
                        .isActive(true)
                        .isEmailVerified(true)
                        .build();
                userRepository.save(customer);
                log.info("Customer user created successfully!");
            } else {
                log.info("Customer user already exists.");
            }
        } catch (Exception e) {
            log.error("Error initializing users: {}", e.getMessage(), e);
        }
    }

    private void initializeShops() {
        try {
            // Create or get shop owner profile
            ShopOwner shopOwner = shopOwnerRepository.findByEmail("owner@shop.com")
                    .orElseGet(() -> {
                        log.info("Creating shop owner profile...");
                        return ShopOwner.builder()
                                .firstName("Shop")
                                .lastName("Owner")
                                .email("owner@shop.com")
                                .password("Owner@123") // This should be encoded
                                .phone("977-9841234568")
                                .active(true)
                                .emailVerified(true)
                                .build();
                    });

            if (shopOwner.getId() == null) {
                shopOwner.setPassword(passwordEncoder.encode("Owner@123"));
                shopOwnerRepository.save(shopOwner);
                log.info("Shop owner profile created successfully!");
            }

            // Check and create sample shop
            if (!shopRepository.existsByLicenseNumber("SHOP-001")) {
                log.info("Creating sample shop...");

                // Get the shop owner user
                User shopOwnerUser = userRepository.findByEmail("owner@shop.com")
                        .orElseThrow(() -> new RuntimeException("Shop owner user not found"));

                Shop sampleShop = Shop.builder()
                        .name("Demo Bike Shop")
                        .description("A sample bike shop for testing vehicle creation")
                        .licenseNumber("SHOP-001")
                        .addressLine1("123 Main Street")
                        .city("Kathmandu")
                        .state("Bagmati")
                        .country("Nepal")
                        .postalCode("44600")
                        .phoneNumber("977-9841234568")
                        .emailAddress("demo@shop.com")
                        .status(ShopStatus.ACTIVE)
                        .isFeatured(false)
                        .shopOwner(shopOwner)
                        .user(shopOwnerUser)
                        .build();

                shopRepository.save(sampleShop);
                log.info("Sample shop created successfully with ID: {}", sampleShop.getId());
            } else {
                log.info("Sample shop already exists.");
            }
        } catch (Exception e) {
            log.error("Error initializing shops: {}", e.getMessage(), e);
        }
    }
}
