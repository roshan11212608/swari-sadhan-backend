package swari.sewa.module.employee.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import swari.sewa.module.employee.dto.AdvancePaymentDto;
import swari.sewa.module.employee.dto.AdvancePaymentRequestDto;
import swari.sewa.module.employee.entity.AdvancePayment;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AdvancePaymentMapper {
    
    AdvancePaymentDto toDto(AdvancePayment advancePayment);
    
    AdvancePayment toEntity(AdvancePaymentDto advancePaymentDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "remainingBalance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedDate", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "advanceDate", target = "date")
    AdvancePayment toEntity(AdvancePaymentRequestDto requestDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(AdvancePaymentDto dto, @MappingTarget AdvancePayment advancePayment);
}
