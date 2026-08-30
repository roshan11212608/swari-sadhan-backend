package swari.sewa.module.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swari.sewa.module.subscription.entity.ShopOwnerRemark;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopOwnerRemarkRepository extends JpaRepository<ShopOwnerRemark, Long> {

    List<ShopOwnerRemark> findByShopOwnerIdOrderByCreatedAtDesc(Long shopOwnerId);

    Optional<ShopOwnerRemark> findFirstByShopOwnerIdOrderByCreatedAtDesc(Long shopOwnerId);
}
