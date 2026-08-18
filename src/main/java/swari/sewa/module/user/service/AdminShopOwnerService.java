package swari.sewa.module.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import swari.sewa.module.dashboard.dto.ShopOwnerDto;

public interface AdminShopOwnerService {

    Page<ShopOwnerDto> getAllShopOwners(Pageable pageable, String search, String status);

    ShopOwnerDto getShopOwnerById(Long id);

    ShopOwnerDto createShopOwner(ShopOwnerDto shopOwnerDto);

    ShopOwnerDto createShopOwnerWithFiles(String shopOwnerDataJson, MultipartFile profilePhoto, MultipartFile shopLogo, MultipartFile citizenshipPicFront, MultipartFile citizenshipPicBack, MultipartFile shopRegUpload);

    ShopOwnerDto updateShopOwner(Long id, ShopOwnerDto shopOwnerDto);

    void deleteShopOwner(Long id);

    void approveShopOwner(Long id);

    void rejectShopOwner(Long id, String reason);

    void suspendShopOwner(Long id);

    void reactivateShopOwner(Long id);

    void changeShopOwnerPassword(Long id, String newPassword);

    Page<Object> getShopOwnerShops(Long id, Pageable pageable);

    Page<Object> getShopOwnerVehicles(Long id, Pageable pageable);
}
