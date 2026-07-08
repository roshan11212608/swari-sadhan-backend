package swari.sewa.module.shop.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.shop.dto.ShopDto;
import swari.sewa.module.shop.service.ShopService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ShopDto> createShop(@Valid @RequestBody ShopDto shopDto, 
                                             @RequestHeader("X-User-Id") Long userId) {
        ShopDto createdShop = shopService.createShop(shopDto, userId);
        return ResponseEntity.ok(createdShop);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopDto> getShopById(@PathVariable Long id) {
        Optional<ShopDto> shop = shopService.getShopById(id);
        return shop.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('SHOP_OWNER') and @userSecurity.isOwner(#userId, authentication.name))")
    public ResponseEntity<ShopDto> getShopByUserId(@PathVariable Long userId) {
        Optional<ShopDto> shop = shopService.getShopByUserId(userId);
        return shop.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ShopDto>> getAllShops() {
        List<ShopDto> shops = shopService.getAllShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<ShopDto>> getShopsByCity(@PathVariable String city) {
        List<ShopDto> shops = shopService.getShopsByCity(city);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/state/{state}")
    public ResponseEntity<List<ShopDto>> getShopsByState(@PathVariable String state) {
        List<ShopDto> shops = shopService.getShopsByState(state);
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ShopDto>> getFeaturedShops() {
        List<ShopDto> shops = shopService.getFeaturedShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ShopDto>> searchShops(@RequestParam String keyword) {
        List<ShopDto> shops = shopService.searchShops(keyword);
        return ResponseEntity.ok(shops);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('SHOP_OWNER') and @shopSecurity.isOwner(#id, authentication.name))")
    public ResponseEntity<ShopDto> updateShop(@PathVariable Long id, @Valid @RequestBody ShopDto shopDto) {
        ShopDto updatedShop = shopService.updateShop(id, shopDto);
        return ResponseEntity.ok(updatedShop);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        shopService.deleteShop(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ShopDto> approveShop(@PathVariable Long id) {
        ShopDto shop = shopService.approveShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ShopDto> rejectShop(@PathVariable Long id) {
        ShopDto shop = shopService.rejectShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ShopDto> activateShop(@PathVariable Long id) {
        ShopDto shop = shopService.activateShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ShopDto> deactivateShop(@PathVariable Long id) {
        ShopDto shop = shopService.deactivateShop(id);
        return ResponseEntity.ok(shop);
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ShopDto> suspendShop(@PathVariable Long id) {
        ShopDto shop = shopService.suspendShop(id);
        return ResponseEntity.ok(shop);
    }
}
