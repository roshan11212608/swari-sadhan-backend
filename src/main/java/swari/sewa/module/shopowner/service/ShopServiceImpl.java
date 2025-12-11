package swari.sewa.module.shopowner.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.shopowner.dto.ShopDto;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.LicenseNumberAlreadyExistsException;
import swari.sewa.module.shopowner.model.Shop;
import swari.sewa.module.publicuser.model.User;
import swari.sewa.module.shopowner.repository.ShopRepository;
import swari.sewa.module.publicuser.repository.UserRepository;
import swari.sewa.common.enums.ShopStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public ShopDto createShop(ShopDto shopDto, Long userId) {
        if (shopRepository.existsByLicenseNumber(shopDto.getLicenseNumber())) {
            throw new LicenseNumberAlreadyExistsException("License number already exists: " + shopDto.getLicenseNumber());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Shop shop = modelMapper.map(shopDto, Shop.class);
        shop.setUser(user);
        shop.setStatus(ShopStatus.PENDING_APPROVAL);

        Shop savedShop = shopRepository.save(shop);
        return modelMapper.map(savedShop, ShopDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopDto> getShopById(Long id) {
        return shopRepository.findById(id)
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopDto> getShopByUserId(Long userId) {
        return shopRepository.findByUserId(userId).stream()
                .findFirst()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getAllShops() {
        return shopRepository.findAll().stream()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getShopsByCity(String city) {
        return shopRepository.findByCityAndStatusActive(city).stream()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getShopsByState(String state) {
        return shopRepository.findByStateAndStatusActive(state).stream()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> getFeaturedShops() {
        return shopRepository.findFeaturedShops().stream()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ShopDto updateShop(Long id, ShopDto shopDto) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));

        if (!shop.getLicenseNumber().equals(shopDto.getLicenseNumber()) && 
            shopRepository.existsByLicenseNumber(shopDto.getLicenseNumber())) {
            throw new LicenseNumberAlreadyExistsException("License number already exists: " + shopDto.getLicenseNumber());
        }

        modelMapper.map(shopDto, shop);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    public void deleteShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shopRepository.delete(shop);
    }

    @Override
    public ShopDto approveShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.ACTIVE);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    public ShopDto rejectShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.REJECTED);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    public ShopDto activateShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.ACTIVE);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    public ShopDto deactivateShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.INACTIVE);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    public ShopDto suspendShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
        shop.setStatus(ShopStatus.SUSPENDED);
        Shop updatedShop = shopRepository.save(shop);
        
        ShopDto result = modelMapper.map(updatedShop, ShopDto.class);
        result.setUserId(updatedShop.getUser().getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLicenseNumber(String licenseNumber) {
        return shopRepository.existsByLicenseNumber(licenseNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDto> searchShops(String keyword) {
        return shopRepository.searchByKeyword(keyword).stream()
                .map(shop -> {
                    ShopDto shopDto = modelMapper.map(shop, ShopDto.class);
                    shopDto.setUserId(shop.getUser().getId());
                    return shopDto;
                })
                .collect(Collectors.toList());
    }
}
