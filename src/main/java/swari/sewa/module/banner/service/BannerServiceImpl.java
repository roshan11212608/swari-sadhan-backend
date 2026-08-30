package swari.sewa.module.banner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.module.banner.dto.BannerDto;
import swari.sewa.module.banner.entity.Banner;
import swari.sewa.module.banner.repository.BannerRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BannerDto> getAllBanners() {
        return bannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerDto> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BannerDto getBannerByPosition(String position) {
        return bannerRepository.findByPositionAndIsActiveTrue(position)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public BannerDto createBanner(BannerDto bannerDto) {
        Banner banner = Banner.builder()
                .imageUrl(bannerDto.getImageUrl())
                .title(bannerDto.getTitle())
                .position(bannerDto.getPosition())
                .isActive(bannerDto.getIsActive() != null ? bannerDto.getIsActive() : true)
                .displayOrder(bannerDto.getDisplayOrder() != null ? bannerDto.getDisplayOrder() : 0)
                .build();
        Banner saved = bannerRepository.save(banner);
        return toDto(saved);
    }

    @Override
    public BannerDto updateBanner(Long id, BannerDto bannerDto) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));

        if (bannerDto.getImageUrl() != null) banner.setImageUrl(bannerDto.getImageUrl());
        if (bannerDto.getTitle() != null) banner.setTitle(bannerDto.getTitle());
        if (bannerDto.getPosition() != null) banner.setPosition(bannerDto.getPosition());
        if (bannerDto.getIsActive() != null) banner.setIsActive(bannerDto.getIsActive());
        if (bannerDto.getDisplayOrder() != null) banner.setDisplayOrder(bannerDto.getDisplayOrder());

        Banner saved = bannerRepository.save(banner);
        return toDto(saved);
    }

    @Override
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        bannerRepository.delete(banner);
    }

    private BannerDto toDto(Banner banner) {
        return BannerDto.builder()
                .id(banner.getId())
                .imageUrl(banner.getImageUrl())
                .title(banner.getTitle())
                .position(banner.getPosition())
                .isActive(banner.getIsActive())
                .displayOrder(banner.getDisplayOrder())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
