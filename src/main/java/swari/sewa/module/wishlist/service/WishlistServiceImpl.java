package swari.sewa.module.wishlist.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.wishlist.dto.WishlistDto;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.common.exception.WishlistAlreadyExistsException;
import swari.sewa.module.wishlist.entity.Wishlist;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.wishlist.repository.WishlistRepository;
import swari.sewa.module.user.repository.UserRepository;
import swari.sewa.module.vehicle.repository.VehicleRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    @Override
    public WishlistDto addToWishlist(Long customerId, Long vehicleId) {
        if (wishlistRepository.existsByCustomerIdAndVehicleId(customerId, vehicleId)) {
            throw new WishlistAlreadyExistsException("Vehicle already in wishlist");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        Wishlist wishlist = Wishlist.builder()
                .customer(customer)
                .vehicle(vehicle)
                .build();

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return mapToDtoWithDetails(savedWishlist);
    }

    @Override
    public void removeFromWishlist(Long customerId, Long vehicleId) {
        Wishlist wishlist = wishlistRepository.findByCustomerIdAndVehicleId(customerId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WishlistDto> getWishlistById(Long id) {
        return wishlistRepository.findById(id)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WishlistDto> getCustomerWishlist(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wishlistRepository.findByCustomer_Id(customerId, pageable)
                .map(this::mapToDtoWithDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistDto> getCustomerWishlist(Long customerId) {
        return wishlistRepository.findByCustomer_Id(customerId).stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistDto> getShopWishlist(Long shopId) {
        return wishlistRepository.findByVehicle_Shop_Id(shopId).stream()
                .map(this::mapToDtoWithDetails)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long customerId, Long vehicleId) {
        return wishlistRepository.existsByCustomerIdAndVehicleId(customerId, vehicleId);
    }

    @Override
    public void deleteWishlist(Long id) {
        Wishlist wishlist = wishlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist not found with id: " + id));
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWishlistCount(Long customerId) {
        return wishlistRepository.countByCustomerId(customerId);
    }

    private WishlistDto mapToDtoWithDetails(Wishlist wishlist) {
        WishlistDto dto = modelMapper.map(wishlist, WishlistDto.class);
        dto.setCustomerId(wishlist.getCustomer().getId());
        dto.setCustomerName(wishlist.getCustomer().getFirstName() + " " + wishlist.getCustomer().getLastName());
        dto.setCustomerPhone(wishlist.getCustomer().getPhone());
        dto.setCustomerEmail(wishlist.getCustomer().getEmail());
        dto.setVehicleId(wishlist.getVehicle().getId());
        dto.setVehicleTitle(wishlist.getVehicle().getTitle());
        dto.setVehicleMainImageUrl(wishlist.getVehicle().getMainImageUrl());
        dto.setVehiclePrice(wishlist.getVehicle().getPrice());
        dto.setShopName(wishlist.getVehicle().getShop().getName());
        return dto;
    }
}
