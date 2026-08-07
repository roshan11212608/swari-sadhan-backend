package swari.sewa.module.employee.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import swari.sewa.module.employee.dto.LeaveRequestDto;
import swari.sewa.module.employee.dto.LeaveRequestRequestDto;
import swari.sewa.module.employee.entity.LeaveRequest;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LeaveRequestMapper {
    
    LeaveRequestDto toDto(LeaveRequest leaveRequest);
    
    LeaveRequest toEntity(LeaveRequestDto leaveRequestDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "duration", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appliedDate", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedDate", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LeaveRequest toEntity(LeaveRequestRequestDto requestDto);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(LeaveRequestDto dto, @MappingTarget LeaveRequest leaveRequest);
}
