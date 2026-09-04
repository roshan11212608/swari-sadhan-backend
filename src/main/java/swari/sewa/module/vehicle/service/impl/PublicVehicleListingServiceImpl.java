package swari.sewa.module.vehicle.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swari.sewa.common.enums.PublicVehicleListingFileType;
import swari.sewa.common.enums.PublicVehicleListingStatus;
import swari.sewa.module.vehicle.dto.*;
import swari.sewa.module.vehicle.entity.PublicVehicleListing;
import swari.sewa.module.vehicle.entity.PublicVehicleListingFile;
import swari.sewa.module.vehicle.entity.PublicVehicleListingReviewHistory;
import swari.sewa.module.vehicle.entity.PublicVehicleListingSequence;
import swari.sewa.module.vehicle.repository.PublicVehicleListingFileRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingReviewHistoryRepository;
import swari.sewa.module.vehicle.repository.PublicVehicleListingSequenceRepository;
import swari.sewa.module.vehicle.service.PublicVehicleListingService;
import swari.sewa.common.service.MultipartUploadValidator;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicVehicleListingServiceImpl implements PublicVehicleListingService {

    private final PublicVehicleListingRepository listingRepository;
    private final PublicVehicleListingFileRepository fileRepository;
    private final PublicVehicleListingSequenceRepository sequenceRepository;
    private final PublicVehicleListingReviewHistoryRepository reviewHistoryRepository;
    private final UserRepository userRepository;
    private final MultipartUploadValidator uploadValidator;

    private static final List<PublicVehicleListingStatus> ACTIVE_STATUSES = List.of(
            PublicVehicleListingStatus.SUBMITTED,
            PublicVehicleListingStatus.UNDER_REVIEW,
            PublicVehicleListingStatus.CHANGES_REQUESTED,
            PublicVehicleListingStatus.APPROVED,
            PublicVehicleListingStatus.PUBLISHED
    );

    @Override
    public PublicVehicleListingSellerDto createListing(PublicVehicleListingRequestDto dto, Long sellerUserId, boolean draft) {
        validateVehicleNumber(dto.getVehicleNumber(), null);
        validateYears(dto);
        validatePrice(dto.getPrice());

        PublicVehicleListing listing = toEntity(dto);
        listing.setSellerUserId(sellerUserId);
        listing.setStatus(draft ? PublicVehicleListingStatus.DRAFT : PublicVehicleListingStatus.SUBMITTED);
        listing.setListingNumber(generateListingNumber());
        if (!draft) {
            listing.setSubmittedAt(LocalDateTime.now());
        }

        // Build and attach files
        attachFiles(listing, dto.getFiles());

        listing = listingRepository.save(listing);
        saveHistory(listing.getId(), "SELLER", draft ? "DRAFT_CREATED" : "SUBMITTED", null, null);
        return toSellerDto(listing);
    }

    @Override
    public PublicVehicleListingSellerDto updateListing(Long id, PublicVehicleListingRequestDto dto, Long sellerUserId) {
        PublicVehicleListing listing = listingRepository.findByIdAndSellerUserId(id, sellerUserId)
                .orElseThrow(() -> new RuntimeException("Listing not found or access denied"));

        if (!canSellerUpdate(listing.getStatus())) {
            throw new RuntimeException("Listing cannot be updated in status: " + listing.getStatus());
        }

        validateVehicleNumber(dto.getVehicleNumber(), id);
        validateYears(dto);
        validatePrice(dto.getPrice());

        updateEntity(listing, dto);

        // Replace files
        listing.getFiles().clear();
        attachFiles(listing, dto.getFiles());

        // When a seller updates a listing that had changes requested (or was a draft),
        // move it back to SUBMITTED so the admin knows to re-review. Clear the previous
        // rejection/changes feedback and record when the seller made their edits.
        if (listing.getStatus() == PublicVehicleListingStatus.CHANGES_REQUESTED
                || listing.getStatus() == PublicVehicleListingStatus.DRAFT) {
            listing.setStatus(PublicVehicleListingStatus.SUBMITTED);
            listing.setRejectionReason(null);
            listing.setReviewedAt(null);
            listing.setSubmittedAt(LocalDateTime.now());
        }
        listing.setSellerUpdatedAt(LocalDateTime.now());

        listing = listingRepository.save(listing);
        saveHistory(id, "SELLER", "SELLER_UPDATED", null, null);
        return toSellerDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicVehicleListingSellerDto getListingForSeller(Long id, Long sellerUserId) {
        PublicVehicleListing listing = listingRepository.findByIdAndSellerUserId(id, sellerUserId)
                .orElseThrow(() -> new RuntimeException("Listing not found or access denied"));
        return toSellerDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicVehicleListingSellerDto> getListingsForSeller(Long sellerUserId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return listingRepository.findBySellerUserIdAndStatus(
                    sellerUserId,
                    PublicVehicleListingStatus.valueOf(status),
                    pageable
            ).map(this::toSellerDto);
        }
        return listingRepository.findBySellerUserId(sellerUserId, pageable).map(this::toSellerDto);
    }

    @Override
    public void deleteListing(Long id, Long sellerUserId) {
        PublicVehicleListing listing = listingRepository.findByIdAndSellerUserId(id, sellerUserId)
                .orElseThrow(() -> new RuntimeException("Listing not found or access denied"));
        listing.setStatus(PublicVehicleListingStatus.CANCELLED);
        listing.setUpdatedAt(LocalDateTime.now());
        listingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicVehicleListingResponseDto getPublicListing(Long id) {
        PublicVehicleListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (listing.getStatus() != PublicVehicleListingStatus.PUBLISHED) {
            throw new RuntimeException("Listing is not publicly available");
        }

        return toResponseDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicVehicleListingResponseDto> getPublicListings(Pageable pageable, java.math.BigDecimal maxPrice, String brand, String city) {
        return listingRepository.findPublishedListings(maxPrice, brand, city, pageable).map(this::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicVehicleListingAdminDto getListingForAdmin(Long id) {
        PublicVehicleListing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        return toAdminDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PublicVehicleListingAdminDto> getListingsForAdmin(String status, String search, Pageable pageable) {
        Page<PublicVehicleListing> page;

        if (status != null && !status.isEmpty()) {
            page = listingRepository.findByStatus(PublicVehicleListingStatus.valueOf(status), pageable);
        } else {
            page = listingRepository.findAll(pageable);
        }

        // Apply simple search filter if provided
        if (search != null && !search.isEmpty()) {
            String term = search.toLowerCase();
            List<PublicVehicleListing> filtered = page.getContent().stream()
                    .filter(l -> matchesSearch(l, term))
                    .collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.stream().map(this::toAdminDto).collect(Collectors.toList()),
                    pageable,
                    filtered.size()
            );
        }

        return page.map(this::toAdminDto);
    }

    @Override
    public PublicVehicleListingAdminDto approveListing(Long id, PublicVehicleListingActionDto action) {
        PublicVehicleListing listing = getAdminListing(id);

        if (listing.getStatus() != PublicVehicleListingStatus.UNDER_REVIEW
                && listing.getStatus() != PublicVehicleListingStatus.SUBMITTED
                && listing.getStatus() != PublicVehicleListingStatus.CHANGES_REQUESTED) {
            throw new RuntimeException("Listing cannot be approved in status: " + listing.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        listing.setStatus(PublicVehicleListingStatus.PUBLISHED);
        listing.setApprovedAt(now);
        listing.setPublishedAt(now);
        listing.setReviewedAt(now);
        listing.setSellerUpdatedAt(null);
        if (action.getNotes() != null && !action.getNotes().isEmpty()) {
            listing.setAdminNotes(action.getNotes());
        }

        listing = listingRepository.save(listing);
        saveHistory(id, "ADMIN", "APPROVED", null, action.getNotes());
        return toAdminDto(listing);
    }

    @Override
    public PublicVehicleListingAdminDto rejectListing(Long id, PublicVehicleListingActionDto action) {
        PublicVehicleListing listing = getAdminListing(id);

        listing.setStatus(PublicVehicleListingStatus.REJECTED);
        listing.setRejectionReason(action.getReason());
        listing.setReviewedAt(LocalDateTime.now());
        listing.setSellerUpdatedAt(null);
        if (action.getNotes() != null && !action.getNotes().isEmpty()) {
            listing.setAdminNotes(action.getNotes());
        }

        listing = listingRepository.save(listing);
        saveHistory(id, "ADMIN", "REJECTED", action.getReason(), action.getNotes());
        return toAdminDto(listing);
    }

    @Override
    public PublicVehicleListingAdminDto requestChanges(Long id, PublicVehicleListingActionDto action) {
        PublicVehicleListing listing = getAdminListing(id);

        listing.setStatus(PublicVehicleListingStatus.CHANGES_REQUESTED);
        listing.setAdminNotes(action.getReason());
        listing.setReviewedAt(LocalDateTime.now());
        listing.setSellerUpdatedAt(null);

        listing = listingRepository.save(listing);
        saveHistory(id, "ADMIN", "CHANGES_REQUESTED", action.getReason(), action.getNotes());
        return toAdminDto(listing);
    }

    @Override
    public PublicVehicleListingAdminDto markAsSold(Long id, PublicVehicleListingActionDto action) {
        PublicVehicleListing listing = getAdminListing(id);

        if (listing.getStatus() != PublicVehicleListingStatus.PUBLISHED) {
            throw new RuntimeException("Only published listings can be marked as sold");
        }

        listing.setStatus(PublicVehicleListingStatus.SOLD);
        listing.setSoldAt(LocalDateTime.now());
        if (action.getNotes() != null && !action.getNotes().isEmpty()) {
            listing.setAdminNotes(action.getNotes());
        }

        listing = listingRepository.save(listing);
        saveHistory(id, "ADMIN", "SOLD", null, action.getNotes());
        return toAdminDto(listing);
    }

    @Override
    public PublicVehicleListingAdminDto underReviewListing(Long id) {
        PublicVehicleListing listing = getAdminListing(id);

        if (listing.getStatus() == PublicVehicleListingStatus.SUBMITTED) {
            listing.setStatus(PublicVehicleListingStatus.UNDER_REVIEW);
            listing.setReviewedAt(LocalDateTime.now());
            listing.setSellerUpdatedAt(null);
            listing = listingRepository.save(listing);
            saveHistory(id, "ADMIN", "UNDER_REVIEW", null, null);
            return toAdminDto(listing);
        }

        return toAdminDto(listing);
    }

    @Override
    public PublicVehicleListingAdminDto unpublishListing(Long id, PublicVehicleListingActionDto action) {
        PublicVehicleListing listing = getAdminListing(id);

        if (listing.getStatus() != PublicVehicleListingStatus.PUBLISHED) {
            throw new RuntimeException("Only published listings can be unpublished. Current status: " + listing.getStatus());
        }

        listing.setStatus(PublicVehicleListingStatus.UNPUBLISHED);
        listing.setPublishedAt(null);
        listing.setReviewedAt(LocalDateTime.now());
        if (action != null && action.getNotes() != null && !action.getNotes().isEmpty()) {
            listing.setAdminNotes(action.getNotes());
        }

        listing = listingRepository.save(listing);
        saveHistory(id, "ADMIN", "UNPUBLISHED", action != null ? action.getReason() : null, action != null ? action.getNotes() : null);
        return toAdminDto(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllActiveVehicleNumbers() {
        return listingRepository.findByStatusIn(ACTIVE_STATUSES, Pageable.unpaged()).getContent().stream()
                .map(PublicVehicleListing::getVehicleNumber)
                .distinct()
                .collect(Collectors.toList());
    }

    /* ===================== HELPERS ===================== */

    private PublicVehicleListing getAdminListing(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
    }

    private void saveHistory(Long listingId, String actor, String action, String reason, String notes) {
        reviewHistoryRepository.save(PublicVehicleListingReviewHistory.builder()
                .listingId(listingId)
                .actor(actor)
                .action(action)
                .reason(reason)
                .notes(notes)
                .performedAt(LocalDateTime.now())
                .build());
    }

    private boolean canSellerUpdate(PublicVehicleListingStatus status) {
        return status == PublicVehicleListingStatus.DRAFT
                || status == PublicVehicleListingStatus.CHANGES_REQUESTED;
    }

    private void validateVehicleNumber(String vehicleNumber, Long excludeId) {
        List<PublicVehicleListing> active = listingRepository.findActiveByVehicleNumber(vehicleNumber, ACTIVE_STATUSES);
        if (excludeId != null) {
            active = active.stream().filter(l -> !l.getId().equals(excludeId)).collect(Collectors.toList());
        }
        if (!active.isEmpty()) {
            throw new RuntimeException("A listing for vehicle number " + vehicleNumber + " is already active");
        }
    }

    private void validateYears(PublicVehicleListingRequestDto dto) {
        int currentYear = Year.now().getValue();

        if (dto.getManufacturingYear() == null || dto.getManufacturingYear() > currentYear) {
            throw new RuntimeException("Manufacturing year cannot be in the future");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Selling price must be greater than zero");
        }
    }

    /**
     * Generates a concurrency-safe, gap-free monthly listing number in the format
     * {@code SVL-<YYYYMM>-<NNNNNN>}.
     * <p>
     * A dedicated counter table ({@code public_vehicle_listing_sequence}) stores the
     * next available sequence value per {@code yearMonth}. The row is locked with a
     * pessimistic write lock ({@code SELECT ... FOR UPDATE}) for the duration of the
     * current transaction, so concurrent submissions serialize on the same row and
     * can never receive the same number. The counter is monotonically increasing and
     * never decremented, so numbers are never reused even if the listing is later
     * rejected, cancelled, deleted, or the surrounding transaction rolls back.
     * <p>
     * The sequence resets automatically when the month changes because each month
     * has its own primary-key row.
     */
    private String generateListingNumber() {
        YearMonth now = YearMonth.now();
        String yearMonth = String.format("%d%02d", now.getYear(), now.getMonthValue());

        PublicVehicleListingSequence seq = sequenceRepository.findByYearMonthForUpdate(yearMonth)
                .orElseGet(() -> {
                    PublicVehicleListingSequence created = PublicVehicleListingSequence.builder()
                            .yearMonth(yearMonth)
                            .nextValue(1L)
                            .build();
                    return sequenceRepository.save(created);
                });

        long next = seq.getNextValue();
        seq.setNextValue(next + 1);
        sequenceRepository.save(seq);

        return String.format("SVL-%s-%06d", yearMonth, next);
    }

    private void attachFiles(PublicVehicleListing listing, List<PublicVehicleListingFileDto> fileDtos) {
        if (fileDtos == null || fileDtos.isEmpty()) {
            return;
        }

        long vehiclePhotoCount = fileDtos.stream()
                .filter(f -> f.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO)
                .count();
        long bluebookCount = fileDtos.stream()
                .filter(f -> f.getFileType() == PublicVehicleListingFileType.BLUEBOOK)
                .count();
        long citizenshipCount = fileDtos.stream()
                .filter(f -> f.getFileType() == PublicVehicleListingFileType.CITIZENSHIP)
                .count();

        if (vehiclePhotoCount > MultipartUploadValidator.MAX_PUBLIC_VEHICLE_PHOTOS) {
            throw new IllegalArgumentException(
                    "Public vehicle listings accept at most " + MultipartUploadValidator.MAX_PUBLIC_VEHICLE_PHOTOS + " vehicle photos.");
        }
        if (bluebookCount > MultipartUploadValidator.MAX_PUBLIC_BLUEBOOK_FILES) {
            throw new IllegalArgumentException(
                    "Public vehicle listings accept at most " + MultipartUploadValidator.MAX_PUBLIC_BLUEBOOK_FILES + " bluebook files.");
        }
        if (citizenshipCount > MultipartUploadValidator.MAX_PUBLIC_CITIZENSHIP_FILES) {
            throw new IllegalArgumentException(
                    "Public vehicle listings accept at most " + MultipartUploadValidator.MAX_PUBLIC_CITIZENSHIP_FILES + " citizenship/ID file.");
        }

        for (int i = 0; i < fileDtos.size(); i++) {
            PublicVehicleListingFileDto fd = fileDtos.get(i);
            PublicVehicleListingFile file = new PublicVehicleListingFile();
            file.setListing(listing);
            file.setFileUrl(fd.getFileUrl());
            file.setOriginalFilename(fd.getOriginalFilename());
            file.setFileType(fd.getFileType());
            file.setDocumentType(fd.getDocumentType());

            // Only vehicle photos and videos are public
            boolean isPublic = fd.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO
                    || fd.getFileType() == PublicVehicleListingFileType.VEHICLE_VIDEO;
            file.setIsPublic(isPublic);

            // First public photo becomes cover unless explicitly set
            boolean isCover = Boolean.TRUE.equals(fd.getIsCover())
                    || (isPublic && fd.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO
                            && listing.getFiles().stream().noneMatch(PublicVehicleListingFile::getIsCover));
            file.setIsCover(isCover);

            file.setDisplayOrder(fd.getDisplayOrder() != null ? fd.getDisplayOrder() : i);

            listing.addFile(file);
        }
    }

    private boolean matchesSearch(PublicVehicleListing listing, String term) {
        return (listing.getListingNumber() != null && listing.getListingNumber().toLowerCase().contains(term))
                || (listing.getVehicleNumber() != null && listing.getVehicleNumber().toLowerCase().contains(term))
                || (listing.getBrand() != null && listing.getBrand().toLowerCase().contains(term))
                || (listing.getModel() != null && listing.getModel().toLowerCase().contains(term))
                || (listing.getTitle() != null && listing.getTitle().toLowerCase().contains(term))
                || (listing.getSellerName() != null && listing.getSellerName().toLowerCase().contains(term))
                || (listing.getSellerPhone() != null && listing.getSellerPhone().toLowerCase().contains(term));
    }

    /* ===================== MAPPERS ===================== */

    private PublicVehicleListing toEntity(PublicVehicleListingRequestDto dto) {
        return PublicVehicleListing.builder()
                .title(dto.getTitle())
                .lotNumber(dto.getLotNumber())
                .sellerName(dto.getSellerName())
                .sellerPhone(dto.getSellerPhone())
                .sellerAddress(dto.getSellerAddress())
                .ownerName(dto.getOwnerName())
                .ownerPhone(dto.getOwnerPhone())
                .ownerAddress(dto.getOwnerAddress())
                .vehicleNumber(dto.getVehicleNumber())
                .brand(dto.getBrand())
                .model(dto.getModel())
                .variant(dto.getVariant())
                .manufacturingYear(dto.getManufacturingYear())
                .kilometersDriven(dto.getKilometersDriven())
                .fuelType(dto.getFuelType())
                .engineCC(dto.getEngineCC())
                .color(dto.getColor())
                .price(dto.getPrice())
                .priceInWords(dto.getPriceInWords())
                .negotiable(dto.getNegotiable())
                .declarationAccepted(dto.getDeclarationAccepted())
                .build();
    }

    private void updateEntity(PublicVehicleListing listing, PublicVehicleListingRequestDto dto) {
        listing.setTitle(dto.getTitle());
        listing.setLotNumber(dto.getLotNumber());
        listing.setSellerName(dto.getSellerName());
        listing.setSellerPhone(dto.getSellerPhone());
        listing.setSellerAddress(dto.getSellerAddress());
        listing.setOwnerName(dto.getOwnerName());
        listing.setOwnerPhone(dto.getOwnerPhone());
        listing.setOwnerAddress(dto.getOwnerAddress());
        listing.setVehicleNumber(dto.getVehicleNumber());
        listing.setBrand(dto.getBrand());
        listing.setModel(dto.getModel());
        listing.setVariant(dto.getVariant());
        listing.setManufacturingYear(dto.getManufacturingYear());
        listing.setKilometersDriven(dto.getKilometersDriven());
        listing.setFuelType(dto.getFuelType());
        listing.setEngineCC(dto.getEngineCC());
        listing.setColor(dto.getColor());
        listing.setPrice(dto.getPrice());
        listing.setPriceInWords(dto.getPriceInWords());
        listing.setNegotiable(dto.getNegotiable());
        listing.setDeclarationAccepted(dto.getDeclarationAccepted());
        listing.setUpdatedAt(LocalDateTime.now());
    }

    private PublicVehicleListingFileDto toFileDto(PublicVehicleListingFile file) {
        return PublicVehicleListingFileDto.builder()
                .id(file.getId())
                .fileUrl(file.getFileUrl())
                .originalFilename(file.getOriginalFilename())
                .fileType(file.getFileType())
                .documentType(file.getDocumentType())
                .isPublic(file.getIsPublic())
                .isCover(file.getIsCover())
                .displayOrder(file.getDisplayOrder())
                .build();
    }

    private String getCoverImageUrl(PublicVehicleListing listing) {
        return listing.getFiles().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsPublic())
                        && f.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO
                        && Boolean.TRUE.equals(f.getIsCover()))
                .findFirst()
                .map(PublicVehicleListingFile::getFileUrl)
                .orElseGet(() -> listing.getFiles().stream()
                        .filter(f -> Boolean.TRUE.equals(f.getIsPublic())
                                && f.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO)
                        .sorted(Comparator.comparingInt(PublicVehicleListingFile::getDisplayOrder))
                        .findFirst()
                        .map(PublicVehicleListingFile::getFileUrl)
                        .orElse(null));
    }

    private List<String> getPublicImageUrls(PublicVehicleListing listing) {
        return listing.getFiles().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsPublic())
                        && f.getFileType() == PublicVehicleListingFileType.VEHICLE_PHOTO)
                .sorted(Comparator.comparingInt(PublicVehicleListingFile::getDisplayOrder))
                .map(PublicVehicleListingFile::getFileUrl)
                .collect(Collectors.toList());
    }

    private String getVideoUrl(PublicVehicleListing listing) {
        return listing.getFiles().stream()
                .filter(f -> f.getFileType() == PublicVehicleListingFileType.VEHICLE_VIDEO)
                .findFirst()
                .map(PublicVehicleListingFile::getFileUrl)
                .orElse(null);
    }

    private List<String> getDocumentUrls(PublicVehicleListing listing) {
        return listing.getFiles().stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsPublic()))
                .map(PublicVehicleListingFile::getFileUrl)
                .collect(Collectors.toList());
    }

    private PublicVehicleListingSellerDto toSellerDto(PublicVehicleListing listing) {
        return PublicVehicleListingSellerDto.builder()
                .id(listing.getId())
                .listingNumber(listing.getListingNumber())
                .status(listing.getStatus())
                .rejectionReason(listing.getRejectionReason())
                .reviewNotes(listing.getAdminNotes())
                .title(listing.getTitle())
                .lotNumber(listing.getLotNumber())
                .vehicleNumber(listing.getVehicleNumber())
                .brand(listing.getBrand())
                .model(listing.getModel())
                .variant(listing.getVariant())
                .manufacturingYear(listing.getManufacturingYear())
                .kilometersDriven(listing.getKilometersDriven())
                .fuelType(listing.getFuelType())
                .engineCC(listing.getEngineCC())
                .color(listing.getColor())
                .price(listing.getPrice())
                .priceInWords(listing.getPriceInWords())
                .negotiable(listing.getNegotiable())
                .sellerName(listing.getSellerName())
                .sellerPhone(listing.getSellerPhone())
                .sellerAddress(listing.getSellerAddress())
                .ownerName(listing.getOwnerName())
                .ownerPhone(listing.getOwnerPhone())
                .ownerAddress(listing.getOwnerAddress())
                .files(listing.getFiles().stream().map(this::toFileDto).collect(Collectors.toList()))
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .submittedAt(listing.getSubmittedAt())
                .reviewedAt(listing.getReviewedAt())
                .approvedAt(listing.getApprovedAt())
                .publishedAt(listing.getPublishedAt())
                .soldAt(listing.getSoldAt())
                .build();
    }

    private PublicVehicleListingResponseDto toResponseDto(PublicVehicleListing listing) {
        String fallbackTitle = (listing.getBrand() != null ? listing.getBrand() : "")
                + (listing.getModel() != null ? " " + listing.getModel() : "");
        String title = (listing.getTitle() != null && !listing.getTitle().trim().isEmpty())
                ? listing.getTitle().trim()
                : fallbackTitle.trim();
        String cover = getCoverImageUrl(listing);
        List<String> images = getPublicImageUrls(listing);

        return PublicVehicleListingResponseDto.builder()
                .id(listing.getId())
                .listingNumber(listing.getListingNumber())
                .title(title.trim())
                .lotNumber(listing.getLotNumber())
                .brand(listing.getBrand())
                .model(listing.getModel())
                .variant(listing.getVariant())
                .year(listing.getManufacturingYear())
                .manufacturingYear(listing.getManufacturingYear())
                .kilometers(listing.getKilometersDriven())
                .kilometersDriven(listing.getKilometersDriven())
                .fuel(listing.getFuelType())
                .fuelType(listing.getFuelType())
                .engineCapacity(listing.getEngineCC())
                .engineCC(listing.getEngineCC())
                .color(listing.getColor())
                .vehicleNumber(listing.getVehicleNumber())
                .price(listing.getPrice())
                .sellPrice(listing.getPrice())
                .negotiable(listing.getNegotiable())
                .exchangeAvailable(false)
                .status("Available")
                .sellerName(listing.getSellerName())
                .sellerPhone(listing.getSellerPhone())
                .sellerPhonePrimary(listing.getSellerPhone())
                .sellerAddress(listing.getSellerAddress())
                .shopAddress(listing.getSellerAddress())
                .image(cover)
                .mainImageUrl(cover)
                .coverImageUrl(cover)
                .images(images)
                .imageUrls(images)
                .videoUrl(getVideoUrl(listing))
                .verified(true)
                .isPublicListing(true)
                .source("public")
                .publishedAt(listing.getPublishedAt())
                .createdAt(listing.getCreatedAt())
                .build();
    }

    private PublicVehicleListingAdminDto toAdminDto(PublicVehicleListing listing) {
        String sellerEmail = null;
        String sellerAccountName = null;
        String sellerAccountPhone = null;
        if (listing.getSellerUserId() != null) {
            // Single DB lookup instead of 3 separate findById calls
            Optional<User> sellerOpt = userRepository.findById(listing.getSellerUserId());
            if (sellerOpt.isPresent()) {
                User seller = sellerOpt.get();
                sellerAccountName = seller.getFirstName() + " " + seller.getLastName();
                sellerAccountPhone = seller.getPhoneNumber();
                sellerEmail = seller.getEmail();
            }
        }
        return PublicVehicleListingAdminDto.builder()
                .id(listing.getId())
                .listingNumber(listing.getListingNumber())
                .title(listing.getTitle())
                .lotNumber(listing.getLotNumber())
                .sellerName(listing.getSellerName())
                .sellerPhone(listing.getSellerPhone())
                .sellerAddress(listing.getSellerAddress())
                .sellerEmail(sellerEmail)
                .sellerAccountName(sellerAccountName)
                .sellerAccountPhone(sellerAccountPhone)
                .ownerName(listing.getOwnerName())
                .ownerPhone(listing.getOwnerPhone())
                .ownerAddress(listing.getOwnerAddress())
                .vehicleNumber(listing.getVehicleNumber())
                .brand(listing.getBrand())
                .model(listing.getModel())
                .variant(listing.getVariant())
                .manufacturingYear(listing.getManufacturingYear())
                .kilometersDriven(listing.getKilometersDriven())
                .fuelType(listing.getFuelType())
                .engineCC(listing.getEngineCC())
                .color(listing.getColor())
                .price(listing.getPrice())
                .priceInWords(listing.getPriceInWords())
                .negotiable(listing.getNegotiable())
                .status(listing.getStatus())
                .adminNotes(listing.getAdminNotes())
                .rejectionReason(listing.getRejectionReason())
                .declarationAccepted(listing.getDeclarationAccepted())
                .files(listing.getFiles().stream().map(this::toFileDto).collect(Collectors.toList()))
                .coverImageUrl(getCoverImageUrl(listing))
                .publicImageUrls(getPublicImageUrls(listing))
                .documentUrls(getDocumentUrls(listing))
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .submittedAt(listing.getSubmittedAt())
                .reviewedAt(listing.getReviewedAt())
                .approvedAt(listing.getApprovedAt())
                .publishedAt(listing.getPublishedAt())
                .soldAt(listing.getSoldAt())
                .sellerUpdatedAt(listing.getSellerUpdatedAt())
                .reviewHistory(reviewHistoryRepository.findByListingIdOrderByPerformedAtAsc(listing.getId())
                        .stream()
                        .map(h -> PublicVehicleListingReviewHistoryDto.builder()
                                .id(h.getId())
                                .actor(h.getActor())
                                .action(h.getAction())
                                .reason(h.getReason())
                                .notes(h.getNotes())
                                .performedAt(h.getPerformedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
