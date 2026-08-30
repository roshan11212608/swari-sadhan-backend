package swari.sewa.module.banner.service;

import java.util.List;

import swari.sewa.module.banner.dto.BannerDto;

public interface BannerService {

    List<BannerDto> getAllBanners();

    List<BannerDto> getActiveBanners();

    BannerDto getBannerByPosition(String position);

    BannerDto createBanner(BannerDto bannerDto);

    BannerDto updateBanner(Long id, BannerDto bannerDto);

    void deleteBanner(Long id);
}
